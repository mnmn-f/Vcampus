package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.common.ai.AiAnswerEvidence;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public final class AiMessageCardTest {
    @Test
    public void businessAndEvidenceBecomeStructuredSections() throws Exception {
        final AiMessageCard[] card = new AiMessageCard[1];
        SwingUtilities.invokeAndWait(() -> {
            card[0] = new AiMessageCard(AiMessageCard.Kind.ASSISTANT,
                    "【实时数据｜查询本人课表】\n周一 08:00 软件工程");
            card[0].complete(value -> { }, Collections.singletonList(
                    new AiAnswerEvidence(1L, "选课操作说明", "SYSTEM_GUIDE", "进入教务模块。", 1L)));
        });
        assertTrue(hasText(card[0], "业务结果 · 查询本人课表"));
        assertTrue(hasText(card[0], "回答依据（1）"));
    }

    @Test
    public void missingStudyRoomDataCreatesThreeParameterInputs() throws Exception {
        final AiMessageCard[] card = new AiMessageCard[1];
        SwingUtilities.invokeAndWait(() -> {
            card[0] = new AiMessageCard(AiMessageCard.Kind.ASSISTANT,
                    "【还需要一点信息】\n预约还需要自习室编号、开始时间和结束时间。");
            card[0].complete(value -> { }, Collections.<AiAnswerEvidence>emptyList());
        });
        assertTrue(count(card[0], JTextField.class) >= 3);
        assertTrue(hasText(card[0], "自习室编号"));
        assertTrue(hasText(card[0], "开始时间"));
        assertTrue(hasText(card[0], "结束时间"));
        assertTrue(hasText(card[0], "日历"));
        assertTrue(hasText(card[0], "补充并继续代办"));
    }

    @Test
    public void feedbackActionsBelongToTheirAssistantBubble() throws Exception {
        final AiMessageCard[] card = new AiMessageCard[1];
        SwingUtilities.invokeAndWait(() -> {
            card[0] = new AiMessageCard(AiMessageCard.Kind.ASSISTANT, "这是一条独立回复。");
            card[0].addFeedbackActions(() -> { }, () -> { }, () -> { });
        });
        assertTrue(hasText(card[0], "评价这条回复"));
        assertTrue(hasText(card[0], "点赞"));
        assertTrue(hasText(card[0], "点踩"));
        assertTrue(hasText(card[0], "纠错"));
    }

    @Test
    public void otherTaskCardsAlsoUseCalendarInputsForDateTimes() throws Exception {
        final AiMessageCard[] card = new AiMessageCard[1];
        SwingUtilities.invokeAndWait(() -> {
            card[0] = new AiMessageCard(AiMessageCard.Kind.ASSISTANT,
                    "【还需要一点信息】\n教室申请还需要教室编号、用途、开始时间和结束时间。"
                            + "\n待补充字段：classroomId,purpose,startAt,endAt");
            card[0].complete(value -> { }, Collections.<AiAnswerEvidence>emptyList());
        });
        assertTrue(hasText(card[0], "教室编号"));
        assertTrue(countText(card[0], "日历") == 2);
    }

    private static boolean hasText(Component root, String expected) {
        if (root instanceof JLabel && ((JLabel) root).getText().contains(expected)) return true;
        if (root instanceof AbstractButton && ((AbstractButton) root).getText().contains(expected)) return true;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            if (hasText(child, expected)) return true;
        }
        return false;
    }

    private static int count(Component root, Class<?> type) {
        int value = type.isInstance(root) ? 1 : 0;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            value += count(child, type);
        }
        return value;
    }

    private static int countText(Component root, String expected) {
        int value = root instanceof AbstractButton
                && expected.equals(((AbstractButton) root).getText()) ? 1 : 0;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            value += countText(child, expected);
        }
        return value;
    }
}
