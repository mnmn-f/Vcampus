package edu.seu.vcampus.common.ai;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class AiStreamEvidenceTest {
    @Test
    public void completedChunkCarriesImmutableEvidenceSummary() {
        AiAnswerEvidence evidence = new AiAnswerEvidence(7L, "选课规则", "SYSTEM_RULE",
                "依据教务系统规则。", 123L);
        AiStreamChunk chunk = new AiStreamChunk("r", "s", "", true,
                Collections.singletonList(evidence));
        assertTrue(chunk.isCompleted());
        assertEquals(1, chunk.getEvidence().size());
        assertEquals("选课规则", chunk.getEvidence().get(0).getTitle());
    }
}
