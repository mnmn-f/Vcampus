package edu.seu.vcampus.server.ai.tool;

import java.io.Serializable;

/** 一个 AI 工具到既有业务命令的适配契约。 */
public interface AiTool {
    String getName();
    String getDescription();
    default String getParameterGuide() { return "{}（无参数）"; }
    default String clarificationFor(String argumentsJson) { return null; }
    String getTargetCommand();
    boolean isWriteOperation();
    Serializable payload(String argumentsJson);
}
