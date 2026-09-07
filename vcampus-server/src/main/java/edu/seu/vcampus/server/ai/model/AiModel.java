package edu.seu.vcampus.server.ai.model;

/** 可替换的生成模型边界。 */
public interface AiModel {
    boolean isConfigured();
    String getModelName();
    void generate(String requestId, String prompt, AiTextSink sink) throws Exception;
}
