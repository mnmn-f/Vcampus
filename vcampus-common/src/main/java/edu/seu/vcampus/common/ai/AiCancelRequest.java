package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 取消仍在生成中的 AI 请求。 */
public final class AiCancelRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestId;

    public AiCancelRequest(String requestId) { this.requestId = requestId; }
    public String getRequestId() { return requestId; }
}
