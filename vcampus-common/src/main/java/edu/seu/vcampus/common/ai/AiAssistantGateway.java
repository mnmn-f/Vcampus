package edu.seu.vcampus.common.ai;

/** AI 扩展边界。本阶段仅定义接口，不绑定任何模型供应商。 */
public interface AiAssistantGateway {
    void query(AiQuery query, AiStreamListener listener);

    void cancel(String requestId);
}

