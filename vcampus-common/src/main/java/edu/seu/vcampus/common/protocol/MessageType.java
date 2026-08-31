package edu.seu.vcampus.common.protocol;

/** 客户端与服务端协议消息的类型。 */
public enum MessageType {
    REQUEST,
    RESPONSE,
    STREAM_CHUNK,
    ACTION_CONFIRMATION,
    EVENT
}
