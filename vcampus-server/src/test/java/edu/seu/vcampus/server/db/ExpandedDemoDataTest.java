package edu.seu.vcampus.server.db;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ExpandedDemoDataTest {
    @Test
    public void baselineOrderMatchesCurrentPriceSnapshotConstraint() throws Exception {
        String sql = resource("/db/migration/V2__demo_data.sql");
        assertTrue(sql.contains("`original_amount`"));
        assertTrue(sql.contains("`discount_amount`"));
        assertTrue(sql.contains("`payment_mode`"));
    }

    @Test
    public void expandedSeedIsSafeRichAndCoversEveryBusinessArea() throws Exception {
        String sql = resource("/db/migration/V18__expanded_demo_data.sql");
        assertTrue(sql.contains("START TRANSACTION;"));
        assertTrue(sql.contains("test_student"));
        assertTrue(sql.contains("test_teacher"));
        assertTrue(sql.contains("demo_student 多学期测试成绩"));
        assertTrue(sql.contains("course_grades"));
        assertTrue(sql.contains("borrow_records"));
        assertTrue(sql.contains("store_orders"));
        assertTrue(sql.contains("leave_requests"));
        assertTrue(sql.contains("classroom_reservations"));
        assertTrue(sql.contains("account_cancellation_requests"));
        assertTrue(sql.contains("https://picsum.photos/seed/vcampus-product-"));
        assertFalse(sql.toUpperCase().contains("TRUNCATE TABLE"));
        assertFalse(sql.toUpperCase().contains("DROP TABLE"));
    }

    private static String resource(String path) throws Exception {
        InputStream input = ExpandedDemoDataTest.class.getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Missing resource " + path);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) >= 0;) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }
}
