package edu.seu.vcampus.client.ui.components;

import org.junit.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import static org.junit.Assert.assertTrue;

/** 验证紧凑窗口下工具栏的操作按钮不会被裁切。 */
public final class DataTableToolbarLayoutTest {
    @Test public void manyActionsWrapAtNarrowWidthWithoutClipping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DataTableToolbar toolbar = new DataTableToolbar("搜索", new String[]{"全部状态", "已发布"});
            java.util.List<JButton> buttons = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) { JButton button = new JButton("操作按钮" + i); buttons.add(button); toolbar.addAction(button); }
            for (int width : new int[]{360, 560, 800}) {
                toolbar.setSize(width, 400);
                for (int pass = 0; pass < 4; pass++) { layout(toolbar); toolbar.setSize(width, toolbar.getPreferredSize().height); }
                layout(toolbar);
                for (JButton button : buttons) assertInside(toolbar, button);
            }
        });
    }
    @Test public void actionsStayInsideToolbarAtCompactWidth() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            DataTableToolbar toolbar = new DataTableToolbar("搜索课程", new String[]{"全部状态", "已发布"});
            JButton first = new JButton("查看课程");
            JButton second = new JButton("新建课程");
            JButton third = new JButton("分配教师");
            toolbar.addAction(first); toolbar.addAction(second); toolbar.addAction(third);
            toolbar.setSize(798, 90); layout(toolbar);
            Rectangle search = javax.swing.SwingUtilities.convertRectangle(
                    toolbar.getSearchField().getParent(), toolbar.getSearchField().getBounds(), toolbar);
            assertTrue("search should stay in the controls row", search.x >= 0
                    && search.x + search.width <= toolbar.getWidth());
            assertInside(toolbar, first); assertInside(toolbar, second); assertInside(toolbar, third);
            assertTrue(toolbar.getPreferredSize().height >= 60);
            }
        });
    }

    private static void assertInside(DataTableToolbar toolbar, JButton button) {
        Rectangle bounds = javax.swing.SwingUtilities.convertRectangle(button.getParent(),
                button.getBounds(), toolbar);
        assertTrue("button must be fully visible", bounds.x >= 0 && bounds.y >= 0
                && bounds.x + bounds.width <= toolbar.getWidth()
                && bounds.y + bounds.height <= toolbar.getHeight());
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }
}
