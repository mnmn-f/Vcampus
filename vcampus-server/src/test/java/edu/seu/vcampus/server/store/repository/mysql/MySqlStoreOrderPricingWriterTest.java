package edu.seu.vcampus.server.store.repository.mysql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MySqlStoreOrderPricingWriterTest {
    @Test public void legacySnapshotKeepsShortValueAndCompactsLongStack() {
        assertEquals("WELCOME", MySqlStoreOrderPricingWriter.legacySnapshot("WELCOME"));
        String value = "PROMOTION-01,PROMOTION-02,PROMOTION-03,PROMOTION-04,"
                + "PROMOTION-05,PROMOTION-06";
        String snapshot = MySqlStoreOrderPricingWriter.legacySnapshot(value);
        assertTrue(snapshot.startsWith("MULTI-6-"));
        assertTrue(snapshot.length() <= 64);
        assertEquals(snapshot, MySqlStoreOrderPricingWriter.legacySnapshot(value));
    }
}
