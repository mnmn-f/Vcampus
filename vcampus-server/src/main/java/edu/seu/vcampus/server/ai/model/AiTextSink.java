package edu.seu.vcampus.server.ai.model;

/** 模型生成文本时的增量回调。 */
public interface AiTextSink {
    void onText(String text);
}
