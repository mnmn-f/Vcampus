package edu.seu.vcampus.server.ai.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 确定性校园意图在生成模型 API 未配置时仍可正确路由。 */
public final class ToolIntentParserTest {
    private final ToolIntentParser parser = new ToolIntentParser();

    @Test public void readsUseExistingBusinessTools() {
        assertEquals("academic.schedule.read", parser.parse("帮我查一下课表").getToolName());
        assertEquals("identity.profile.read", parser.parse("查看我的个人资料").getToolName());
        assertEquals("student.profile.read", parser.parse("查看我的学籍信息").getToolName());
        assertEquals("student.grades.read", parser.parse("查询我的成绩").getToolName());
        assertEquals("library.borrow.mine", parser.parse("查看我的借阅记录").getToolName());
        assertEquals("library.study-room.search", parser.parse("查询可用自习室").getToolName());
        assertEquals("library.resource.search", parser.parse("查询图书馆在线资源").getToolName());
        assertEquals("store.account.read", parser.parse("我的账户余额是多少").getToolName());
        assertEquals("store.orders.mine", parser.parse("查看我的订单").getToolName());
        assertEquals("dorm.accommodation.read", parser.parse("我住在哪个宿舍").getToolName());
        assertEquals("dorm.repair.mine", parser.parse("查看我的报修进度").getToolName());
        assertEquals("campus.srtp.mine", parser.parse("查看我的 SRTP 项目").getToolName());
    }

    @Test public void writesExtractIdAndRemainConfirmable() {
        AiToolInvocation enroll = parser.parse("帮我选课 1208");
        assertEquals("academic.course.enroll", enroll.getToolName());
        assertTrue(enroll.getArgumentsJson().contains("1208"));
        AiToolInvocation borrow = parser.parse("借阅图书 57");
        assertEquals("library.book.borrow", borrow.getToolName());
        assertTrue(borrow.getArgumentsJson().contains("57"));
        assertEquals("academic.course.drop", parser.parse("退课 1208").getToolName());
        assertEquals("library.book.return", parser.parse("还书 88").getToolName());
        assertEquals("campus.competition.register", parser.parse("报名竞赛 19").getToolName());
        AiToolInvocation cart = parser.parse("把商品 20 加入购物车，数量 3");
        assertEquals("store.cart.add", cart.getToolName());
        assertTrue(cart.getArgumentsJson().contains("\"quantity\":3"));
        assertEquals("store.order.create", parser.parse("从购物车创建订单").getToolName());
    }

    @Test public void unknownQuestionFallsBackToKnowledgeAndModel() {
        assertNull(parser.parse("学校的历史是什么？"));
        assertNull(parser.parse("如何选课 1208"));
        assertNull(parser.parse("怎么在课表里添加课程"));
    }

    @Test public void searchKeywordIsEscapedAndCleaned() {
        String arguments = parser.parse("搜索图书 Java 入门").getArgumentsJson();
        assertTrue(arguments.contains("Java 入门"));
        assertFalse(arguments.contains("搜索图书"));
        String controlled = parser.parse("搜索图书 Java\n\t入门").getArgumentsJson();
        assertTrue(controlled.contains("Java\\n\\t入门"));
        assertFalse(controlled.contains("\n"));
    }
}
