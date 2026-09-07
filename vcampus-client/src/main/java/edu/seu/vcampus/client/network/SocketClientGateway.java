package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 TCP ObjectStream 的同步请求边界。
 *
 * <p>创建输出流并 flush 后再创建输入流，避免两端同时等待对象流头导致握手阻塞。
 * 后续若启用异步推送，可在该接口之上增加接收循环，不改变页面层。</p>
 */
public final class SocketClientGateway implements ClientGateway {
    public static final int DEFAULT_TIMEOUT_MILLIS = 5000;
    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public SocketClientGateway(String host, int port) {
        this(host, port, DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MILLIS);
    }

    public SocketClientGateway(String host, int port, int connectTimeoutMillis) {
        this(host, port, connectTimeoutMillis, connectTimeoutMillis);
    }

    public SocketClientGateway(String host, int port, int connectTimeoutMillis,
                               int readTimeoutMillis) {
        if (host == null || host.trim().length() == 0) {
            throw new IllegalArgumentException("host 不能为空");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port 不合法");
        }
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) {
            throw new IllegalArgumentException("网络超时必须为正数");
        }
        this.host = host.trim();
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public synchronized Message send(Message request)
            throws IOException, ClassNotFoundException {
        if (request == null) {
            throw new IllegalArgumentException("request 不能为空");
        }
        boolean fileRequest = request.getCommand().startsWith("library.pdf.");
        if (fileRequest) close();
        ensureConnected();
        try {
            output.writeObject(request);
            output.flush();
            output.reset();
            Object value = input.readObject();
            if (!(value instanceof Message)) {
                throw new IOException("服务器返回了未知消息类型");
            }
            if (fileRequest) close();
            return (Message) value;
        } catch (IOException ex) {
            close();
            throw ex;
        } catch (ClassNotFoundException ex) {
            close();
            throw ex;
        }
    }

    @Override
    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void close() {
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
        input = null;
        output = null;
        socket = null;
    }

    private void ensureConnected() throws IOException {
        if (isConnected()) {
            return;
        }
        close();
        Socket connected = new Socket();
        try {
            connected.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            connected.setSoTimeout(readTimeoutMillis);
            ObjectOutputStream connectedOutput = new ObjectOutputStream(
                    new BufferedOutputStream(connected.getOutputStream()));
            connectedOutput.flush();
            ObjectInputStream connectedInput = new SafeObjectInputStream(
                    new BufferedInputStream(connected.getInputStream()));
            socket = connected;
            output = connectedOutput;
            input = connectedInput;
        } catch (IOException ex) {
            closeQuietly(connected);
            throw ex;
        }
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // 关闭阶段不覆盖原始错误。
            }
        }
    }
}
