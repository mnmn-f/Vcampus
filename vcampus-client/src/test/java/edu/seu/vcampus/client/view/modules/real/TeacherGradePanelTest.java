package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.student.StudentDetailDto;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 教师选课、选学生并复用 GRADE_RECORD 的 Swing 闭环。 */
public final class TeacherGradePanelTest {
    @Test public void selectedRosterEntrySuppliesEnrollmentIdToExistingGradeService()
            throws Exception {
        final AcademicGateway gateway = new AcademicGateway();
        final RecordingGradeService grades = new RecordingGradeService();
        final AtomicReference<TeacherGradePanel> reference =
                new AtomicReference<TeacherGradePanel>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                reference.set(new TeacherGradePanel(page(),
                        new AcademicClientService(new NetworkClientService(gateway)), grades));
            }
        });
        assertTrue(gateway.courses.await(2, TimeUnit.SECONDS));
        assertTrue(gateway.roster.await(2, TimeUnit.SECONDS));
        final TeacherGradePanel panel = reference.get();
        waitForRoster(panel);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                table(panel).setRowSelectionInterval(0, 0);
                text(panel, "score").setText("91.5");
                area(panel, "remark").setText("平时成绩良好");
                findButton(panel, "登记 / 修改成绩").doClick();
            }
        });

        assertTrue(grades.saved.await(2, TimeUnit.SECONDS));
        assertEquals(731L, grades.request.get().getEnrollmentId());
        assertEquals(new BigDecimal("91.5"), grades.request.get().getScore());
        assertEquals(null, grades.request.get().getGradePoint());
        assertFalse(hasLabel(panel, "选课记录编号"));
        assertEquals(1, table(panel).getRowCount());
        assertEquals("S007", table(panel).getValueAt(0, 0));
    }

    private static void waitForRoster(final TeacherGradePanel panel) throws Exception {
        for (int i = 0; i < 20; i++) {
            final int[] rows = new int[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override public void run() { rows[0] = table(panel).getRowCount(); }
            });
            if (rows[0] == 1) return;
            Thread.sleep(50L);
        }
        throw new AssertionError("roster row was not displayed");
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(Object target, String name) {
        try {
            Field field = TeacherGradePanel.class.getDeclaredField(name);
            field.setAccessible(true); return (T) field.get(target);
        } catch (Exception ex) { throw new AssertionError(ex); }
    }
    private static JTable table(TeacherGradePanel panel) { return value(panel, "roster"); }
    private static JTextField text(TeacherGradePanel panel, String name) { return value(panel, name); }
    private static JTextArea area(TeacherGradePanel panel, String name) { return value(panel, name); }

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton && text.equals(((JButton) root).getText())) return (JButton) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JButton found = findButton(child, text); if (found != null) return found;
        }
        return null;
    }

    private static boolean hasLabel(Component root, String text) {
        if (root instanceof javax.swing.JLabel
                && text.equals(((javax.swing.JLabel) root).getText())) return true;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            if (hasLabel(child, text)) return true;
        }
        return false;
    }

    private static BasePage page() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(10L, "teacher", "教师", Role.TEACHER, "token"));
        return new BasePage(session, "测试", "") { };
    }

    private static final class AcademicGateway implements ClientGateway {
        private final CountDownLatch courses = new CountDownLatch(1);
        private final CountDownLatch roster = new CountDownLatch(1);
        @Override public Message send(Message request) {
            if (AcademicCommands.TEACHER_COURSES.equals(request.getCommand())) {
                courses.countDown();
                CourseDto course = new CourseDto(19L, "CS101", "程序设计", "REQUIRED",
                        BigDecimal.ONE, 32, 80, 1L, "", "PUBLISHED",
                        Collections.emptyList(), Collections.emptyList());
                return Message.success(request, new CoursePageDto(1, 100, 1L,
                        Collections.singletonList(course)));
            }
            if (AcademicCommands.COURSE_ROSTER.equals(request.getCommand())) {
                roster.countDown();
                CourseRosterEntryDto entry = new CourseRosterEntryDto(731L, 7L, "S007",
                        "学生七", "计算机学院", "软件工程", "软工2601", "ENROLLED",
                        LocalDateTime.now());
                return Message.success(request, new CourseRosterDto(19L,
                        Collections.singletonList(entry)));
            }
            return Message.failure(request, "TEST.UNEXPECTED", "unexpected command");
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }

    private static final class RecordingGradeService implements StudentRecordClientService {
        private final AtomicReference<StudentGradeRecordRequest> request =
                new AtomicReference<StudentGradeRecordRequest>();
        private final CountDownLatch saved = new CountDownLatch(1);
        @Override public StudentGradeDto recordGrade(StudentGradeRecordRequest value) {
            request.set(value); saved.countDown(); return null;
        }
        @Override public StudentProfileDto getOwnProfile() throws NetworkClientException { return null; }
        @Override public StudentGradePage getOwnGrades(StudentGradeQuery query) throws NetworkClientException { return null; }
        @Override public edu.seu.vcampus.common.dto.student.StudentGradeReportDto
                getOwnGradeReport(StudentGradeQuery query) throws NetworkClientException {
            return null;
        }
        @Override public edu.seu.vcampus.common.dto.student.StudentGradeExportDto
                exportOwnGrades(edu.seu.vcampus.common.dto.student.StudentGradeExportQuery query)
                throws NetworkClientException {
            return null;
        }
        @Override public StudentProfilePage searchProfiles(StudentProfileQuery query) throws NetworkClientException { return null; }
        @Override public StudentDetailDto getProfileDetail(long id) throws NetworkClientException { return null; }
        @Override public StudentProfileDto createProfile(StudentProfileWriteRequest request) throws NetworkClientException { return null; }
        @Override public StudentProfileDto updateProfile(StudentProfileWriteRequest request) throws NetworkClientException { return null; }
        @Override public StudentGradePage reviewGrades(StudentGradeReviewQuery query) throws NetworkClientException { return null; }
    }
}
