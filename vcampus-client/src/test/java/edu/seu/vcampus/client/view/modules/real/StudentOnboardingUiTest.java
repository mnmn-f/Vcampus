package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.student.StudentAccountCandidateDto;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 学籍建档只从可读账号选择，字段选择后提交稳定值。 */
public final class StudentOnboardingUiTest {
    @Test public void cohortYearsAreLockedInBothForms() throws Exception {
        StudentAdmissionForm form = new StudentAdmissionForm();
        combo(form, "className").setSelectedItem("建筑学2026级2班");
        assertEquals(Integer.valueOf(2026), combo(form, "enrollmentYear").getSelectedItem());
        assertEquals(Integer.valueOf(2031), combo(form, "graduationYear").getSelectedItem());
        assertFalse(combo(form, "enrollmentYear").isEnabled());
        assertFalse(combo(form, "graduationYear").isEnabled());
        StudentProfileEditorPanel editor = new StudentProfileEditorPanel(null);
        combo(editor, "college").setSelectedItem("建筑学院");
        combo(editor, "major").setSelectedItem("建筑学");
        combo(editor, "className").setSelectedItem("建筑学2024级1班");
        assertEquals(Integer.valueOf(2024), combo(editor, "enrollmentYear").getSelectedItem());
        assertEquals(Integer.valueOf(2029), combo(editor, "graduationYear").getSelectedItem());
        assertFalse(combo(editor, "enrollmentYear").isEnabled());
        assertFalse(combo(editor, "graduationYear").isEnabled());
        combo(editor, "className").setSelectedItem("建筑学2026级2班");
        assertEquals(Integer.valueOf(2031), combo(editor, "graduationYear").getSelectedItem());
    }
    @Test public void admissionUsesStudentNumberAsLoginAndKeepsVisibleInitialPassword() throws Exception {
        StudentAdmissionForm form = new StudentAdmissionForm();
        text(form, "name").setText("王晨茜");
        text(form, "studentNo").setText("09024201");
        combo(form, "college").setSelectedItem("计算机科学与工程学院");
        combo(form, "major").setSelectedItem("计算机科学与技术");
        combo(form, "className").setSelectedItem("计算机科学与技术2026级1班");
        StudentProfileCreateRequest request = form.request();
        assertEquals("09024201", request.getAccount());
        assertEquals("09024201", request.getStudentNo());
        assertTrue(request.getInitialPassword().matches("Vc[0-9]{6}A"));
        String credentials = StudentRegistrarPanel.admissionCredentialText(request);
        assertTrue(credentials.contains("校园账号：09024201"));
        assertTrue(credentials.contains("初始密码：" + request.getInitialPassword()));
    }

    @Test public void selectingClassFillsDirectoryAndStudyYears() throws Exception {
        StudentAdmissionForm form = new StudentAdmissionForm();
        combo(form, "className").setSelectedItem("计算机科学与技术2024级1班");
        assertEquals("计算机科学与工程学院", combo(form, "college").getSelectedItem());
        assertEquals("计算机科学与技术", combo(form, "major").getSelectedItem());
        assertEquals(Integer.valueOf(2024), combo(form, "enrollmentYear").getSelectedItem());
        assertEquals(Integer.valueOf(2028), combo(form, "graduationYear").getSelectedItem());
        assertEquals("未填写", RealUi.status("UNKNOWN"));
    }

    @Test public void editorSubmitsSelectedAccountAndControlledValues() throws Exception {
        final AtomicReference<StudentProfileCreateRequest> sent = new AtomicReference<StudentProfileCreateRequest>();
        StudentProfileEditorPanel editor = new StudentProfileEditorPanel(new StudentProfileEditorPanel.Listener() {
            @Override public void onCreate(StudentProfileCreateRequest request) { sent.set(request); }
            @Override public void onUpdate(edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest request) { }
        });
        editor.setCandidates(Collections.singletonList(new StudentAccountCandidateDto(
                "student01", "王晨茜", null, null, null)));
        combo(editor, "account").setSelectedIndex(0); text(editor, "studentNo").setText("S-01");
        combo(editor, "college").setSelectedItem("软件学院");
        combo(editor, "major").setSelectedItem("软件工程"); combo(editor, "className").setSelectedItem("软件工程2026级1班");
        combo(editor, "enrollmentYear").setSelectedItem(2026); combo(editor, "graduationYear").setSelectedItem(2030);
        combo(editor, "degreeLevel").setSelectedIndex(0); combo(editor, "gender").setSelectedIndex(0);
        click(editor, "保存");
        assertEquals("student01", sent.get().getAccount()); assertEquals("UNDERGRADUATE", sent.get().getDegreeLevel());
        assertEquals("MALE", sent.get().getGender()); assertEquals("软件工程2026级1班", sent.get().getClassName());
    }

