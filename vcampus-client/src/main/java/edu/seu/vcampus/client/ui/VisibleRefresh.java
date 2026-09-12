package edu.seu.vcampus.client.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.HierarchyEvent;
import java.util.function.BooleanSupplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

/** 仅在页面可见时刷新；离开或关闭页面立即停止，不打断正在输入的内容。 */
public final class VisibleRefresh {
    private VisibleRefresh() { }

    public static void attach(final JComponent owner, final BooleanSupplier ready, final Runnable refresh) {
        install(owner, ready, refresh, 8000);
    }

    static Timer install(final JComponent owner, final BooleanSupplier ready, final Runnable refresh, int interval) {
        final Timer timer = new Timer(interval, e -> {
            if (owner.isShowing() && ready.getAsBoolean() && !editing(owner)) refresh.run();
        });
        timer.setInitialDelay(Math.min(400, interval));
        owner.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (owner.isShowing()) timer.restart(); else timer.stop();
        });
        return timer;
    }

    private static boolean editing(JComponent owner) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!(focus instanceof JTextComponent) || !((JTextComponent) focus).isEditable()) return false;
        Component window = SwingUtilities.getWindowAncestor(owner);
        return window != null && SwingUtilities.isDescendingFrom(focus, (java.awt.Container) window);
    }
}
