package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.Locale;

/** 公告的行内编辑器，创建、发布和撤回均通过当前页面完成。 */
public final class CampusAnnouncementEditorPanel extends SectionCard {
    public interface Listener { void onSave(CampusAnnouncementSaveRequest request); }
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JTextField title = field();
    private final JComboBox<ScopeOption> scope = new JComboBox<ScopeOption>(ScopeOption.values());
    private final JComboBox<RoleOption> targetRole = new JComboBox<RoleOption>(roleOptions());
    private final JTextField publishAt = field();
    private final JTextField expireAt = field();
    private final JTextArea content = UiFactory.textArea(4, 28);
    private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(
            RealUi.options("DRAFT", "PUBLISHED", "SCHEDULED"));
    private final JLabel error = UiFactory.muted(" ");
    private final Listener listener;
    private final String moduleCode;
    private long id;

    public CampusAnnouncementEditorPanel(Listener listener) {
        this("ACADEMIC", "教务公告编辑", listener);
    }

    public CampusAnnouncementEditorPanel(String moduleCode, String editorTitle, Listener listener) {
        super(editorTitle, "填写公告内容、可见范围和发布时间。");
        this.moduleCode = moduleCode == null ? "" : moduleCode.trim().toUpperCase(Locale.ROOT);
        this.listener = listener; status.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "标题", title); add(fields, "可见范围", scope);
        add(fields, "目标角色（可选）", targetRole); add(fields, "状态", status);
        add(fields, "生效时间", publishAt); add(fields, "失效时间", expireAt);
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false);
        body.add(fields, BorderLayout.NORTH); body.add(UiFactory.labelledField("正文", content), BorderLayout.CENTER);
        scope.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateTargetRoleEnabled(); }
        });
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建");
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        }); JButton save = new PrimaryButton("保存公告");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(clear); actions.add(save); actions.add(error);
        body.add(actions, BorderLayout.SOUTH); setContent(body); startNew();
    }

    public void startNew() {
        id = 0L; title.setText(""); scope.setSelectedItem(ScopeOption.ALL);
        targetRole.setSelectedItem(RoleOption.none());
        publishAt.setText(""); expireAt.setText(""); content.setText("");
        status.setSelectedItem(RealUi.option("DRAFT")); error.setText(" ");
    }

    public void showAnnouncement(CampusAnnouncementDto value) {
        if (value == null) { startNew(); return; }
        id = value.getId(); title.setText(RealUi.input(value.getTitle()));
        scope.setSelectedItem(ScopeOption.fromCode(value.getVisibleScope()));
        targetRole.setSelectedItem(RoleOption.fromCode(value.getTargetRoleCode())); updateTargetRoleEnabled();
        publishAt.setText(value.getPublishAt() == null ? "" : FORMAT.format(value.getPublishAt()));
        expireAt.setText(value.getExpireAt() == null ? "" : FORMAT.format(value.getExpireAt()));
        content.setText(RealUi.input(value.getContent())); status.setSelectedItem(RealUi.option(value.getStatus())); error.setText(" ");
    }

    private void save() {
        try {
            LocalDateTime from = parse(publishAt.getText(), "生效时间");
            LocalDateTime to = parse(expireAt.getText(), "失效时间");
            if (from != null && to != null && !to.isAfter(from)) throw new IllegalArgumentException("失效时间必须晚于生效时间");
            ScopeOption selectedScope = (ScopeOption) scope.getSelectedItem();
            RoleOption selectedRole = (RoleOption) targetRole.getSelectedItem();
            String scopeCode = selectedScope == null ? ScopeOption.ALL.code : selectedScope.code;
            String targetCode = "ROLE".equals(scopeCode) && selectedRole != null ? selectedRole.code() : null;
            CampusAnnouncementSaveRequest request = new CampusAnnouncementSaveRequest(
                    id == 0L ? null : Long.valueOf(id), moduleCode,
                    RealUi.required(title.getText(), "标题"), RealUi.required(content.getText(), "正文"),
                    scopeCode, null, targetCode,
                    RealUi.code(status.getSelectedItem()), from, to);
            if (listener != null) listener.onSave(request); error.setText(" ");
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static LocalDateTime parse(String value, String label) {
        String text = RealUi.optional(value); if (text == null) return null;
        try { return LocalDateTime.parse(text, FORMAT); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm"); }
    }

    private static JTextField field() { return UiFactory.textField(12); }
    private static void add(JPanel panel, String label, java.awt.Component field) { panel.add(UiFactory.labelledField(label, field)); }

    private void updateTargetRoleEnabled() {
        ScopeOption selected = (ScopeOption) scope.getSelectedItem();
        boolean enabled = selected == ScopeOption.ROLE;
        targetRole.setEnabled(enabled);
        if (!enabled) targetRole.setSelectedItem(RoleOption.none());
    }

    private static RoleOption[] roleOptions() {
        Role[] roles = Role.values(); RoleOption[] result = new RoleOption[roles.length + 1];
        result[0] = RoleOption.none();
        for (int i = 0; i < roles.length; i++) result[i + 1] = RoleOption.of(roles[i]);
        return result;
    }

    private enum ScopeOption {
        ALL("ALL", "全部用户"), ROLE("ROLE", "指定角色");
        private final String code; private final String label;
        ScopeOption(String code, String label) { this.code = code; this.label = label; }
        private static ScopeOption fromCode(String value) {
            return "ROLE".equalsIgnoreCase(value) ? ROLE : ALL;
        }
        @Override public String toString() { return label; }
    }

    private static final class RoleOption {
        private final Role role;
        private RoleOption(Role role) { this.role = role; }
        private static RoleOption none() { return new RoleOption(null); }
        private static RoleOption of(Role value) { return new RoleOption(value); }
        private static RoleOption fromCode(String value) {
            if (value == null || value.trim().isEmpty()) return none();
            try { return of(Role.valueOf(value.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ex) { return none(); }
        }
        private String code() { return role == null ? null : role.name(); }
        @Override public String toString() { return role == null ? "无" : role.getDisplayName(); }
        @Override public boolean equals(Object other) {
            return other instanceof RoleOption && ((RoleOption) other).role == role;
        }
        @Override public int hashCode() { return role == null ? 0 : role.hashCode(); }
    }
}
