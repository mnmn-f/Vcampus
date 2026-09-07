package edu.seu.vcampus.server.ai.tool;

import java.io.Serializable;

/** 一个 AI 工具到既有业务命令的适配契约。 */
public interface AiTool {
    String getName();
    String getDescription();
    String getTargetCommand();
    boolean isWriteOperation();
    Serializable payload(String argumentsJson);
}
