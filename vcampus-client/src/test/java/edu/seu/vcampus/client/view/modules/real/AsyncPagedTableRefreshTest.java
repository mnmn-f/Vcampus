package edu.seu.vcampus.client.view.modules.real;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsyncPagedTableRefreshTest {
    @Test public void refreshPreservesStableSelectionWithoutClearingEditorMidway() throws Exception {
        AtomicReference<List<String>> rows = new AtomicReference<>(Arrays.asList("one", "two"));
        java.util.ArrayList<String> events = new java.util.ArrayList<>();
        AsyncPagedTable<String> table = create((p, k, f) -> page(rows.get(), p), events::add);
        await(() -> table.getTable().getRowCount() == 2);
        edt(() -> { table.setItemKey(v -> v); table.getTable().setRowSelectionInterval(1, 1); events.clear();
            rows.set(Arrays.asList("two", "one")); table.refreshCurrentPage(); });
        await(() -> "two".equals(table.getTable().getValueAt(0, 0)) && !events.isEmpty());
        edt(() -> { assertEquals("two", table.selectedItem()); assertEquals(Collections.singletonList("two"), events); });
    }

    @Test public void sortedRowsUseModelIdentityForActions() throws Exception {
        AsyncPagedTable<String> table = create((p, k, f) -> page(Arrays.asList("z", "a"), p), null);
        await(() -> table.getTable().getRowCount() == 2);
        edt(() -> { table.getTable().setAutoCreateRowSorter(true); table.getTable().getRowSorter().toggleSortOrder(0);
            table.getTable().setRowSelectionInterval(0, 0); assertEquals("a", table.selectedItem()); });
    }

    @Test public void sortedRefreshRestoresSameRecordNotSameVisibleIndex() throws Exception {
        AtomicReference<List<String>> rows = new AtomicReference<>(Arrays.asList("z", "a"));
        AsyncPagedTable<String> table = create((p, k, f) -> page(rows.get(), p), null);
        await(() -> table.getTable().getRowCount() == 2);
        edt(() -> { table.setItemKey(v -> v); table.getTable().setAutoCreateRowSorter(true);
            table.getTable().getRowSorter().toggleSortOrder(0); table.getTable().setRowSelectionInterval(0, 0);
            rows.set(Arrays.asList("a", "b", "z")); table.refreshCurrentPage(); });
        await(() -> !table.isLoading() && table.getTable().getRowCount() == 3);
        edt(() -> assertEquals("a", table.selectedItem()));
    }

    @Test public void slowerOldResponseCannotOverwriteNewSearch() throws Exception {
        AtomicInteger calls = new AtomicInteger(); CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        AsyncPagedTable<String> table = create((p, k, f) -> {
            if (calls.incrementAndGet() == 1) { started.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); return page(Arrays.asList("old"), p); }
            return page(Arrays.asList("new"), p);
        }, null);
        try { assertTrue(started.await(2, TimeUnit.SECONDS)); edt(table::reload);
            await(() -> table.getTable().getRowCount() == 1); release.countDown();
            Thread.sleep(120); edt(() -> assertEquals("new", table.getTable().getValueAt(0, 0)));
        } finally { release.countDown(); }
    }

    @Test public void failedManualRefreshClearsStaleActionTarget() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AsyncPagedTable<String> table = create((p, k, f) -> {
            if (calls.incrementAndGet() > 1) throw new IllegalStateException("offline");
            return page(Arrays.asList("one"), p);
        }, null);
        await(() -> table.getTable().getRowCount() == 1);
        edt(() -> { table.getTable().setRowSelectionInterval(0, 0); table.reload(); });
        await(() -> table.getTable().getRowCount() == 0); edt(() -> assertNull(table.selectedItem()));
    }

    private static PageSlice<String> page(List<String> rows, int page) { return new PageSlice<>(rows, rows.size(), page, 20); }
    private static AsyncPagedTable<String> create(AsyncPagedTable.Loader<String> loader, AsyncPagedTable.SelectionListener<String> listener) throws Exception {
        AtomicReference<AsyncPagedTable<String>> result = new AtomicReference<>();
        edt(() -> result.set(new AsyncPagedTable<>("测试", "", "查询", null, new String[]{"名称"}, loader, v -> new Object[]{v}, listener)));
        return result.get();
    }
    static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean();
            edt(() -> result.set(condition.getAsBoolean())); if (result.get()) return; Thread.sleep(20);
        }
        fail("等待界面更新超时");
    }
}
