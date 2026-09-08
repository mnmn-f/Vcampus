package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 混合排序优先命中与用户问题语义接近的校园知识片段。 */
public final class KnowledgeRankerTest {
    @Test public void operationQuestionRanksExactGuideFirst() {
        AiKnowledgeChunk operation = chunk(1L, "学生选课并加入课表操作步骤",
                "进入教务管理，打开选课中心，搜索课程并点击选课，成功后查看我的课表。");
        AiKnowledgeChunk rules = chunk(2L, "选课业务规则",
                "课程已发布、有剩余容量并且时间不冲突时才允许选课。");
        AiKnowledgeChunk unrelated = chunk(3L, "图书借阅步骤", "在图书馆搜索图书并借阅。");
        List<AiKnowledgeChunk> ranked = new KnowledgeRanker().rank(
                "如何在课表里添加课程", Arrays.asList(rules, unrelated, operation), 2);
        assertEquals(2, ranked.size());
        assertEquals(operation.getChunkId(), ranked.get(0).getChunkId());
    }

    @Test public void emptyQuestionReturnsNoEvidence() {
        List<AiKnowledgeChunk> ranked = new KnowledgeRanker().rank(" ",
                Arrays.asList(chunk(1L, "规则", "内容")), 3);
        assertTrue(ranked.isEmpty());
    }

    @Test public void unrelatedQuestionDoesNotMatchOnCommonSingleCharacters() {
        List<AiKnowledgeChunk> ranked = new KnowledgeRanker().rank("食堂失物招领流程",
                Arrays.asList(chunk(1L, "课程查询说明", "学生在教务模块查看已选课程。")), 3);
        assertTrue(ranked.isEmpty());
    }

    private AiKnowledgeChunk chunk(long id, String title, String content) {
        return new AiKnowledgeChunk(id, "SYSTEM_GUIDE", title, content,
                "ACTIVE", 100L + id);
    }
}
