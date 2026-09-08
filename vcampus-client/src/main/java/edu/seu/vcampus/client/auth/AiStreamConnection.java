package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.AiConversationListener;
import edu.seu.vcampus.common.ai.AiPendingAction;
import edu.seu.vcampus.common.ai.AiQuery;
import edu.seu.vcampus.common.ai.AiStreamChunk;
import edu.seu.vcampus.common.ai.AiStreamListener;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.MessageType;
import edu.seu.vcampus.common.protocol.command.AiCommands;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** 单次 AI_QUERY 的专用流式 Socket，避免影响普通业务请求。 */
final class AiStreamConnection implements Runnable {
    private final String host;
    private final int port;
    private final int connectTimeout;
    private final int readTimeout;
    private final String token;
    private final AiQuery query;
    private final AiStreamListener listener;
    private volatile Socket socket;
    private volatile boolean cancelled;

    AiStreamConnection(String host, int port, int connectTimeout, int readTimeout,
            String token, AiQuery query, AiStreamListener listener) {
        this.host = host; this.port = port; this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout; this.token = token; this.query = query; this.listener = listener;
    }

    public void run() {
        boolean completed = false;
        try {
            Socket current = new Socket(); socket = current;
            current.connect(new InetSocketAddress(host, port), connectTimeout);
            current.setSoTimeout(readTimeout);
            ObjectOutputStream output = new ObjectOutputStream(
                    new BufferedOutputStream(current.getOutputStream()));
            output.flush();
            ObjectInputStream input = new SafeObjectInputStream(
                    new BufferedInputStream(current.getInputStream()));
            output.writeObject(Message.request(AiCommands.QUERY, token, query));
            output.flush(); output.reset();
            while (!current.isClosed()) {
                Object value = input.readObject();
                if (!(value instanceof Message)) throw new IOException("服务器返回未知 AI 消息");
                Message message = (Message) value;
                if (message.getType() == MessageType.STREAM_CHUNK
                        && message.getPayload() instanceof AiStreamChunk) {
                    AiStreamChunk chunk = (AiStreamChunk) message.getPayload();
                    if (!chunk.getText().isEmpty()) listener.onChunk(chunk.getText());
                    if (!chunk.getEvidence().isEmpty()
                            && listener instanceof AiConversationListener) {
                        ((AiConversationListener) listener).onEvidence(chunk.getEvidence());
                    }
                    if (chunk.isCompleted()) { completed = true; listener.onComplete(); break; }
                } else if (message.getType() == MessageType.ACTION_CONFIRMATION
                        && message.getPayload() instanceof AiPendingAction
                        && listener instanceof AiConversationListener) {
                    ((AiConversationListener) listener).onActionRequired(
                            (AiPendingAction) message.getPayload());
                } else if (message.getType() == MessageType.RESPONSE && !message.isSuccess()) {
                    listener.onFailure(message.getUserMessage()); return;
                }
            }
        } catch (Exception ex) {
            if (!completed && !cancelled && !Thread.currentThread().isInterrupted()) {
                listener.onFailure("AI 连接中断，请稍后重试。");
            }
        } finally { close(); }
    }

    void cancel() {
        cancelled = true;
        close();
    }

    void close() {
        Socket current = socket; socket = null;
        if (current != null) try { current.close(); } catch (IOException ignored) { }
    }
}
