package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.PdfClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.security.Role;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** PDF 公共资源、上传记录、管理员审核与下载中心。 */
public final class PdfResourcesPanel extends JPanel {
    private final BasePage page; private final PdfClientService service; private final Role role;
    private final JTabbedPane tabs = new edu.seu.vcampus.client.ui.FitContentTabs();
    private final PdfDownloadsPanel downloads;
    private final List<AsyncPagedTable<PdfResourceView>> lists = new ArrayList<AsyncPagedTable<PdfResourceView>>();
    private final Timer polling;
    public PdfResourcesPanel(BasePage page, PdfClientService service, Role role) {
        super(new BorderLayout()); setOpaque(false); this.page = page; this.service = service; this.role = role;
        downloads = new PdfDownloadsPanel(page, service);
        if (role == Role.LIBRARIAN) {
            tabs.addTab("待审核", table("REVIEW")); tabs.addTab("全部 PDF", table("ADMIN"));
        } else tabs.addTab("资源列表", table("PUBLIC"));
        if (role == Role.STUDENT || role == Role.LIBRARIAN) {
            tabs.addTab("上传资源", new PdfUploadPanel(page, service)); tabs.addTab("我的上传", table("MINE"));
        }
        tabs.addTab("下载进度与记录", downloads); add(tabs, BorderLayout.CENTER);
        page.addPropertyChangeListener("library.pdf.version", event -> refresh());
        tabs.addChangeListener(event -> refreshSelected());
        polling = new Timer(15000, event -> refreshSelected());
    }
    @Override public void addNotify() { super.addNotify(); if (polling != null) polling.start(); }
    @Override public void removeNotify() { if (polling != null) polling.stop(); super.removeNotify(); }
    private void refreshSelected() {
        if (!isShowing()) return;
        if (tabs.getSelectedComponent() instanceof AsyncPagedTable) {
            AsyncPagedTable<?> table = (AsyncPagedTable<?>) tabs.getSelectedComponent();
            table.refreshCurrentPage();
        }
    }
    private void refresh() { for (AsyncPagedTable<PdfResourceView> table : lists) table.refreshCurrentPage(); }
    private AsyncPagedTable<PdfResourceView> table(String scope) {
        boolean review = "REVIEW".equals(scope), admin = "ADMIN".equals(scope), mine = "MINE".equals(scope);
        String title = review ? "待审核 PDF" : admin ? "PDF 资源管理" : mine ? "我的上传记录" : "可下载 PDF 资源";
        AsyncPagedTable<PdfResourceView> table = new AsyncPagedTable<PdfResourceView>(title,
                mine ? "查看审核状态；被拒绝时可在详情中查看完整理由。" : "勾选多个文件后点击批量下载；点击列表行可查看详情。",
                "搜索资源名称、简介或文件名", review ? new String[]{"待审核"}
                        : mine || admin ? new String[]{"全部状态", "待审核", "已通过", "已拒绝", "已停用"} : new String[]{"已通过"},
                new String[]{"选择", "资源名称", "文件名", "大小", "上传者", "提交时间", "审核状态", "拒绝理由"},
                (p, keyword, filter) -> { PageResult<PdfResourceView> result = service.list(new PdfQuery(scope, keyword, PdfUi.filter(filter), p, 20));
                    return new PageSlice<PdfResourceView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize()); },
                r -> new Object[]{Boolean.FALSE, r.getTitle(), r.getFileName(), PdfUi.size(r.getFileSize()), r.getUploaderName(),
                        PdfUi.date(r.getUploadedAt()), PdfUi.state(r.getStatus()), RealUi.text(r.getRejectionReason())}, null);
        table.enableCheckboxSelection(); table.setItemKey(r -> r.getId());
        JButton detail = new SecondaryButton("查看详情"); detail.addActionListener(event -> detail(table.selectedItem())); table.addAction(detail);
        JButton download = new PrimaryButton(review ? "下载并查看文件" : "下载选中 / 勾选文件");
        download.addActionListener(event -> download(table, review)); table.addAction(download);
        if (review || admin) {
            JButton approve = new PrimaryButton("审核通过"); approve.addActionListener(event -> review(table.selectedItem(), "APPROVED")); table.addAction(approve);
            JButton reject = new DangerButton("拒绝并填写理由"); reject.addActionListener(event -> review(table.selectedItem(), "REJECTED")); table.addAction(reject);
            if (admin) { JButton disable = new DangerButton("停用资源"); disable.addActionListener(event -> review(table.selectedItem(), "INACTIVE")); table.addAction(disable); }
        }
        lists.add(table); return table;
    }
    private void detail(PdfResourceView row) {
        if (row == null) { page.showWarning("请先选择资源。"); return; }
        AsyncTask.run(() -> service.detail(row.getId()), new AsyncTask.Callback<PdfResourceView>() {
            @Override public void onSuccess(PdfResourceView r) {
                JTextArea text = UiFactory.textArea(16, 38); text.setEditable(false);
                text.setText("资源名称：" + r.getTitle() + "\n\n资源简介：" + RealUi.text(r.getDescription())
                        + "\n\n文件名：" + r.getFileName() + "\n文件大小：" + PdfUi.size(r.getFileSize())
                        + "\n上传者：" + r.getUploaderName() + "\n提交时间：" + PdfUi.date(r.getUploadedAt())
                        + "\n审核状态：" + PdfUi.state(r.getStatus()) + "\n审核人：" + RealUi.text(r.getReviewerName())
                        + "\n审核时间：" + PdfUi.date(r.getReviewedAt()) + "\n拒绝理由：" + RealUi.text(r.getRejectionReason()));
                text.setCaretPosition(0);
                int choice = JOptionPane.showOptionDialog(PdfResourcesPanel.this, new JScrollPane(text), "PDF 资源详情",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"下载文件", "关闭"}, "关闭");
                if (choice == 0) {
                    Path directory = PdfDownloadsPanel.chooseDirectory(PdfResourcesPanel.this);
                    if (directory != null) { downloads.enqueue(r.getId(), r.getFileName(), directory, role == Role.LIBRARIAN); tabs.setSelectedComponent(downloads); }
                }
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
    private void download(AsyncPagedTable<PdfResourceView> table, boolean open) {
        List<PdfResourceView> selected = table.checkedItems();
        if (selected.isEmpty() && table.selectedItem() != null) selected.add(table.selectedItem());
        if (selected.isEmpty()) { page.showWarning("请勾选或选择要下载的 PDF。"); return; }
        Path directory = PdfDownloadsPanel.chooseDirectory(this); if (directory == null) return;
        for (PdfResourceView row : selected) downloads.enqueue(row.getId(), row.getFileName(), directory, open);
        tabs.setSelectedComponent(downloads);
    }
    private void review(PdfResourceView row, String decision) {
        if (row == null) { page.showWarning("请先选择资源。"); return; }
        String reason = null;
        if ("REJECTED".equals(decision)) {
            JTextArea input = UiFactory.textArea(4, 32);
            if (JOptionPane.showConfirmDialog(this, new Object[]{"请输入拒绝理由（必填，最多 1000 字）：", new JScrollPane(input)},
                    "拒绝资源", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            reason = input.getText().trim(); if (reason.isEmpty() || reason.length() > 1000) { page.showWarning("拒绝理由应为 1 至 1000 字。"); return; }
        } else if (!RealUi.confirm(this, "确认将“" + row.getTitle() + "”设为“" + PdfUi.state(decision) + "”？")) return;
        final PdfReviewRequest request = new PdfReviewRequest(row.getId(), decision, reason);
        AsyncTask.run(() -> service.review(request), new AsyncTask.Callback<PdfResourceView>() {
            @Override public void onSuccess(PdfResourceView value) { page.showSuccess("审核结果已保存。"); page.putClientProperty("library.pdf.version", System.nanoTime()); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
}
