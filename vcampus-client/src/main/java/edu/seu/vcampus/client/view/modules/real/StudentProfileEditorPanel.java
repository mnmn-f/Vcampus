package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;
import org.threeten.bp.LocalDate;

/** 学籍管理员的行内档案表单；保存由上层异步提交。 */
public final class StudentProfileEditorPanel extends SectionCard {
    public interface Listener {
        void onSave(StudentProfileWriteRequest request, boolean update);
    }

    private final JTextField userId = field();
    private final JTextField studentNo = field();
    private final JTextField college = field();
    private final JTextField major = field();
    private final JTextField className = field();
    private final JTextField enrollmentYear = field();
    private final JTextField graduationYear = field();
    private final JTextField degreeLevel = field();
    private final JTextField gender = field();
    private final JTextField birthDate = field();
    private final JTextField address = field();
    private final JTextField emergencyContact = field();
    private final JTextField emergencyPhone = field();
    private final JComboBox<StudentStatus> status = new JComboBox<StudentStatus>(StudentStatus.values());
    private final JLabel error = new JLabel(" ");
    private final Listener listener;
    private boolean update;

    public StudentProfileEditorPanel(Listener listener) {
        super("档案详情与维护", "维护学生档案。");
        this.listener = listener;
        status.setFont(DesignTokens.regular(13)); RealUi.codeRenderer(status);
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 8));
        form.setOpaque(false);
        add(form, "用户编号", userId); add(form, "学号", studentNo);
        add(form, "学院", college); add(form, "专业", major);
        add(form, "班级", className); add(form, "入学年份", enrollmentYear);
        add(form, "预计毕业年份", graduationYear); add(form, "学籍状态", status);
        add(form, "学位层次", degreeLevel); add(form, "性别", gender);
        add(form, "出生日期（yyyy-MM-dd）", birthDate); add(form, "地址", address);
        add(form, "紧急联系人", emergencyContact); add(form, "紧急联系电话", emergencyPhone);
        getBody().setBorder(BorderFactory.createEmptyBorder());
        setContent(form);
        error.setForeground(DesignTokens.ERROR);
        error.setFont(DesignTokens.regular(12));
        getBody().add(error, java.awt.BorderLayout.SOUTH);
        JPanel actions = UiFactory.horizontal(8);
        JButton clear = new SecondaryButton("新建");
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        JButton save = new PrimaryButton("保存档案");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        actions.add(clear); actions.add(save);
        getBody().add(actions, java.awt.BorderLayout.NORTH);
        startNew();
    }

    public void startNew() {
        update = false; userId.setEditable(true); clearFields();
        status.setSelectedItem(StudentStatus.ENROLLED); error.setText(" ");
    }

    public void showProfile(StudentProfileDto value) {
        if (value == null) { startNew(); return; }
        update = true; userId.setText(String.valueOf(value.getUserId())); userId.setEditable(false);
        studentNo.setText(text(value.getStudentNo())); college.setText(text(value.getCollege()));
        major.setText(text(value.getMajor())); className.setText(text(value.getClassName()));
        enrollmentYear.setText(text(value.getEnrollmentYear()));
        graduationYear.setText(text(value.getExpectedGraduationYear()));
        degreeLevel.setText(text(value.getDegreeLevel())); gender.setText(text(value.getGender()));
        birthDate.setText(text(value.getBirthDate())); address.setText(text(value.getAddress()));
        emergencyContact.setText(text(value.getEmergencyContact())); emergencyPhone.setText(text(value.getEmergencyPhone()));
        status.setSelectedItem(value.getStatus()); error.setText(" ");
    }

    private void save() {
        try {
            long id = Long.parseLong(required(userId.getText(), "用户编号"));
            Integer year = optionalInt(enrollmentYear.getText(), "入学年份");
            Integer graduation = optionalInt(graduationYear.getText(), "预计毕业年份");
            LocalDate birthday = optionalDate(birthDate.getText());
            StudentProfileWriteRequest request = new StudentProfileWriteRequest(id,
                    required(studentNo.getText(), "学号"), optional(college.getText()),
                    optional(major.getText()), optional(className.getText()), year, graduation,
                    optional(degreeLevel.getText()), optional(gender.getText()), birthday,
                    optional(address.getText()), optional(emergencyContact.getText()),
                    optional(emergencyPhone.getText()),
                    (StudentStatus) status.getSelectedItem());
            if (listener != null) listener.onSave(request, update);
            error.setText(" ");
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
        catch (RuntimeException ex) { error.setText("出生日期格式不正确"); }
    }

    private void clearFields() {
        userId.setText(""); studentNo.setText(""); college.setText(""); major.setText("");
        className.setText(""); enrollmentYear.setText(""); graduationYear.setText(""); degreeLevel.setText("");
        gender.setText(""); birthDate.setText(""); address.setText(""); emergencyContact.setText(""); emergencyPhone.setText("");
    }

    private static void add(JPanel panel, String label, java.awt.Component field) {
        panel.add(UiFactory.labelledField(label, field));
    }

    private static JTextField field() { return UiFactory.textField(12); }
    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + "不能为空");
        return value.trim();
    }
    private static String optional(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private static Integer optionalInt(String value, String name) {
        String text = optional(value); if (text == null) return null;
        try { return Integer.valueOf(text); } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + "必须是数字");
        }
    }
    private static LocalDate optionalDate(String value) {
        String text = optional(value); return text == null ? null : LocalDate.parse(text);
    }
    private static String text(Object value) { return RealUi.input(value); }
}
