package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiToolRouteTestResult;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiTextSink;
import edu.seu.vcampus.server.ai.tool.AiToolRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 路由测试不得依赖或执行 ToolBridge。 */
public final class AiToolRouteTestServiceTest {
    @Test public void parsesReadRouteWithoutBusinessBridge() {
        AiAssistantService service = new AiAssistantService(null, null, null,
                AiToolRegistry.campusDefaults(), null, new DisabledModel());
        AiToolRouteTestResult result = service.testToolRoute("商店里有什么商品？");
        assertTrue(result.isMatched());
        assertEquals("store.product.search", result.getToolName());
        assertEquals("{\"keyword\":\"\"}", result.getArgumentsJson());
        assertFalse(result.isWriteOperation());
    }

    @Test public void reportsMissingParametersWithoutExecutingWrite() {
        AiAssistantService service = new AiAssistantService(null, null, null,
                AiToolRegistry.campusDefaults(), null, new DisabledModel());
        AiToolRouteTestResult result = service.testToolRoute("帮我预约自习室");
        assertEquals("library.study-room.reserve", result.getToolName());
        assertTrue(result.isWriteOperation());
        assertNotNull(result.getClarification());
    }

    private static final class DisabledModel implements AiModel {
        public boolean isConfigured() { return false; }
        public String getModelName() { return "disabled"; }
        public void generate(String requestId, String prompt, AiTextSink sink) { }
    }
}
