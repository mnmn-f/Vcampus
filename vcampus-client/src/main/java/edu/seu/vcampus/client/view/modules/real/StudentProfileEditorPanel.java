package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateDto;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import org.threeten.bp.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import java.awt.GridLayout;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** 学籍管理员维护档案；新档案必须从待建档账号选择。 */
public final class StudentProfileEditorPanel extends SectionCard {
    public interface Listener {
        void onCreate(StudentProfileCreateRequest request);
        void onUpdate(StudentProfileWriteRequest request);
    }

    private final JComboBox<StudentAccountCandidateDto> account = new JComboBox<StudentAccountCandidateDto>();
    private final JComboBox<String> college = StudentDirectoryOptions.collegeBox();
    private final JComboBox<String> major = StudentDirectoryOptions.majorBox();
    private final JComboBox<String> className = StudentDirectoryOptions.classBox();
    private final JComboBox<Integer> enrollmentYear = new JComboBox<Integer>(years());
    private final JComboBox<Integer> graduationYear = new JComboBox<Integer>(years());
    private final JComboBox<RealUi.CodeOption> degreeLevel = new JComboBox<RealUi.CodeOption>(
            RealUi.options("UNDERGRADUATE", "MASTER", "DOCTORATE"));
    private final JComboBox<RealUi.CodeOption> gender = new JComboBox<RealUi.CodeOption>(
            RealUi.options("MALE", "FEMALE", "UNKNOWN", "OTHER"));
    private final JComboBox<StudentStatus> status = new JComboBox<StudentStatus>(StudentStatus.values());
    private final JTextField studentNo = field();
    private final JTextField address = field();
    private final JTextField emergencyContact = field();
    private final JTextField emergencyPhone = field();
    private final JSpinner birthDate = new JSpinner(new SpinnerDateModel());
    private final JCheckBox noBirthDate = new JCheckBox("不填写");
    private final JLabel error = new JLabel(" ");
    private final Listener listener;
    private long selectedUserId;
    private String editingAccount;
    private boolean update;
    private boolean changingAccount;
    public StudentProfileEditorPanel(Listener listener) {
        super("档案维护", "");
        this.listener = listener; configure();
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 8)); form.setOpaque(false);
        add(form, "校园账号", account); add(form, "学号", studentNo);
        add(form, "学院", college); add(form, "专业", major);
        add(form, "班级", className); add(form, "入学年份", enrollmentYear);
        add(form, "预计毕业年份", graduationYear); add(form, "学历层次", degreeLevel);
        add(form, "性别", gender); add(form, "学籍状态", status);
        add(form, "出生日期", birthDate); add(form, "", noBirthDate);
        add(form, "地址", address); add(form, "紧急联系人", emergencyContact);
        add(form, "紧急联系电话", emergencyPhone);
        setContent(form); getBody().setBorder(BorderFactory.createEmptyBorder());
        error.setForeground(DesignTokens.ERROR); error.setFont(DesignTokens.regular(12));
        getBody().add(error, java.awt.BorderLayout.SOUTH);
        JButton save = new PrimaryButton("保存"); save.addActionListener(e -> save());
        JPanel actions = UiFactory.horizontal(8); actions.add(save);
        getBody().add(actions, java.awt.BorderLayout.NORTH); startNew();
    }
    public void setCandidates(List<StudentAccountCandidateDto> values) {
        if (update) return;
        String selected = selectedAccount(); account.removeAllItems();
        if (values != null) for (StudentAccountCandidateDto value : values) account.addItem(value);
        selectAccount(selected); if (!update) account.setEnabled(true);
    }
    public void startNew() {
        prepareCreate(null);
    }

    private void prepareCreate(String accountValue) {
        update = false; selectedUserId = 0L; editingAccount = null; account.setEnabled(true);
        changingAccount = true;
        if (accountValue == null) account.setSelectedItem(null); else selectAccount(accountValue);
        changingAccount = false;
        studentNo.setText(""); select(college, "请选择"); refreshMajors();
        select(className, "请选择"); enrollmentYear.setSelectedItem(null); graduationYear.setSelectedItem(null);
        degreeLevel.setSelectedIndex(-1); gender.setSelectedIndex(-1); status.setSelectedItem(StudentStatus.ENROLLED);
        noBirthDate.setSelected(true); clearText(); error.setText(" ");
    }

    public void showProfile(StudentProfileDto value) {
        if (value == null) { startNew(); return; }
        changingAccount = true; update = true; selectedUserId = value.getUserId();
        editingAccount = value.getAccount(); account.setEnabled(true);
        if (findAccount(value.getAccount()) < 0) account.addItem(new StudentAccountCandidateDto(
                value.getAccount(), value.getDisplayName(), null, null, null));
        selectAccount(value.getAccount()); studentNo.setText(text(value.getStudentNo()));
        select(college, text(value.getCollege())); refreshMajors(); select(major, text(value.getMajor()));
        refreshClasses(); select(className, text(value.getClassName())); select(enrollmentYear, value.getEnrollmentYear());
        select(graduationYear, value.getExpectedGraduationYear()); selectCode(degreeLevel, value.getDegreeLevel());
        selectCode(gender, value.getGender()); select(status, value.getStatus());
        if (value.getBirthDate() == null) noBirthDate.setSelected(true);
        else { noBirthDate.setSelected(false); birthDate.setValue(java.sql.Date.valueOf(value.getBirthDate().toString())); }
        address.setText(text(value.getAddress())); emergencyContact.setText(text(value.getEmergencyContact()));
        emergencyPhone.setText(text(value.getEmergencyPhone())); error.setText(" "); changingAccount = false;
    }

    private void configure() {
        RealUi.codeRenderer(status); RealUi.codeRenderer(degreeLevel); RealUi.codeRenderer(gender);
        birthDate.setEditor(new JSpinner.DateEditor(birthDate, "yyyy-MM-dd"));
        birthDate.setFont(DesignTokens.regular(13)); major.addActionListener(e -> refreshClasses());
        college.addActionListener(e -> { refreshMajors(); refreshClasses(); });
        account.addActionListener(e -> {
            if (changingAccount || !update) return;
            String selected = selectedAccount();
            if (selected != null && !selected.equals(editingAccount)) prepareCreate(selected);
        });
    }

    private void save() {
        try {
            String no = required(studentNo.getText(), "学号");
            String collegeValue = requiredChoice(StudentDirectoryOptions.selected(college), "学院");
            String majorValue = requiredChoice(StudentDirectoryOptions.selected(major), "专业");
            String classValue = requiredChoice(StudentDirectoryOptions.selected(className), "班级");
            Integer enroll = requiredItem(enrollmentYear, "入学年份");
            Integer graduate = requiredItem(graduationYear, "预计毕业年份");
            String degree = requiredCode(degreeLevel, "学历层次");
            String sex = requiredCode(gender, "性别");
            if (update) listener.onUpdate(write(selectedUserId, no));
            else listener.onCreate(new StudentProfileCreateRequest(requiredAccount(), no,
                    collegeValue, majorValue, classValue, enroll, graduate, degree, sex, date(),
                    optional(address), optional(emergencyContact), optional(emergencyPhone),
                    (StudentStatus) status.getSelectedItem()));
            error.setText(" ");
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private StudentProfileWriteRequest write(long id, String no) {
        return new StudentProfileWriteRequest(id, no, StudentDirectoryOptions.selected(college),
                StudentDirectoryOptions.selected(major), StudentDirectoryOptions.selected(className),
                selected(enrollmentYear), selected(graduationYear),
                code(degreeLevel), code(gender), date(), optional(address),
                optional(emergencyContact), optional(emergencyPhone), (StudentStatus) status.getSelectedItem());
    }

    private String requiredAccount() {
        StudentAccountCandidateDto value = (StudentAccountCandidateDto) account.getSelectedItem();
        if (value == null) throw new IllegalArgumentException("请选择待建档账号");
        return value.getAccount();
    }

    private LocalDate date() {
        if (noBirthDate.isSelected()) return null;
        Calendar value = Calendar.getInstance(); value.setTime((Date) birthDate.getValue());
        return LocalDate.of(value.get(Calendar.YEAR), value.get(Calendar.MONTH) + 1,
                value.get(Calendar.DAY_OF_MONTH));
    }

    private void refreshMajors() { StudentDirectoryOptions.majorsFor(major, StudentDirectoryOptions.selected(college)); }
    private void refreshClasses() { StudentDirectoryOptions.classesFor(className, StudentDirectoryOptions.selected(major)); }
    private <T> T selected(JComboBox<T> box) { return box.getSelectedIndex() < 0 ? null : (T) box.getSelectedItem(); }
    private static String code(JComboBox<RealUi.CodeOption> box) { return RealUi.code(box.getSelectedItem()); }
    private static String requiredChoice(String value, String label) {
        if (value == null) throw new IllegalArgumentException("请选择" + label);
        return value;
    }
    private static String requiredCode(JComboBox<RealUi.CodeOption> box, String label) {
        if (box.getSelectedIndex() < 0) throw new IllegalArgumentException("请选择" + label);
        return code(box);
    }
    private static <T> T requiredItem(JComboBox<T> box, String label) {
        if (box.getSelectedIndex() < 0) throw new IllegalArgumentException("请选择" + label);
        return box.getItemAt(box.getSelectedIndex());
    }
    private static String optional(JTextField field) { return field.getText().trim().isEmpty() ? null : field.getText().trim(); }
    private static String required(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + "不能为空"); return value.trim(); }
    private static void add(JPanel panel, String label, java.awt.Component field) { panel.add(UiFactory.labelledField(label, field)); }
    private static JTextField field() { return UiFactory.textField(12); }
    private static Integer[] years() { Integer[] values = new Integer[41]; for (int i = 0; i < values.length; i++) values[i] = 2000 + i; return values; }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private String selectedAccount() { StudentAccountCandidateDto value = (StudentAccountCandidateDto) account.getSelectedItem(); return value == null ? null : value.getAccount(); }
    private void selectAccount(String value) { for (int i = 0; i < account.getItemCount(); i++) if (value != null && value.equals(account.getItemAt(i).getAccount())) { account.setSelectedIndex(i); return; } }
    private int findAccount(String value) { for (int i = 0; i < account.getItemCount(); i++) if (value != null && value.equals(account.getItemAt(i).getAccount())) return i; return -1; }
    private static <T> void select(JComboBox<T> box, T value) {
        if (value == null || "".equals(value)) { box.setSelectedIndex(0); return; }
        for (int i = 0; i < box.getItemCount(); i++) if (value.equals(box.getItemAt(i))) { box.setSelectedIndex(i); return; }
        // 已有档案来自服务器，目录未包含的历史值必须保留，不能默默改成另一个学院/专业。
        box.addItem(value); box.setSelectedItem(value);
    }
    private static void selectCode(JComboBox<RealUi.CodeOption> box, String value) {
        if (value == null) { box.setSelectedIndex(-1); return; }
        for (int i = 0; i < box.getItemCount(); i++) {
            RealUi.CodeOption item = box.getItemAt(i);
            if (value.equalsIgnoreCase(item.getCode()) || value.equals(item.toString())) { box.setSelectedIndex(i); return; }
        }
        box.setSelectedIndex(-1);
    }
    private void clearText() { address.setText(""); emergencyContact.setText(""); emergencyPhone.setText(""); }
}
