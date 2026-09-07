package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 课程列表筛选控件；学生只筛可见的已发布课程，不展示无效生命周期筛选。 */
final class AcademicCourseFilters {
    private final Role role;
    private final JTextField courseCode = UiFactory.textField(10);
    private final JComboBox<Object> courseType = combo("全部类型", CourseType.values());
    private final JComboBox<Object> courseStatus = combo("全部状态", CourseStatus.values());

    AcademicCourseFilters(Role role) { this.role = role; }

    void attach(final AsyncPagedTable<?> table) {
        JPanel panel = UiFactory.horizontal(8);
        panel.add(UiFactory.body("课程编号")); panel.add(courseCode);
        panel.add(UiFactory.body("课程类型")); panel.add(courseType);
        if (role != Role.STUDENT) { panel.add(UiFactory.body("课程状态")); panel.add(courseStatus); }
        SecondaryButton reset = new SecondaryButton("重置");
        reset.addActionListener(e -> { courseCode.setText(""); courseType.setSelectedIndex(0);
            courseStatus.setSelectedIndex(0); table.resetFilters(); });
        panel.add(reset);
        table.setAdditionalFilters(panel, () -> courseCode.getText().trim().length() > 0
                || courseType.getSelectedIndex() > 0 || courseStatus.getSelectedIndex() > 0);
        java.awt.event.ActionListener reload = e -> table.reload();
        courseCode.addActionListener(reload); courseType.addActionListener(reload); courseStatus.addActionListener(reload);
    }

    String courseCode() { return courseCode.getText(); }
    String courseType() { return selected(courseType); }
    String courseStatus() { return selected(courseStatus); }

    private static JComboBox<Object> combo(String all, Enum<?>[] values) {
        Object[] items = new Object[values.length + 1]; items[0] = all;
        System.arraycopy(values, 0, items, 1, values.length);
        JComboBox<Object> result = new JComboBox<Object>(items); RealUi.codeRenderer(result); return result;
    }

    private static String selected(JComboBox<Object> box) {
        Object value = box.getSelectedItem(); return value instanceof Enum ? ((Enum<?>) value).name() : null;
    }
}
