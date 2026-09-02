package edu.seu.vcampus.common.protocol;

import java.io.Serializable;
import java.util.UUID;

/** 客户端与服务端统一传输信封。 */
public final class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final MessageType type;
    private final String command;
    private final String sessionToken;
    private final Serializable payload;
    private final boolean success;
    private final String resultCode;
    private final String userMessage;
    private final long timestamp;

    private Message(String requestId, MessageType type, String command,
                    String sessionToken, Serializable payload, boolean success,
                    String resultCode, String userMessage) {
        this.requestId = requestId;
        this.type = type;
        this.command = command;
        this.sessionToken = sessionToken;
        this.payload = payload;
        this.success = success;
        this.resultCode = resultCode;
        this.userMessage = userMessage;
        this.timestamp = System.currentTimeMillis();
    }

    public static Message request(String command, String token, Serializable payload) {
        return new Message(UUID.randomUUID().toString(), MessageType.REQUEST,
                command, token, payload, false, null, null);
    }

    public static Message success(Message request, Serializable payload) {
        return response(request, true, ResultCodes.OK, "操作成功", payload);
    }

    public static Message failure(Message request, String code, String message) {
        return response(request, false, code, message, null);
    }

    public static Message stream(Message request, Serializable payload) {
        return related(request, MessageType.STREAM_CHUNK, payload,
                true, ResultCodes.OK, null);
    }

    public static Message action(Message request, Serializable payload) {
        return related(request, MessageType.ACTION_CONFIRMATION, payload,
                true, ResultCodes.OK, null);
    }

    private static Message response(Message request, boolean success, String code,
                                    String text, Serializable payload) {
        return related(request, MessageType.RESPONSE, payload, success, code, text);
    }

    private static Message related(Message request, MessageType type,
                                   Serializable payload, boolean success,
                                   String code, String text) {
        return new Message(request.requestId, type, request.command,
                null, payload, success, code, text);
    }

    public String getRequestId() { return requestId; }
    public MessageType getType() { return type; }
    public String getCommand() { return command; }
    public String getSessionToken() { return sessionToken; }
    public Serializable getPayload() { return payload; }
    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return userMessage; }
    public long getTimestamp() { return timestamp; }
}
