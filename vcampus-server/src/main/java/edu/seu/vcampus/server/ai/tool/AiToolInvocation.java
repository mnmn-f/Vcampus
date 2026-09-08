package edu.seu.vcampus.server.ai.tool;

/** 从自然语言识别出的确定性工具调用。 */
public final class AiToolInvocation {
    private final String toolName;
    private final String argumentsJson;
    private final String summary;

    public AiToolInvocation(String toolName, String argumentsJson, String summary) {
        this.toolName = toolName; this.argumentsJson = argumentsJson; this.summary = summary;
    }

    public String getToolName() { return toolName; }
    public String getArgumentsJson() { return argumentsJson; }
    public String getSummary() { return summary; }
}
