package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 教务管理员的课程行内新建/编辑表单。 */
public final class CourseEditorPanel extends SectionCard {
    public interface Listener { void onSave(CourseSaveRequest request, boolean update); }

    private final JTextField code = field();
    private final JTextField name = field();
    private final JTextField semester = field();
    private final JComboBox<CourseType> type = new JComboBox<CourseType>(CourseType.values());
    private final JTextField credits = field();
    private final JTextField hours = field();
    private final JTextField capacity = field();
    private final JComboBox<CourseStatus> status = new JComboBox<CourseStatus>(CourseStatus.values());
    private final JTextField teachers = field();
    private final JTextArea description = UiFactory.textArea(3, 28);
    private final JLabel error = UiFactory.muted(" ");
    private final Listener listener;
    private long courseId;
    private boolean update;

    public CourseEditorPanel(Listener listener) {
        super("课程详情与维护", "维护课程信息和授课教师。");
        this.listener = listener;
        type.setFont(DesignTokens.regular(13)); status.setFont(DesignTokens.regular(13)); RealUi.codeRenderer(type); RealUi.codeRenderer(status);
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        addField(fields, "课程编号", code); addField(fields, "课程名称", name);
        addField(fields, "学期编号", semester);
        addField(fields, "课程类型", type); addField(fields, "学分", credits);
        addField(fields, "总学时", hours); addField(fields, "容量", capacity);
        addField(fields, "状态", status); addField(fields, "教师编号（多个请用逗号分隔）", teachers);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(fields, BorderLayout.NORTH); content.add(UiFactory.labelledField("课程说明", description), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButtonPair pair = actions();
        actions.add(pair.clear); actions.add(pair.save); actions.add(error);
        content.add(actions, BorderLayout.SOUTH); setContent(content); startNew();
    }

    public void startNew() {
        courseId = 0; update = false; code.setEditable(true); clear();
        type.setSelectedItem(CourseType.REQUIRED); status.setSelectedItem(CourseStatus.DRAFT); error.setText(" ");
    }

    public void showCourse(CourseDto value) {
        if (value == null) { startNew(); return; }
        courseId = value.getId(); update = true; code.setEditable(false);
        code.setText(RealUi.input(value.getCourseCode())); name.setText(RealUi.input(value.getCourseName()));
        semester.setText(RealUi.input(value.getSemesterCode()));
        type.setSelectedItem(parseType(value.getCourseType())); credits.setText(RealUi.input(value.getCredits()));
        hours.setText(RealUi.input(value.getTotalHours())); capacity.setText(String.valueOf(value.getCapacity()));
        status.setSelectedItem(parseStatus(value.getStatus())); description.setText(value.getDescription() == null ? "" : value.getDescription());
        StringBuilder ids = new StringBuilder();
        for (edu.seu.vcampus.common.dto.academic.CourseInstructorDto teacher : value.getInstructors()) {
            if (ids.length() > 0) ids.append(','); ids.append(teacher.getTeacherUserId());
        }
        teachers.setText(ids.toString()); error.setText(" ");
    }

    private JButtonPair actions() {
        javax.swing.JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        javax.swing.JButton save = new PrimaryButton("保存课程"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        return new JButtonPair(clear, save);
    }

    private void save() {
        try {
            String courseCode = required(code.getText(), "课程编号"); String courseName = required(name.getText(), "课程名称");
            BigDecimal credit = new BigDecimal(required(credits.getText(), "学分"));
            Integer totalHours = Integer.valueOf(required(hours.getText(), "总学时"));
            Integer seats = Integer.valueOf(required(capacity.getText(), "容量"));
            String semesterCode = semester.getText().trim();
            if (semesterCode.length() == 0) semesterCode = "UNSPECIFIED";
            List<Long> ids = new ArrayList<Long>();
            for (String value : teachers.getText().split(",")) if (!value.trim().isEmpty()) ids.add(Long.valueOf(value.trim()));
            CourseSaveRequest request = update ? CourseSaveRequest.update(courseId, courseCode, courseName,
                    (CourseType) type.getSelectedItem(), credit, totalHours, seats, description.getText(),
                    (CourseStatus) status.getSelectedItem(), ids, semesterCode) : CourseSaveRequest.create(courseCode, courseName,
                    (CourseType) type.getSelectedItem(), credit, totalHours, seats, description.getText(),
                    (CourseStatus) status.getSelectedItem(), ids, semesterCode);
            if (listener != null) listener.onSave(request, update); error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("学分、学时、容量和教师编号必须是数字"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private void clear() { code.setText(""); name.setText(""); semester.setText(""); credits.setText(""); hours.setText(""); capacity.setText(""); teachers.setText(""); description.setText(""); }
    private static JTextField field() { return UiFactory.textField(12); }
    private static void addField(JPanel p, String label, java.awt.Component field) { p.add(UiFactory.labelledField(label, field)); }
    private static String required(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + "不能为空"); return value.trim(); }
    private static CourseType parseType(String value) { try { return CourseType.valueOf(value); } catch (Exception ex) { return CourseType.REQUIRED; } }
    private static CourseStatus parseStatus(String value) { try { return CourseStatus.valueOf(value); } catch (Exception ex) { return CourseStatus.DRAFT; } }
    private static final class JButtonPair { final javax.swing.JButton clear; final javax.swing.JButton save; JButtonPair(javax.swing.JButton c, javax.swing.JButton s) { clear = c; save = s; } }
}
