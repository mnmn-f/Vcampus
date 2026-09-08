package edu.seu.vcampus.client.view.modules.real;

import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 所有业务列表共用的刷新一致性约束。 */
public final class AsyncPagedTableConsistencyTest {
    @Test
    public void newestReloadWinsAndColumnsCannotBeMoved() throws Exception {
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<AsyncPagedTable<String>> reference = new AtomicReference<AsyncPagedTable<String>>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                reference.set(new AsyncPagedTable<String>("列表", "", "搜索", null,
                        new String[]{"名称"}, (page, keyword, filter) -> {
                            int call = calls.incrementAndGet();
                            if (call == 1) {
                                firstStarted.countDown();
                                assertTrue(releaseFirst.await(2, TimeUnit.SECONDS));
                                return slice("旧数据");
                            }
                            return slice("新数据");
                        }, row -> new Object[]{row}, null));
            }
        });
        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> reference.get().reload());
        awaitCell(reference.get(), "新数据");
        releaseFirst.countDown();
        Thread.sleep(80L);

        assertEquals("新数据", cell(reference.get()));
        assertFalse(reference.get().getTable().getTableHeader().getReorderingAllowed());
    }

    private static PageSlice<String> slice(String value) {
        return new PageSlice<String>(Collections.singletonList(value), 1L, 1, 20);
    }

    private static void awaitCell(AsyncPagedTable<String> table, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (!expected.equals(cell(table)) && System.currentTimeMillis() < deadline) Thread.sleep(20L);
        assertEquals(expected, cell(table));
    }

    private static String cell(final AsyncPagedTable<String> table) throws Exception {
        final AtomicReference<String> value = new AtomicReference<String>();
        SwingUtilities.invokeAndWait(() -> {
            if (table.getTable().getRowCount() > 0) value.set(String.valueOf(table.getTable().getValueAt(0, 0)));
        });
        return value.get();
    }
}
