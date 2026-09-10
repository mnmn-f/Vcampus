package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.ai.AiMode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ModeIntentClassifierTest {
    private final ToolIntentParser parser = new ToolIntentParser();
    private final AiToolRegistry tools = AiToolRegistry.campusDefaults();
    private final ModeIntentClassifier classifier = new ModeIntentClassifier();

    @Test public void campusReadBelongsToQaMode() {
        String text = "我的成绩是多少";
        assertEquals(AiMode.QA, classifier.recommend(text, parser.parse(text), tools));
    }

    @Test public void campusWriteBelongsToTaskMode() {
        String text = "帮我预约自习室";
        assertEquals(AiMode.TASK, classifier.recommend(text, parser.parse(text), tools));
    }

    @Test public void systemHowToQuestionBelongsToQaButGeneralChatStaysChat() {
        assertEquals(AiMode.QA, classifier.recommend("怎么在 VCampus 里选课", null, tools));
        assertEquals(AiMode.CHAT, classifier.recommend("解释 Java 的多态", null, tools));
    }
}
