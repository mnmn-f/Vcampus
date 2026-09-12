package edu.seu.vcampus.client.ui;

import java.awt.event.HierarchyEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisibleRefreshTest {
    @Test public void refreshesVisibleReadyPagesAndStopsWhenHidden() throws Exception {
        AtomicBoolean showing = new AtomicBoolean(), ready = new AtomicBoolean();
        AtomicInteger calls = new AtomicInteger(); CountDownLatch updated = new CountDownLatch(2);
        JPanel owner = new JPanel() { @Override public boolean isShowing() { return showing.get(); } };
        Timer[] timer = new Timer[1];
        SwingUtilities.invokeAndWait(() -> {
            timer[0] = VisibleRefresh.install(owner, ready::get, () -> { calls.incrementAndGet(); updated.countDown(); }, 40);
            assertFalse(timer[0].isRunning()); showing.set(true); changed(owner); assertTrue(timer[0].isRunning());
        });
        try {
            Thread.sleep(120); assertEquals(0, calls.get()); ready.set(true);
            assertTrue(updated.await(2, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> { showing.set(false); changed(owner); assertFalse(timer[0].isRunning()); });
            int stopped = calls.get(); Thread.sleep(120); assertEquals(stopped, calls.get());
        } finally { SwingUtilities.invokeAndWait(() -> timer[0].stop()); }
    }
    private static void changed(JPanel owner) {
        owner.dispatchEvent(new HierarchyEvent(owner, HierarchyEvent.HIERARCHY_CHANGED, owner, null, HierarchyEvent.SHOWING_CHANGED));
    }
}
