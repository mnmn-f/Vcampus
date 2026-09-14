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
        String seed = resource("/db/migration/V2__demo_data.sql");
        String upgrade = resource("/db/migration/V4__store_experience.sql");

        // V2 runs before the extended order columns exist.
        assertTrue(seed.contains("INSERT INTO `store_orders`"));
        assertFalse(seed.contains("`original_amount`"));
        assertFalse(seed.contains("`discount_amount`"));
        assertFalse(seed.contains("`payment_mode`"));

        // V4 introduces the columns and backfills historical order amounts.
        assertTrue(upgrade.contains("ADD COLUMN original_amount"));
        assertTrue(upgrade.contains("ADD COLUMN discount_amount"));
        assertTrue(upgrade.contains("ADD COLUMN payment_mode"));
        String backfill = "UPDATE `store_orders` SET `original_amount`=`total_amount`";
        String constraint = "ADD CONSTRAINT ck_store_orders_price_snapshot";
        assertTrue(upgrade.contains(backfill));
        assertTrue(upgrade.contains(constraint));
        assertTrue("Historical amounts must be backfilled before adding the constraint",
                upgrade.indexOf(backfill) < upgrade.indexOf(constraint));
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
