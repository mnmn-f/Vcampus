package edu.seu.vcampus.client.view;

import org.junit.Test;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 注册页使用紧凑的横向输入线，不允许表单布局把输入框拉成大方块。 */
public final class RegistrationPanelTest {
    @Test public void registrationFieldsStayCompactAndSingleColumn() {
        RegistrationPanel panel = new RegistrationPanel(null, () -> { });
        List<JTextField> fields = new ArrayList<JTextField>(); collect(panel, fields);
        assertEquals(5, fields.size());
        for (JTextField field : fields) {
            assertEquals(42, field.getPreferredSize().height);
            assertTrue(field.getBorder() instanceof javax.swing.border.CompoundBorder);
        }
    }

    private static void collect(Component value, List<JTextField> fields) {
        if (value instanceof JTextField) fields.add((JTextField) value);
        if (value instanceof Container) for (Component child : ((Container) value).getComponents()) {
            collect(child, fields);
        }
    }
}
