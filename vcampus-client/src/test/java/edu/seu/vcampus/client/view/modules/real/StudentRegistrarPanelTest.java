package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
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
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 学籍页保留档案查询，建档和账号选择不暴露内部编号。 */
public final class StudentRegistrarPanelTest {
    @Test
    public void combinedFiltersUseFirstPage() throws Exception {
        final RecordingService service = new RecordingService();
        final AtomicReference<StudentRegistrarPanel> reference = new AtomicReference<StudentRegistrarPanel>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() { reference.set(new StudentRegistrarPanel(page(), service)); }
        });
        assertTrue(service.queries.poll(2, TimeUnit.SECONDS) != null);
        final StudentRegistrarPanel panel = reference.get();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                textField(panel, "studentNo").setText(" S001 ");
                combo(panel, "college").setSelectedItem("电气工程学院");
                combo(panel, "major").setSelectedItem("电气工程及其自动化");
                combo(panel, "className").setSelectedItem("电气2601");
                findSearchField(panel).setText("学生一");
                findStatusBox(panel).setSelectedItem("在读");
            }
        });
        assertTrue(service.queries.poll(2, TimeUnit.SECONDS) != null);
        service.queries.clear();
        click(panel, "查询");
        StudentProfileQuery combined = service.queries.poll(2, TimeUnit.SECONDS);
        assertEquals("S001", combined.getStudentNo());
        assertEquals("学生一", combined.getDisplayName());
        assertEquals("电气工程学院", combined.getCollege());
        assertEquals("电气工程及其自动化", combined.getMajor());
        assertEquals("电气2601", combined.getClassName());
        assertEquals(StudentStatus.ENROLLED, combined.getStatus());
        assertEquals(1, combined.getPage());

        assertNull(findButton(panel, "重置"));
        assertNull(findButton(panel, "新建档案"));
        assertNull(findButton(panel, "成绩档案核对"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(StudentRegistrarPanel panel, String name) {
        try {
            Field field = StudentRegistrarPanel.class.getDeclaredField(name);
            field.setAccessible(true); return (T) field.get(panel);
        } catch (Exception ex) { throw new AssertionError(ex); }
    }

    private static JTextField textField(StudentRegistrarPanel panel, String name) {
        return field(panel, name);
    }

    private static JComboBox<?> combo(StudentRegistrarPanel panel, String name) {
        return field(panel, name);
    }

    private static JTextField findSearchField(Component root) {
        if (root instanceof JTextField && "输入姓名".equals(((JTextField) root).getToolTipText())) return (JTextField) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JTextField found = findSearchField(child); if (found != null) return found;
        }
        return null;
    }

    private static JComboBox<?> findStatusBox(Component root) {
        if (root instanceof JComboBox && ((JComboBox<?>) root).getItemCount() == StudentStatus.values().length + 1) return (JComboBox<?>) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JComboBox<?> found = findStatusBox(child); if (found != null) return found;
        }
        return null;
    }

    private static void click(final Component root, final String text) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() { findButton(root, text).doClick(); }
        });
    }

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton && text.equals(((JButton) root).getText())) return (JButton) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JButton found = findButton(child, text); if (found != null) return found;
        }
        return null;
    }

    private static BasePage page() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(9L, "registrar", "学籍管理员", Role.REGISTRAR, "token-9"));
        return new BasePage(session, "测试", "") { };
    }

    private static final class RecordingService implements StudentRecordClientService {
        private final BlockingQueue<StudentProfileQuery> queries = new LinkedBlockingQueue<StudentProfileQuery>();
        @Override public StudentProfilePage searchProfiles(StudentProfileQuery query) {
            queries.add(query);
            return new StudentProfilePage(Collections.<StudentProfileDto>emptyList(), 0L,
                    query.getPage(), query.getPageSize());
        }
        @Override public StudentGradePage reviewGrades(StudentGradeReviewQuery query) {
            return new StudentGradePage(Collections.<StudentGradeDto>emptyList(), 0L,
                    query.getPage(), query.getPageSize());
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
        @Override public StudentDetailDto getProfileDetail(long id) throws NetworkClientException { return null; }
        @Override public StudentProfileDto createProfile(StudentProfileWriteRequest request) throws NetworkClientException { return null; }
        @Override public StudentProfileDto updateProfile(StudentProfileWriteRequest request) throws NetworkClientException { return null; }
        @Override public StudentGradeDto recordGrade(StudentGradeRecordRequest request) throws NetworkClientException { return null; }
    }
}
