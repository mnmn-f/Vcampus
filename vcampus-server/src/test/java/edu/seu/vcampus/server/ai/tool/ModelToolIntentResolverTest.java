package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.ai.AiMode;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiTextSink;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 大模型可以泛化表达，但只能返回注册表中的、模式允许的工具。 */
public final class ModelToolIntentResolverTest {
    @Test public void resolvesParaphraseInsideWhitelistAndUsesHistory() {
        FakeModel model = new FakeModel("TOOL\tstore.orders.mine\t{}\t查询我的订单");
        AiToolInvocation result = new ModelToolIntentResolver(
                model, AiToolRegistry.campusDefaults()).resolve("r1",
                "看看我之前买的东西", "用户：刚才在校园商店买了东西", AiMode.QA);
        assertEquals("store.orders.mine", result.getToolName());
        assertTrue(model.prompt.contains("最近对话"));
        assertTrue(model.prompt.contains("看看我之前买的东西"));
    }

    @Test public void rejectsWritesInQuestionAnswerMode() {
        FakeModel model = new FakeModel(
                "TOOL\tstore.order.create\t{}\t创建订单");
        assertNull(new ModelToolIntentResolver(model, AiToolRegistry.campusDefaults())
                .resolve("r2", "结算一下", "", AiMode.QA));
    }

    @Test public void chatModeNeverRoutesBusinessTool() {
        FakeModel model = new FakeModel("TOOL\tstore.orders.mine\t{}\t查询订单");
        assertNull(new ModelToolIntentResolver(model, AiToolRegistry.campusDefaults())
                .resolve("r3", "聊聊订单设计", "", AiMode.CHAT));
    }

    private static final class FakeModel implements AiModel {
        private final String output; private String prompt;
        private FakeModel(String output) { this.output = output; }
        public boolean isConfigured() { return true; }
        public String getModelName() { return "fake"; }
        public void generate(String requestId, String value, AiTextSink sink) {
            prompt = value; sink.onText(output);
        }
    }
}
