package edu.seu.vcampus.common.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiKnowledgeTestResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String answer;
    private final List<AiKnowledgeChunk> matches;
    private final boolean modelUsed;
    public AiKnowledgeTestResult(String answer, List<AiKnowledgeChunk> matches, boolean modelUsed) {
        this.answer = answer;
        this.matches = Collections.unmodifiableList(new ArrayList<AiKnowledgeChunk>(matches));
        this.modelUsed = modelUsed;
    }
    public String getAnswer() { return answer; }
    public List<AiKnowledgeChunk> getMatches() { return matches; }
    public boolean isModelUsed() { return modelUsed; }
}
