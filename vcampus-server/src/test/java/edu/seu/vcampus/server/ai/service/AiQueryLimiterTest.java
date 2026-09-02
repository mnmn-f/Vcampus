package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public final class AiQueryLimiterTest {
    @Test
    public void limitsEachUserAndReleasesCapacity() {
        AiQueryLimiter limiter = new AiQueryLimiter();
        limiter.register("one", 7L);
        limiter.register("two", 7L);
        try {
            limiter.register("three", 7L);
            fail("third concurrent query must be rejected");
        } catch (AiServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
        limiter.register("other-user", 8L);
        assertFalse(limiter.cancel(8L, "one"));
        limiter.release("one", 7L);
        limiter.register("three", 7L);
        limiter.release("two", 7L);
        limiter.release("three", 7L);
        limiter.release("other-user", 8L);
    }
}
