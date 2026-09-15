package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Locale;

/** 学生、教师或管理员点击公告后展示的完整公告详情。 */
final class CampusAnnouncementDetailPanel extends JPanel {
    CampusAnnouncementDetailPanel(CampusAnnouncementDto value) {
        super(new BorderLayout(0, DesignTokens.SPACE_16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel title = UiFactory.title(RealUi.text(value.getTitle()));
        title.setForeground(DesignTokens.PRIMARY);
        JLabel hint = UiFactory.muted("公告编号：" + value.getId());
        heading.add(title, BorderLayout.NORTH);
        heading.add(hint, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, DesignTokens.SPACE_16));
        center.setOpaque(false);
        JPanel metadata = new JPanel(new GridLayout(0, 2, 14, 10));
        metadata.setOpaque(false);
        metadata.add(item("公告状态", RealUi.status(value.getStatus())));
        metadata.add(item("可见范围", scope(value)));
        metadata.add(item("生效时间", dateTime(value.getPublishAt())));
        metadata.add(item("失效时间", dateTime(value.getExpireAt())));
        center.add(metadata, BorderLayout.NORTH);

        JTextArea content = UiFactory.textArea(13, 56);
        content.setEditable(false);
        content.setText(RealUi.text(value.getContent()));
        content.setCaretPosition(0);
        content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        center.add(UiFactory.labelledField("公告正文", scroll), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private static JPanel item(String label, String value) {
        JLabel text = UiFactory.body(value);
        text.setOpaque(true);
        text.setBackground(DesignTokens.PRIMARY_LIGHT);
        text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.PRIMARY_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return UiFactory.labelledField(label, text);
    }

    private static String dateTime(org.threeten.bp.LocalDateTime value) {
        return value == null ? "未设置" : RealUi.dateTime(value);
    }

    private static String scope(CampusAnnouncementDto value) {
        if (!"ROLE".equalsIgnoreCase(value.getVisibleScope())) return "全部用户";
        String roleCode = value.getTargetRoleCode();
        if (roleCode == null || roleCode.trim().isEmpty()) return "指定角色";
        try {
            return "指定角色（" + Role.valueOf(roleCode.trim().toUpperCase(Locale.ROOT)).getDisplayName() + "）";
        } catch (IllegalArgumentException ex) {
            return "指定角色";
        }
    }
}
