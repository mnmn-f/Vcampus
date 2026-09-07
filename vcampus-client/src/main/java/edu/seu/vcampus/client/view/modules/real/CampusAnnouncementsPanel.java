package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** 通用模块公告查询；指定职责可在当前页面发布、编辑和撤回。 */
public final class CampusAnnouncementsPanel extends JPanel {
    private final BasePage page;
    private final CampusClientService service;
    private final Role role;
    private final String moduleCode;
    private final String displayTitle;
    private final Role manageRole;
    private final AsyncPagedTable<CampusAnnouncementDto> table;
    private final CampusAnnouncementEditorPanel editor;
    private final JLabel detail = UiFactory.muted("选择公告查看详情。");

    public CampusAnnouncementsPanel(BasePage page, CampusClientService service, Role role) {
        this(page, service, role, "ACADEMIC", "教务公告", Role.ACADEMIC_ADMIN);
    }

    public CampusAnnouncementsPanel(BasePage page, CampusClientService service, Role role,
                                    String moduleCode, String displayTitle, Role manageRole) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        this.moduleCode = moduleCode; this.displayTitle = displayTitle; this.manageRole = manageRole;
        editor = canManage() ? new CampusAnnouncementEditorPanel(moduleCode, displayTitle + "编辑", new CampusAnnouncementEditorPanel.Listener() {
            @Override public void onSave(CampusAnnouncementSaveRequest request) { save(request); }
        }) : null;
        table = createTable(); add(table); if (editor != null) add(editor);
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
    }

    private AsyncPagedTable<CampusAnnouncementDto> createTable() {
        AsyncPagedTable<CampusAnnouncementDto> value = new AsyncPagedTable<CampusAnnouncementDto>(
                displayTitle, canManage() ? "公告管理" : "查看已生效公告。", "搜索标题或正文",
                canManage() ? new String[]{"全部状态", "已发布", "草稿", "定时发布"} : null,
                columns(),
                new AsyncPagedTable.Loader<CampusAnnouncementDto>() {
                    @Override public PageSlice<CampusAnnouncementDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(service.announcements(new CampusAnnouncementQuery(
                                new CampusPageQuery(p, 20, keyword,
                                        canManage() ? status(filter) : "PUBLISHED"), moduleCode)));
                    }
                }, new AsyncPagedTable.RowMapper<CampusAnnouncementDto>() {
                    @Override public Object[] values(CampusAnnouncementDto row) { return row(row); }
                }, new AsyncPagedTable.SelectionListener<CampusAnnouncementDto>() {
                    @Override public void onSelected(CampusAnnouncementDto row) { select(row); }
                });
        JButton open = new SecondaryButton("打开公告");
        open.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openSelected(); }
        }); value.addAction(open);
        value.getTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });
        if (canManage()) {
            JButton create = new PrimaryButton("新建公告"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); value.addAction(create);
            JButton revoke = new DangerButton("撤回公告"); revoke.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { revoke(); }
            }); value.addAction(revoke);
        }
        return value;
    }

    private void select(CampusAnnouncementDto value) {
        if (value == null) { detail.setText("选择公告查看详情。"); if (editor != null) editor.startNew(); return; }
        detail.setText("已选择：" + RealUi.text(value.getTitle()) + "　" + summary(value.getContent()));
        if (editor != null) editor.showAnnouncement(value);
    }

    private void openSelected() {
        CampusAnnouncementDto value = table.selectedItem();
        if (value == null) { page.showWarning("请先选择公告。"); return; }
        JTextArea content = UiFactory.textArea(14, 56); content.setEditable(false);
        content.setText(RealUi.text(value.getContent())); content.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(content); scroll.setPreferredSize(new Dimension(640, 360));
        JOptionPane.showMessageDialog(this, scroll, RealUi.text(value.getTitle()),
                JOptionPane.PLAIN_MESSAGE);
    }

    private void save(CampusAnnouncementSaveRequest request) {
        final CampusAnnouncementSaveRequest finalRequest = request;
        AsyncTask.run(new AsyncTask.Work<CampusAnnouncementDto>() {
            @Override public CampusAnnouncementDto run() throws Exception { return service.saveAnnouncement(finalRequest); }
        }, new AsyncTask.Callback<CampusAnnouncementDto>() {
            @Override public void onSuccess(CampusAnnouncementDto value) { page.showSuccess("公告已保存。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void revoke() {
        final CampusAnnouncementDto value = table.selectedItem();
        if (value == null) { page.showWarning("请先选择要撤回的公告。"); return; }
        if (!RealUi.confirm(this, "确认撤回“" + RealUi.text(value.getTitle()) + "”？")) return;
        AsyncTask.run(new AsyncTask.Work<CampusAnnouncementDto>() {
            @Override public CampusAnnouncementDto run() throws Exception { return service.revokeAnnouncement(value.getId()); }
        }, new AsyncTask.Callback<CampusAnnouncementDto>() {
            @Override public void onSuccess(CampusAnnouncementDto result) { page.showSuccess("公告已撤回。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String status(String filter) {
        if ("已发布".equals(filter)) return "PUBLISHED"; if ("草稿".equals(filter)) return "DRAFT";
        return "定时发布".equals(filter) ? "SCHEDULED" : null;
    }
    private static String scope(String value) { return "ROLE".equalsIgnoreCase(value) ? "指定角色" : "全部用户"; }
    private String[] columns() { return canManage()
            ? new String[]{"标题", "可见范围", "正文摘要", "生效时间", "状态"}
            : new String[]{"标题", "正文摘要", "发布时间"}; }
    private Object[] row(CampusAnnouncementDto value) { return canManage()
            ? new Object[]{value.getTitle(), scope(value.getVisibleScope()), summary(value.getContent()),
                    RealUi.dateTime(value.getPublishAt()), RealUi.status(value.getStatus())}
            : new Object[]{value.getTitle(), summary(value.getContent()), RealUi.dateTime(value.getPublishAt())}; }
    private static String summary(String value) { if (value == null) return "--"; String text = value.replace('\n', ' ').trim(); return text.length() > 80 ? text.substring(0, 80) + "…" : text; }
    private boolean canManage() { return manageRole != null && manageRole == role; }
}
