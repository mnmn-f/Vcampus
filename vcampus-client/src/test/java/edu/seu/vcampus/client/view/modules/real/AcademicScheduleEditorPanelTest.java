package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JSpinner;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 排课编辑器在提交前拦截明显的节次逆序，并将提示留在当前行。 */
public final class AcademicScheduleEditorPanelTest {
    @Test public void reversedPeriodsAreRejectedInline() {
        CountingGateway gateway = new CountingGateway();
        CourseScheduleEditorPanel panel = new CourseScheduleEditorPanel(page(),
                new AcademicClientService(new NetworkClientService(gateway)));
        panel.showCourse(course());
        List<JSpinner> spinners = new ArrayList<JSpinner>(); collect(panel, spinners);
        spinners.get(0).setValue(Integer.valueOf(4));
        spinners.get(1).setValue(Integer.valueOf(2));
        JButton save = findButton(panel, "保存时段");
        assertTrue(save != null);
        save.doClick();
        assertTrue(hasText(panel, "结束节次不能早于开始节次"));
        assertEquals(0, gateway.calls);
    }

    private static CourseDto course() {
        return new CourseDto(7L, "CS101", "程序设计基础", "REQUIRED", BigDecimal.ONE,
                Integer.valueOf(32), 100, 0L, "", "PUBLISHED",
                Collections.<edu.seu.vcampus.common.dto.academic.CourseScheduleDto>emptyList(),
                Collections.<edu.seu.vcampus.common.dto.academic.CourseInstructorDto>emptyList());
    }
    private static BasePage page() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "admin", "教务", Role.ACADEMIC_ADMIN, "token"));
        return new BasePage(session, "测试", "") { };
    }
    private static void collect(Component root, List<JSpinner> result) {
        if (root instanceof JSpinner) result.add((JSpinner) root);
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) collect(child, result);
    }
    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton && text.equals(((JButton) root).getText())) return (JButton) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JButton found = findButton(child, text); if (found != null) return found;
        }
        return null;
    }
    private static boolean hasText(Component root, String text) {
        if (root instanceof javax.swing.JLabel && String.valueOf(((javax.swing.JLabel) root).getText()).contains(text)) return true;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) if (hasText(child, text)) return true;
        return false;
    }
    private static final class CountingGateway implements ClientGateway {
        private int calls;
        @Override public Message send(Message request) { calls++; return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
