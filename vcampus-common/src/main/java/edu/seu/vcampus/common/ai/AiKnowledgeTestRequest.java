package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiKnowledgeTestRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String question;
    public AiKnowledgeTestRequest(String question) { this.question = question; }
    public String getQuestion() { return question; }
}
