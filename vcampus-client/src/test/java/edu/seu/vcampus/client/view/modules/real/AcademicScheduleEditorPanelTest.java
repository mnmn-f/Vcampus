package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.threeten.bp.LocalDate;

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

    @Test public void reversedOptionalDatesAreRejectedInline() {
        CountingGateway gateway = new CountingGateway();
        CourseScheduleEditorPanel panel = new CourseScheduleEditorPanel(page(),
                new AcademicClientService(new NetworkClientService(gateway)));
        panel.showCourse(course());
        List<DormDateField> dates = new ArrayList<DormDateField>(); collectDates(panel, dates);
        dates.get(0).setDate(LocalDate.of(2026, 9, 30));
        dates.get(1).setDate(LocalDate.of(2026, 9, 1));
        findButton(panel, "保存时段").doClick();
        assertTrue(hasText(panel, "结束日期不能早于起始日期"));
        assertEquals(0, gateway.calls);
    }

    @Test public void availableClassroomsLoadAndExistingRoomRefills() {
        ClassroomDto room = new ClassroomDto(9L, "九龙湖计算机楼", "B201", "LAB", 80, "AVAILABLE");
        CourseScheduleDto schedule = new CourseScheduleDto(3L, 7L, 2, 3, 4,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), room);
        CourseScheduleEditorPanel panel = new CourseScheduleEditorPanel(page(),
                new AcademicClientService(new NetworkClientService(new CountingGateway())));
        panel.setAvailableClassrooms(Collections.singletonList(room));
        panel.showCourse(course(Collections.singletonList(schedule)));
        JTable table = find(panel, JTable.class); table.setRowSelectionInterval(0, 0);
        JComboBox<?> rooms = classroomCombo(panel);
        assertEquals(2, rooms.getItemCount());
        assertTrue(String.valueOf(rooms.getSelectedItem()).contains("九龙湖计算机楼 B201"));
        List<DormDateField> dates = new ArrayList<DormDateField>(); collectDates(panel, dates);
        assertEquals(LocalDate.of(2026, 9, 1), dates.get(0).getDate());
        assertEquals(LocalDate.of(2026, 12, 31), dates.get(1).getDate());
    }

    private static CourseDto course() {
        return course(Collections.<CourseScheduleDto>emptyList());
    }
    private static CourseDto course(List<CourseScheduleDto> schedules) {
        return new CourseDto(7L, "CS101", "程序设计基础", "REQUIRED", BigDecimal.ONE,
                Integer.valueOf(32), 100, 0L, "", "PUBLISHED",
                schedules,
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
    private static void collectDates(Component root, List<DormDateField> result) {
        if (root instanceof DormDateField) result.add((DormDateField) root);
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) collectDates(child, result);
    }
    private static JComboBox<?> classroomCombo(Component root) {
        List<JComboBox> values = new ArrayList<JComboBox>(); collectCombos(root, values);
        for (JComboBox value : values) if (value.getItemCount() > 0
                && String.valueOf(value.getItemAt(0)).contains("不指定教室")) return value;
        throw new AssertionError("classroom combo not found");
    }
    private static void collectCombos(Component root, List<JComboBox> result) {
        if (root instanceof JComboBox) result.add((JComboBox) root);
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) collectCombos(child, result);
    }
    private static <T extends Component> T find(Component root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            T found=find(child,type); if(found!=null)return found;
        }
        return null;
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