    @Test public void editorHasNoInternalUserIdFieldOrExtraCreateAction() throws Exception {
        StudentProfileEditorPanel editor = new StudentProfileEditorPanel(null);
        assertNull(findText(editor, "用户编号")); assertNull(findButton(editor, "新建"));
        assertTrue(findButton(editor, "保存") != null);
    }

    @Test public void directoryChoicesFollowCollegeAndMajor() throws Exception {
        StudentProfileEditorPanel editor = new StudentProfileEditorPanel(null);
        JComboBox<Object> colleges = combo(editor, "college");
        JComboBox<Object> majors = combo(editor, "major"); JComboBox<Object> classes = combo(editor, "className");
        colleges.setSelectedItem("电气工程学院");
        assertEquals("电气工程及其自动化", majors.getItemAt(1));
        majors.setSelectedItem("电气工程及其自动化");
        assertEquals("电气工程及其自动化2026级1班", classes.getItemAt(1));
    }

    @Test public void pendingAccountSelectionLeavesExistingProfileUpdateMode() throws Exception {
        final AtomicReference<StudentProfileCreateRequest> created =
                new AtomicReference<StudentProfileCreateRequest>();
        final AtomicReference<edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest> updated =
                new AtomicReference<edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest>();
        StudentProfileEditorPanel editor = new StudentProfileEditorPanel(new StudentProfileEditorPanel.Listener() {
            @Override public void onCreate(StudentProfileCreateRequest request) { created.set(request); }
            @Override public void onUpdate(edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest request) { updated.set(request); }
        });
        StudentAccountCandidateDto pending = new StudentAccountCandidateDto(
                "pending01", "待建档学生", null, null, null);
        editor.setCandidates(Collections.singletonList(pending));
        editor.showProfile(new StudentProfileDto(88L, "已有学生", "existing01", "S-88",
                "软件学院", "软件工程", "软件工程2026级1班", 2026, 2030,
                "UNDERGRADUATE", "MALE", null, null, null, null,
                edu.seu.vcampus.common.dto.student.StudentStatus.ENROLLED));
        combo(editor, "account").setSelectedItem(pending);
        text(editor, "studentNo").setText("S-89"); combo(editor, "college").setSelectedItem("软件学院");
        combo(editor, "major").setSelectedItem("软件工程"); combo(editor, "className").setSelectedItem("软件工程2026级1班");
        combo(editor, "enrollmentYear").setSelectedItem(2026); combo(editor, "graduationYear").setSelectedItem(2030);
        combo(editor, "degreeLevel").setSelectedIndex(0); combo(editor, "gender").setSelectedIndex(0);
        click(editor, "保存");
        assertEquals("pending01", created.get().getAccount()); assertNull(updated.get());
    }

    private static JTextField text(Object target, String name) throws Exception { return field(target, name); }
    @SuppressWarnings("unchecked") private static JComboBox<Object> combo(Object target, String name) throws Exception { return field(target, name); }
    @SuppressWarnings("unchecked") private static <T> T field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name); value.setAccessible(true); return (T) value.get(target);
    }
    private static void click(Component root, String value) { AbstractButton button = findButton(root, value); assertTrue(button != null); button.doClick(); }
    private static AbstractButton findButton(Component root, String value) {
        if (root instanceof AbstractButton && value.equals(((AbstractButton) root).getText())) return (AbstractButton) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) { AbstractButton found = findButton(child, value); if (found != null) return found; }
        return null;
    }
    private static JTextField findText(Component root, String value) {
        if (root instanceof JTextField && value.equals(((JTextField) root).getText())) return (JTextField) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) { JTextField found = findText(child, value); if (found != null) return found; }
        return null;
    }
}
