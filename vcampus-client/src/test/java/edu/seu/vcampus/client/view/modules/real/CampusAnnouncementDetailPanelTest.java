package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 公告详情应展示正文以及学生需要的完整时间和范围信息。 */
public final class CampusAnnouncementDetailPanelTest {
    @Test public void studentOpensDetailsWithOneClick() {
        assertTrue(CampusAnnouncementsPanel.opensOnRowClick(Role.STUDENT, 1));
        assertFalse(CampusAnnouncementsPanel.opensOnRowClick(Role.STUDENT, 2));
        assertFalse(CampusAnnouncementsPanel.opensOnRowClick(Role.LIBRARIAN, 1));
        assertTrue(CampusAnnouncementsPanel.opensOnRowClick(Role.LIBRARIAN, 2));
    }

    @Test public void showsCompleteAnnouncementInformation() {
        CampusAnnouncementDto announcement = new CampusAnnouncementDto(
                12L, "LIBRARY", "暑期开放通知", "暑期图书馆开放时间调整为 09:00—20:00。",
                "ALL", null, "PUBLISHED",
                LocalDateTime.of(2028, 7, 1, 9, 0),
                LocalDateTime.of(2028, 8, 31, 20, 0), 5L);
        CampusAnnouncementDetailPanel panel = new CampusAnnouncementDetailPanel(announcement);

        List<JLabel> labels = new ArrayList<JLabel>();
        collect(panel, JLabel.class, labels);
        StringBuilder visibleText = new StringBuilder();
        for (JLabel label : labels) visibleText.append(label.getText()).append('\n');
        assertTrue(visibleText.toString().contains("暑期开放通知"));
        assertTrue(visibleText.toString().contains("2028-07-01 09:00"));
        assertTrue(visibleText.toString().contains("2028-08-31 20:00"));
        assertTrue(visibleText.toString().contains("全部用户"));

        List<JTextArea> areas = new ArrayList<JTextArea>();
        collect(panel, JTextArea.class, areas);
        assertEquals("暑期图书馆开放时间调整为 09:00—20:00。", areas.get(0).getText());
    }

    private static <T> void collect(Component root, Class<T> type, List<T> result) {
        if (type.isInstance(root)) result.add(type.cast(root));
        if (!(root instanceof Container)) return;
        for (Component child : ((Container) root).getComponents()) collect(child, type, result);
    }
}
