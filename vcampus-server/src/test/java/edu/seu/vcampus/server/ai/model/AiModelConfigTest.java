package edu.seu.vcampus.server.ai.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** API 地址和密钥配置的安全边界。 */
public final class AiModelConfigTest {
    @Test public void acceptsHttpsAndLoopbackTestEndpoints() {
        assertEquals("https://api.example.com/v1/responses",
                config("https://api.example.com/v1/responses", "key").getEndpoint());
        assertEquals("http://127.0.0.1:8080/v1/responses",
                config("http://127.0.0.1:8080/v1/responses", "key").getEndpoint());
        rejected("http://api.example.com/v1/responses");
        rejected("https://user@example.com/v1/responses");
        rejected("https://api.example.com/v1/responses?debug=true");
    }

    @Test public void requiresKeyBeforeCallingApi() {
        assertTrue(config("https://api.example.com/v1/responses", "secret").isConfigured());
        assertFalse(config("https://api.example.com/v1/responses", " ").isConfigured());
    }

    private AiModelConfig config(String endpoint, String key) {
        return new AiModelConfig(endpoint, key, "test-model", 1000, 1000);
    }

    private void rejected(String endpoint) {
        try { config(endpoint, "key"); fail("must reject insecure endpoint: " + endpoint); }
        catch (IllegalArgumentException expected) { }
    }
}
