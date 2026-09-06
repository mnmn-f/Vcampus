package edu.seu.vcampus.server.ai.model;

import edu.seu.vcampus.common.ai.AiAttachment;

import java.util.List;

/** 可替换的生成模型边界。 */
public interface AiModel {
    boolean isConfigured();
    String getModelName();
    void generate(String requestId, String prompt, AiTextSink sink) throws Exception;

    default void generate(String requestId, String prompt, List<AiAttachment> attachments,
                          AiTextSink sink) throws Exception {
        if (attachments != null && !attachments.isEmpty()) {
            throw new IllegalArgumentException("当前模型不支持附件");
        }
        generate(requestId, prompt, sink);
    }
}
