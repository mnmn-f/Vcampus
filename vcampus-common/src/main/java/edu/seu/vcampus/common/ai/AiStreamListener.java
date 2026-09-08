package edu.seu.vcampus.common.ai;

/** 接收 AI 查询的分块、完成和失败事件。 */
public interface AiStreamListener {
    void onChunk(String text);
    void onComplete();
    void onFailure(String userMessage);
}
