package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.service.AiServiceException;
import edu.seu.vcampus.server.ai.service.AiToolService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import org.junit.Assume;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/** 真实 MySQL 验证同一个 AI 写操作不能被并发确认两次。 */
public final class AiToolClaimMySqlIntegrationTest {
    @Test
    public void onlyOneConcurrentConfirmationCanClaimTheAction() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        JdbcConnectionFactory factory = factory();
        long userId = demoStudent(factory);
        AiToolService service = new AiToolService(new AiToolRepository(),
                new TransactionManager(factory));
        long actionId = service.create(null, "claim-" + UUID.randomUUID(),
                "store.cart.add", true, "{\"id\":1,\"quantity\":1}", userId);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = pool.submit(() -> claim(service, actionId, userId, start));
            Future<String> second = pool.submit(() -> claim(service, actionId, userId, start));
            start.countDown();
            String outcomes = first.get() + "," + second.get();
            assertEquals(outcomes, 1, occurrences(outcomes, "CLAIMED"));
            assertEquals(outcomes, 1, occurrences(outcomes, "CONFLICT"));
        } finally {
            pool.shutdownNow();
            delete(factory, actionId);
        }
    }

    private static String claim(AiToolService service, long actionId, long userId,
                                CountDownLatch start) throws Exception {
        start.await();
        try {
            service.pending(actionId, userId);
            return "CLAIMED";
        } catch (AiServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
            return "CONFLICT";
        }
    }

    private static int occurrences(String text, String value) {
        return (text.length() - text.replace(value, "").length()) / value.length();
    }

    private static JdbcConnectionFactory factory() {
        return new JdbcConnectionFactory();
    }

    private static long demoStudent(JdbcConnectionFactory factory) throws Exception {
        try (Connection c = factory.open(); PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM users WHERE username='demo_student'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("demo_student is missing");
            return rs.getLong(1);
        }
    }

    private static void delete(JdbcConnectionFactory factory, long id) throws Exception {
        try (Connection c = factory.open(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM ai_tool_call_logs WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }
}
