package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 宿舍公告查询和宿管员行内发布维护。 */
public final class DormAnnouncementsPanel extends JPanel {
    private final BasePage page; private final DormClientService service; private final Role role;
    private final AsyncPagedTable<DormAnnouncementDto> announcements;
    private final JTextField title = UiFactory.textField(14); private final JTextArea content = UiFactory.textArea(3, 28);
    private final JComboBox<RealUi.CodeOption> scope = new JComboBox<RealUi.CodeOption>(RealUi.options("ALL", "ROLE")); private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(RealUi.options("DRAFT", "PUBLISHED", "ARCHIVED"));
    private final JLabel error = UiFactory.muted(" "); private long announcementId;

    public DormAnnouncementsPanel(BasePage page, DormClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; this.role = role;
        announcements = table(); add(announcements); if (role == Role.DORM_MANAGER) add(editor());
    }

    public void reload() { announcements.reload(); }

    private AsyncPagedTable<DormAnnouncementDto> table() {
        AsyncPagedTable<DormAnnouncementDto> table = new AsyncPagedTable<DormAnnouncementDto>("宿舍公告", role == Role.DORM_MANAGER ? "发布和维护宿舍公告。" : "查看有效宿舍公告。", "搜索公告标题或内容",
                new String[]{"全部状态", "已发布", "草稿", "已归档"}, new String[]{"标题", "可见范围", "内容摘要", "发布时间", "状态"},
                new AsyncPagedTable.Loader<DormAnnouncementDto>() {
                    @Override public PageSlice<DormAnnouncementDto> load(int p, String keyword, String filter) throws Exception { return slice(service.announcements(new DormPageQuery(p, 20, keyword, announceStatus(filter), null, null))); }
                }, new AsyncPagedTable.RowMapper<DormAnnouncementDto>() {
                    @Override public Object[] values(DormAnnouncementDto row) { return new Object[]{row.getTitle(), RealUi.status(row.getVisibleScope()), RealUi.text(row.getContent()), RealUi.dateTime(row.getPublishAt()), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<DormAnnouncementDto>() {
                    @Override public void onSelected(DormAnnouncementDto row) { select(row); }
                });
        if (role == Role.DORM_MANAGER) { JButton create = new PrimaryButton("新建公告"); create.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clear(); }
        }); table.addAction(create); }
        return table;
    }

    private JPanel editor() {
        SectionCard card = new SectionCard("公告详情与发布", "填写公告内容和可见范围。");
        scope.setFont(DesignTokens.regular(13)); status.setFont(DesignTokens.regular(13)); JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("标题", title)); fields.add(UiFactory.labelledField("可见范围", scope)); fields.add(UiFactory.labelledField("状态", status)); JPanel blank = new JPanel(); blank.setOpaque(false); fields.add(blank);
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10)); contentPanel.setOpaque(false); contentPanel.add(fields, BorderLayout.NORTH); contentPanel.add(UiFactory.labelledField("正文", content), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clear(); }
        }); JButton save = new PrimaryButton("保存公告"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(clear); actions.add(save); actions.add(error); contentPanel.add(actions, BorderLayout.SOUTH); card.setContent(contentPanel);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); clear(); return wrap;
    }

    private void select(DormAnnouncementDto value) {
        if (value == null || role != Role.DORM_MANAGER) return;
        announcementId = value.getId(); title.setText(RealUi.input(value.getTitle())); scope.setSelectedItem(RealUi.option(value.getVisibleScope())); content.setText(RealUi.input(value.getContent())); status.setSelectedItem(RealUi.option(value.getStatus())); error.setText(" ");
    }

    private void clear() { announcementId = 0L; title.setText(""); scope.setSelectedItem(RealUi.option("ALL")); content.setText(""); status.setSelectedItem(RealUi.option("DRAFT")); error.setText(" "); }
    private void save() {
        try {
            final AnnouncementSaveRequest request = new AnnouncementSaveRequest(announcementId, RealUi.required(title.getText(), "标题"), RealUi.required(content.getText(), "正文"), RealUi.code(scope.getSelectedItem()), null, RealUi.code(status.getSelectedItem()), null, null);
            AsyncTask.run(new AsyncTask.Work<DormAnnouncementDto>() {
                @Override public DormAnnouncementDto run() throws Exception { return service.saveAnnouncement(request); }
            }, new AsyncTask.Callback<DormAnnouncementDto>() {
                @Override public void onSuccess(DormAnnouncementDto value) { page.showSuccess("公告已保存。"); announcements.reload(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static PageSlice<DormAnnouncementDto> slice(DormPage<DormAnnouncementDto> value) { return RealUi.page(value); }
    private static String announceStatus(String filter) { if ("已发布".equals(filter)) return "PUBLISHED"; if ("草稿".equals(filter)) return "DRAFT"; if ("已归档".equals(filter)) return "ARCHIVED"; return null; }
}
