package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;

import javax.swing.JButton;
import javax.swing.JPanel;

/** 系统管理员在线会话查询与强制下线。 */
public final class IdentitySessionsPanel extends JPanel {
    private final BasePage page; private final IdentityClientService service;
    private final AsyncPagedTable<SessionDto> table; private long selectedId;

    public IdentitySessionsPanel(BasePage page, IdentityClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; table = createTable(); add(table);
    }

    private AsyncPagedTable<SessionDto> createTable() {
        AsyncPagedTable<SessionDto> value = new AsyncPagedTable<SessionDto>("有效登录会话", "登录凭据不显示；管理员可让指定设备退出登录。", "搜索用户编号",
                new String[]{"有效会话", "包含已撤销"}, new String[]{"账号", "用户编号", "身份", "最后活动", "状态"},
                new AsyncPagedTable.Loader<SessionDto>() {
                    @Override public PageSlice<SessionDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.searchSessions(new SessionQuery(userId(keyword), "包含已撤销".equals(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<SessionDto>() {
                    @Override public Object[] values(SessionDto row) { return new Object[]{row.getAccount(), row.getUserId(), row.getActiveRole() == null ? "--" : row.getActiveRole().getDisplayName(),
                            RealUi.dateTime(row.getLastSeenAt()), row.isRevoked() ? "已撤销" : "有效"}; }
                }, new AsyncPagedTable.SelectionListener<SessionDto>() {
                    @Override public void onSelected(SessionDto row) { select(row); }
                });
        JButton revoke = new DangerButton("强制下线"); revoke.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { revoke(); }
        }); value.addAction(revoke); return value;
    }

    private void select(SessionDto value) { selectedId = value == null ? 0L : value.getSessionId(); }

    private void revoke() {
        if (selectedId <= 0L) { page.showWarning("请先选择在线会话。"); return; }
        if (!RealUi.confirm(this, "确认强制该会话下线？")) return;
        final edu.seu.vcampus.common.dto.identity.SessionRevokeRequest request = new edu.seu.vcampus.common.dto.identity.SessionRevokeRequest(selectedId);
        AsyncTask.run(new AsyncTask.Work<Boolean>() {
            @Override public Boolean run() throws Exception { return service.revokeSession(request); }
        }, new AsyncTask.Callback<Boolean>() {
            @Override public void onSuccess(Boolean value) { page.showSuccess(Boolean.TRUE.equals(value) ? "会话已强制下线。" : "会话已不可用。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static PageSlice<SessionDto> slice(SessionPage value) { return RealUi.page(value); }
    private static Long userId(String value) { return value != null && value.trim().matches("\\d+") ? Long.valueOf(value.trim()) : null; }
}
