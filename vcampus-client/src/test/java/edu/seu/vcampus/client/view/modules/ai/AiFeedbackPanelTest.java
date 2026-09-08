package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.common.ai.AiFeedbackEntry;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class AiFeedbackPanelTest {
    @Test
    public void detailTextKeepsFullSummaryCommentAndMetadata() {
        String summary = "这是一段在表格中可能显示不完整、但详情中必须全部保留的问题摘要。";
        String comment = "这里是管理员需要完整查看的纠错说明，包括具体建议和更多上下文。";
        AiFeedbackEntry entry = new AiFeedbackEntry(18L, "UNHELPFUL", "知识错误",
                comment, summary, 1_788_774_400_000L, "RESOLVED", 42L, "图书馆服务说明");

        String detail = AiFeedbackPanel.detailText(entry);

        assertTrue(detail.contains("反馈编号：18"));
        assertTrue(detail.contains("需改进"));
        assertTrue(detail.contains("已处理"));
        assertTrue(detail.contains("42 · 图书馆服务说明"));
        assertTrue(detail.contains(summary));
        assertTrue(detail.contains(comment));
        assertTrue(detail.contains("提交时间："));
    }
}
