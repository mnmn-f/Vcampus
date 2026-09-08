package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 只解析而不执行工具的安全路由测试结果。 */
public final class AiToolRouteTestResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean matched;
    private final String toolName;
    private final String description;
    private final String argumentsJson;
    private final String clarification;
    private final boolean writeOperation;

    public AiToolRouteTestResult(boolean matched, String toolName, String description,
            String argumentsJson, String clarification, boolean writeOperation) {
        this.matched = matched; this.toolName = toolName; this.description = description;
        this.argumentsJson = argumentsJson; this.clarification = clarification;
        this.writeOperation = writeOperation;
    }
    public boolean isMatched() { return matched; }
    public String getToolName() { return toolName; }
    public String getDescription() { return description; }
    public String getArgumentsJson() { return argumentsJson; }
    public String getClarification() { return clarification; }
    public boolean isWriteOperation() { return writeOperation; }
}
