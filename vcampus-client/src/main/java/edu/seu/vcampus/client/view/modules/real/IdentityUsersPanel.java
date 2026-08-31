package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.PasswordResetRequest;
import edu.seu.vcampus.common.dto.identity.RoleAssignmentRequest;
import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.util.List;

/** 系统管理员用户搜索、启停、密码重置和角色分配页面。 */
public final class IdentityUsersPanel extends JPanel {
    private final BasePage page; private final IdentityClientService service;
    private final AsyncPagedTable<ProfileDto> table; private final JLabel detail = UiFactory.muted("选择用户查看详情。");
    private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(RealUi.options("ACTIVE", "DISABLED"));
    private final JComboBox<RealUi.CodeOption> role = new JComboBox<RealUi.CodeOption>();
    private final JPasswordField resetPassword = password(); private long selectedId;

    public IdentityUsersPanel(BasePage page, IdentityClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; table = createTable(); add(table); add(actionCard()); loadRoles();
    }

    private AsyncPagedTable<ProfileDto> createTable() {
        return new AsyncPagedTable<ProfileDto>("用户与角色", "维护账号状态和人员角色。", "搜索账号或姓名",
                new String[]{"全部状态", "已启用", "已停用"}, new String[]{"账号", "姓名", "角色", "状态", "最近登录"},
                new AsyncPagedTable.Loader<ProfileDto>() {
                    @Override public PageSlice<ProfileDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.searchUsers(new UserQuery(keyword, userStatus(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<ProfileDto>() {
                    @Override public Object[] values(ProfileDto row) { return new Object[]{row.getAccount(), row.getDisplayName(), roles(row), RealUi.status(row.getStatus()), RealUi.dateTime(row.getLastLoginAt())}; }
                }, new AsyncPagedTable.SelectionListener<ProfileDto>() {
                    @Override public void onSelected(ProfileDto row) { select(row); }
                });
    }

    private SectionCard actionCard() {
        SectionCard card = new SectionCard("账号操作", "选择用户后执行操作；重要操作需确认。");
        JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("账号状态", status)); fields.add(UiFactory.labelledField("分配/撤销角色", role));
        fields.add(UiFactory.labelledField("重置密码", resetPassword)); fields.add(detail);
        JPanel actions = UiFactory.horizontal(8); JButton update = new PrimaryButton("保存启停状态"); update.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateStatus(); }
        });
        JButton reset = new DangerButton("重置密码"); reset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reset(); }
        }); JButton assign = new PrimaryButton("分配角色"); assign.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { assign(); }
        });
        JButton revoke = new DangerButton("撤销角色"); revoke.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { revoke(); }
        }); actions.add(update); actions.add(reset); actions.add(assign); actions.add(revoke);
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false); body.add(fields, BorderLayout.CENTER); body.add(actions, BorderLayout.SOUTH); card.setContent(body); return card;
    }

    private void select(ProfileDto value) {
        if (value == null) { selectedId = 0L; detail.setText("选择用户查看详情。"); return; }
        selectedId = value.getUserId(); status.setSelectedItem(RealUi.option(value.getStatus()));
        detail.setText("已选择：" + RealUi.text(value.getAccount()) + "　" + RealUi.text(value.getDisplayName()) + "　角色：" + roles(value));
    }

    private void loadRoles() {
        AsyncTask.run(new AsyncTask.Work<List<RoleDto>>() {
            @Override public List<RoleDto> run() throws Exception { return service.listRoles(); }
        }, new AsyncTask.Callback<List<RoleDto>>() {
            @Override public void onSuccess(List<RoleDto> values) { role.removeAllItems(); for (RoleDto value : values) role.addItem(RealUi.option(value.getCode())); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void updateStatus() {
        if (!selected()) return; final String value = RealUi.code(status.getSelectedItem());
        final UserStatusUpdateRequest request = new UserStatusUpdateRequest(selectedId, value);
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.updateUserStatus(request); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) { page.showSuccess("账号状态已更新。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void reset() {
        if (!selected()) return; final String next = new String(resetPassword.getPassword());
        if (next.trim().isEmpty()) { page.showWarning("请输入新的临时密码。"); return; }
        if (!RealUi.confirm(this, "确认重置该用户密码？")) return;
        final PasswordResetRequest request = new PasswordResetRequest(selectedId, next);
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.resetPassword(request); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) { resetPassword.setText(""); page.showSuccess("密码已重置。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void assign() {
        final Role selected = selectedRole(); if (!selected()) return; if (selected == null) { page.showWarning("角色列表尚未加载。"); return; }
        final RoleAssignmentRequest request = new RoleAssignmentRequest(selectedId, selected);
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.assignRole(request); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) { page.showSuccess("角色已分配。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void revoke() {
        final Role selected = selectedRole(); if (!selected()) return; if (selected == null) { page.showWarning("角色列表尚未加载。"); return; }
        if (!RealUi.confirm(this, "确认撤销该用户的“" + selected.getDisplayName() + "”角色？")) return;
        final RoleRevokeRequest request = new RoleRevokeRequest(selectedId, selected);
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.revokeRole(request); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) { page.showSuccess("角色已撤销。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private boolean selected() { if (selectedId > 0L) return true; page.showWarning("请先选择用户。"); return false; }
    private Role selectedRole() { try { return Role.valueOf(RealUi.code(role.getSelectedItem())); } catch (Exception ex) { return null; } }
    private static PageSlice<ProfileDto> slice(UserPage value) { return RealUi.page(value); }
    private static String userStatus(String filter) { return "已启用".equals(filter) ? "ACTIVE" : "已停用".equals(filter) ? "DISABLED" : null; }
    private static String roles(ProfileDto value) { StringBuilder text = new StringBuilder(); for (Role item : value.getRoles()) { if (text.length() > 0) text.append('、'); text.append(item.getDisplayName()); } return text.length() == 0 ? "--" : text.toString(); }
    private static JPasswordField password() { JPasswordField value = new JPasswordField(14); UiFactory.styleField(value); value.setFont(DesignTokens.regular(14)); return value; }
}
