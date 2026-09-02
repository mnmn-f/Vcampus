package edu.seu.vcampus.common.ai;

/** 在文本流之外接收需要用户确认的操作。 */
public interface AiConversationListener extends AiStreamListener {
    void onActionRequired(AiPendingAction action);
}
