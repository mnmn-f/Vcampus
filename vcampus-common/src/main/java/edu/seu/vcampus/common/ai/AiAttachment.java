package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 聊天模式附件；内容只随当前请求发送，不写入会话消息表。 */
public final class AiAttachment implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String fileName;
    private final String mediaType;
    private final byte[] content;

    public AiAttachment(String fileName, String mediaType, byte[] content) {
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.content = content == null ? new byte[0] : content.clone();
    }

    public String getFileName() { return fileName; }
    public String getMediaType() { return mediaType; }
    public byte[] getContent() { return content.clone(); }

    public boolean isImage() {
        return mediaType != null && mediaType.toLowerCase().startsWith("image/");
    }

    public boolean isText() {
        return mediaType != null && (mediaType.toLowerCase().startsWith("text/")
                || "application/json".equalsIgnoreCase(mediaType)
                || "application/xml".equalsIgnoreCase(mediaType));
    }
}
