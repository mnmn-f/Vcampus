package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 校园助手的三种明确工作模式。 */
public enum AiMode implements Serializable {
    QA("问答模式"), CHAT("聊天模式"), TASK("代办模式");

    private final String displayName;

    AiMode(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override public String toString() { return displayName; }

    public static AiMode from(String value) {
        if (value == null) return QA;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return QA; }
    }
}
