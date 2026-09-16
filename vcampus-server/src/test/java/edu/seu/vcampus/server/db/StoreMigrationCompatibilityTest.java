package edu.seu.vcampus.server.db;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StoreMigrationCompatibilityTest {
    @Test public void couponRefreshOnlyRenewsExpiredDemoCoupon() throws Exception {
        String migration = resource("/db/migration/V13__store_coupon_refresh.sql");
        assertTrue(migration.contains("WHERE `code` = 'WELCOME10'"));
        assertTrue(migration.contains("`expires_at` <= CURRENT_TIMESTAMP(3)"));
        assertFalse(migration.contains("DELETE")); assertFalse(migration.contains("DROP TABLE"));
    }

    @Test public void shippingMigrationAddsConstrainedOrderFields() throws Exception {
        String migration = resource("/db/migration/V14__store_order_shipping.sql");
        assertTrue(migration.contains("ADD COLUMN `shipping_status`"));
        assertTrue(migration.contains("'READY_FOR_PICKUP','DELIVERED'"));
        assertFalse(migration.contains("DROP TABLE")); assertFalse(migration.contains("DELETE FROM"));
    }

    @Test public void orderSequenceMigrationIsBoundedAndNonDestructive() throws Exception {
        String migration = resource("/db/migration/V23__store_order_daily_sequence.sql");
        assertTrue(migration.contains("PRIMARY KEY (`order_date`)"));
        assertTrue(migration.contains("BETWEEN 1 AND 9999"));
        assertFalse(migration.contains("DROP TABLE")); assertFalse(migration.contains("DELETE FROM"));
    }

    @Test public void productImageMigrationSeparatesOriginalAndThumbnail() throws Exception {
        String migration = resource("/db/migration/V24__store_product_image_variants.sql");
        assertTrue(migration.contains("`original_data` MEDIUMBLOB"));
        assertTrue(migration.contains("`thumbnail_data` MEDIUMBLOB"));
        assertTrue(migration.contains("`reference` VARCHAR(64)"));
        assertFalse(migration.contains("DROP TABLE")); assertFalse(migration.contains("DELETE FROM"));
    }

    @Test public void promotionSnapshotMigrationSupportsStackedPromotions() throws Exception {
        String migration = resource("/db/migration/V25__store_promotion_snapshot_capacity.sql");
        assertTrue(migration.contains("`promotion_code` TEXT NULL"));
        assertFalse(migration.contains("DROP TABLE")); assertFalse(migration.contains("DELETE FROM"));
    }

    private static String resource(String name) throws Exception {
        try (InputStream input = StoreMigrationCompatibilityTest.class.getResourceAsStream(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("missing resource " + name);
            byte[] buffer = new byte[4096]; int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
