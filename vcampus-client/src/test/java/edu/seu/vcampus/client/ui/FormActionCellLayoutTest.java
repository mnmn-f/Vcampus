package edu.seu.vcampus.client.ui;

import edu.seu.vcampus.client.ui.components.PrimaryButton;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 表单操作按钮不会被响应式网格拉成输入框大小。 */
public final class FormActionCellLayoutTest {
    @Test public void primaryActionKeepsItsStandardSizeInsideTallGridCell() {
        JButton button = new PrimaryButton("保存促销");
        JPanel grid = new JPanel(new ResponsiveGridLayout(180, 2, 8));
        grid.add(UiFactory.labelledField("名称", UiFactory.textField(12)));
        grid.add(UiFactory.formActionCell(button));
        grid.setBounds(0, 0, 800, 120);
        layout(grid);
        assertEquals(button.getPreferredSize().height, button.getHeight());
        assertEquals(button.getPreferredSize().width, button.getWidth());
        assertTrue(button.getWidth() < 300);
    }

    @Test public void shortFieldsAreNotStretchedByTallCellsInTheSameGrid() {
        JTextField field = UiFactory.textField(12);
        JTextArea description = UiFactory.textArea(5, 24);
        JPanel grid = new JPanel(new ResponsiveGridLayout(180, 2, 8));
        grid.add(UiFactory.labelledField("名称", field));
        grid.add(UiFactory.labelledField("说明", description));
        grid.setBounds(0, 0, 800, 240);
        layout(grid);
        assertEquals(field.getPreferredSize().height, field.getHeight());
        assertTrue(description.getHeight() > field.getHeight());
    }

    private static void layout(Component component) {
        component.doLayout();
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) layout(child);
        }
    }
}
