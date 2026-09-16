package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.InputLimiter;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.validation.InputRules;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.security.SecureRandom;

/** 学籍管理员直接录入新生账号和基本学籍。 */
final class StudentAdmissionForm extends JPanel {
    private boolean synchronizing;
    private final JTextField name = field();
    private final JTextField studentNo = field();
    private final JTextField password = field();
    private final JComboBox<String> college = StudentDirectoryOptions.collegeBox();
    private final JComboBox<String> major = StudentDirectoryOptions.majorBox();
    private final JComboBox<String> className = StudentDirectoryOptions.classBox();
    private final JComboBox<Integer> enrollmentYear = new JComboBox<Integer>(years());
    private final JComboBox<Integer> graduationYear = new JComboBox<Integer>(years());
    private final JComboBox<RealUi.CodeOption> degree = new JComboBox<RealUi.CodeOption>(
            RealUi.options("UNDERGRADUATE", "MASTER", "DOCTORATE"));
    private final JComboBox<RealUi.CodeOption> gender = new JComboBox<RealUi.CodeOption>(
            RealUi.options("UNKNOWN", "MALE", "FEMALE", "OTHER"));

    StudentAdmissionForm() {
        super(new GridLayout(0, 2, 12, 8)); setOpaque(false);
        InputLimiter.personName(name); InputLimiter.studentNumber(studentNo); InputLimiter.length(password, 72);
        add(UiFactory.labelledField("姓名", name));
        add(UiFactory.labelledField("学号", studentNo));
        add(UiFactory.labelledField("初始密码", password));
        add(UiFactory.labelledField("学院", college));
        add(UiFactory.labelledField("专业", major));
        add(UiFactory.labelledField("班级", className));
        add(UiFactory.labelledField("入学年份", enrollmentYear));
        add(UiFactory.labelledField("预计毕业年份", graduationYear));
        add(UiFactory.labelledField("学历层次", degree));
        add(UiFactory.labelledField("性别", gender));
        RealUi.codeRenderer(degree); RealUi.codeRenderer(gender);
        college.addActionListener(e -> refreshMajors());
        major.addActionListener(e -> refreshClasses());
        className.addActionListener(e -> synchronizeFromClass());
        enrollmentYear.addActionListener(e -> synchronizeGraduationYear());
        degree.addActionListener(e -> synchronizeGraduationYear());
        enrollmentYear.setEnabled(false);
        graduationYear.setEnabled(false);
        password.setText(initialPassword());
        enrollmentYear.setSelectedItem(Integer.valueOf(java.time.Year.now().getValue()));
        graduationYear.setSelectedItem(Integer.valueOf(java.time.Year.now().getValue() + 4));
        degree.setSelectedItem(RealUi.option("UNDERGRADUATE"));
        gender.setSelectedItem(RealUi.option("UNKNOWN"));
    }

    StudentProfileCreateRequest request() {
        synchronizeFromClass();
        String no = InputRules.studentNumber(studentNo.getText());
        return new StudentProfileCreateRequest(no, InputRules.personName(name.getText(), "姓名"),
                InputRules.password(password.getText(), "初始密码"), no,
                requiredChoice(StudentDirectoryOptions.selected(college), "学院"),
                requiredChoice(StudentDirectoryOptions.selected(major), "专业"),
                requiredChoice(StudentDirectoryOptions.selected(className), "班级"),
                selected(enrollmentYear, "入学年份"), selected(graduationYear, "预计毕业年份"),
                RealUi.code(degree.getSelectedItem()), RealUi.code(gender.getSelectedItem()),
                null, null, null, null, StudentStatus.ENROLLED);
    }

    private void refreshMajors() {
        StudentDirectoryOptions.majorsFor(major, StudentDirectoryOptions.selected(college));
        refreshClasses();
    }
    private void refreshClasses() {
        StudentDirectoryOptions.classesFor(className, StudentDirectoryOptions.selected(major));
    }
    private void synchronizeFromClass() {
        if (synchronizing) return;
        String selectedClass = StudentDirectoryOptions.selected(className);
        String inferredMajor = StudentDirectoryOptions.majorForClass(selectedClass);
        if (inferredMajor == null) return;
        String inferredCollege = StudentDirectoryOptions.collegeForMajor(inferredMajor);
        synchronizing = true;
        try {
            if (inferredCollege != null && !inferredCollege.equals(StudentDirectoryOptions.selected(college))) {
                college.setSelectedItem(inferredCollege);
                StudentDirectoryOptions.majorsFor(major, inferredCollege);
            }
            if (!inferredMajor.equals(StudentDirectoryOptions.selected(major))) {
                major.setSelectedItem(inferredMajor);
            }
            StudentDirectoryOptions.classesFor(className, inferredMajor);
            className.setSelectedItem(selectedClass);
            Integer year = StudentDirectoryOptions.enrollmentYearForClass(selectedClass);
            if (year != null) enrollmentYear.setSelectedItem(year);
            synchronizeGraduationYear();
        } finally {
            synchronizing = false;
        }
    }
    private void synchronizeGraduationYear() {
        String cohort = StudentDirectoryOptions.selected(className);
        Integer cohortYear = edu.seu.vcampus.common.validation.StudentCohortYears.enrollment(cohort);
        if (cohortYear != null) {
            if (!cohortYear.equals(enrollmentYear.getSelectedItem())) enrollmentYear.setSelectedItem(cohortYear);
            graduationYear.setSelectedItem(edu.seu.vcampus.common.validation.StudentCohortYears.graduation(cohort));
            return;
        }
        Object value = enrollmentYear.getSelectedItem();
        if (!(value instanceof Integer)) return;
        String level = RealUi.code(degree.getSelectedItem());
        int duration = "MASTER".equals(level) ? 3 : 4;
        graduationYear.setSelectedItem(Integer.valueOf(((Integer) value).intValue() + duration));
    }
    private static String initialPassword() {
        SecureRandom random = new SecureRandom();
        return "Vc" + (100000 + random.nextInt(900000)) + "A";
    }
    private static String requiredChoice(String value, String label) {
        if (value == null) throw new IllegalArgumentException("请选择" + label);
        return value;
    }
    private static Integer selected(JComboBox<Integer> box, String label) {
        Object value = box.getSelectedItem();
        if (!(value instanceof Integer)) throw new IllegalArgumentException("请选择" + label);
        return (Integer) value;
    }
    private static JTextField field() { return UiFactory.textField(12); }
    private static Integer[] years() {
        Integer[] values = new Integer[41];
        for (int i = 0; i < values.length; i++) values[i] = Integer.valueOf(2000 + i);
        return values;
    }
}
