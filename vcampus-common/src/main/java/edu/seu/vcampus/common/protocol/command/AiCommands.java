package edu.seu.vcampus.common.protocol.command;

/** AI 助手命令字的唯一登记处。 */
public final class AiCommands {
    public static final String QUERY = "ai.query";
    public static final String CANCEL = "ai.cancel";
    public static final String CONFIRM = "ai.confirm";
    public static final String SESSION_LIST = "ai.session.list";
    public static final String SESSION_CREATE = "ai.session.create";
    public static final String SESSION_HISTORY = "ai.session.history";
    public static final String SESSION_CLEAR = "ai.session.clear";
    public static final String KNOWLEDGE_LIST = "ai.knowledge.list";
    public static final String KNOWLEDGE_SAVE = "ai.knowledge.save";
    public static final String KNOWLEDGE_DELETE = "ai.knowledge.delete";
    public static final String MONITOR = "ai.monitor";

    private AiCommands() { }
}
