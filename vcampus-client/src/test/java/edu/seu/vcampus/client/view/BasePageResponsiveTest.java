package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class BasePageResponsiveTest {
    @Test public void wideContentShrinksAndActionsWrapInsidePage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton[] actions = new JButton[10];
            JPanel toolbar = UiFactory.horizontal(8);
            for (int i = 0; i < actions.length; i++) { actions[i] = new JButton("操作按钮" + i); toolbar.add(actions[i]); }
            JScrollPane table = new JScrollPane(new JTable(8, 6)); table.setPreferredSize(new Dimension(1200, 220));
            table.setMinimumSize(new Dimension(0, 120));
            JPanel content = new JPanel(new java.awt.BorderLayout()); content.add(toolbar, "North"); content.add(table, "Center");
            SectionCard card = new SectionCard("操作", ""); card.setContent(content);
            JPanel nested = UiFactory.vertical(8); nested.add(card);
            BasePage page = new BasePage(new ClientSession(), "测试", "") { { addBlock(new TaskTabs().addTask("列表", nested)); } };
            for (int width : new int[]{800, 960, 1280}) {
                page.setSize(width, 760);
                for (int pass = 0; pass < 12; pass++) { invalidate(page); layout(page); }
                JScrollPane outer = (JScrollPane) ((java.awt.BorderLayout) page.getLayout()).getLayoutComponent("Center");
                assertEquals(outer.getViewport().getWidth(), outer.getViewport().getView().getWidth());
                for (JButton action : actions) {
                    Rectangle bounds = SwingUtilities.convertRectangle(action.getParent(), action.getBounds(), page);
                    assertTrue("action outside page at " + width + ": " + bounds, bounds.x >= 0 && bounds.x + bounds.width < width);
                    assertTrue(action.getY() + action.getHeight() <= toolbar.getHeight());
                }
            }
        });
    }
    private static void invalidate(Component root) {
        root.invalidate(); if (root instanceof Container) for (Component child : ((Container) root).getComponents()) invalidate(child);
    }
    private static void layout(Component root) {
        root.doLayout(); if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }
}
