package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.net.URI;

/** 线上资源检索、访问和图书管理员启停维护。 */
public final class LibraryResourcesPanel extends JPanel {
    private final BasePage page;
    private final LibraryClientService service;
    private final Role role;
    private final AsyncPagedTable<OnlineResourceView> resources;
    private final ResourceEditorPanel editor;
    private final JLabel detail = UiFactory.muted("选择资源查看详情。");
    private final JTextField copyUrl = UiFactory.textField(42);
    private final JButton accessButton = new PrimaryButton("访问选中资源");

    public LibraryResourcesPanel(BasePage page, LibraryClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        copyUrl.setEditable(false); copyUrl.setVisible(false);
        resources = table(); add(resources); add(info());
        if (isLibrarian()) {
            editor = new ResourceEditorPanel(new ResourceEditorPanel.Listener() {
                @Override public void onSave(OnlineResourceUpsertRequest request) { save(request); }
            }); add(editor);
            add(new LibraryResourceAccessLogsPanel(page, service));
        } else editor = null;
    }

    private AsyncPagedTable<OnlineResourceView> table() {
        AsyncPagedTable<OnlineResourceView> table = new AsyncPagedTable<OnlineResourceView>(
                "线上资源", "普通用户只查询已启用资源，管理员可查看全部状态。",
                "搜索资源名称或类型", statusFilters(),
                new String[]{"名称", "类型", "地址", "状态", "发布时间"},
                new AsyncPagedTable.Loader<OnlineResourceView>() {
                    @Override public PageSlice<OnlineResourceView> load(int p, String keyword, String filter) throws Exception { return LibraryResourcesPanel.this.load(p, keyword, filter); }
                }, new AsyncPagedTable.RowMapper<OnlineResourceView>() {
                    @Override public Object[] values(OnlineResourceView row) { return new Object[]{row.getTitle(), row.getResourceType(), row.getUrl(),
                            RealUi.status(row.getStatus()), RealUi.dateTime(row.getPublishedAt())}; }
                }, new AsyncPagedTable.SelectionListener<OnlineResourceView>() {
                    @Override public void onSelected(OnlineResourceView row) { select(row); }
                });
        accessButton.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { accessSelected(); }
        }); table.addAction(accessButton);
        if (isLibrarian()) {
            JButton create = new PrimaryButton("新建资源");
            create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); table.addAction(create);
        }
        return table;
    }

    private PageSlice<OnlineResourceView> load(int pageNumber, String keyword, String filter)
            throws Exception {
        PageResult<OnlineResourceView> result = service.searchResources(new OnlineResourceSearchRequest(
                keyword, null, resourceStatus(filter), pageNumber, 20));
        return new PageSlice<OnlineResourceView>(result.getItems(), result.getTotal(),
                result.getPage(), result.getPageSize());
    }

    private JPanel info() {
        JPanel info = new JPanel(new BorderLayout(0, 6)); info.setOpaque(false);
        JPanel content = UiFactory.vertical(6); content.add(detail); content.add(copyUrl);
        info.add(content, BorderLayout.CENTER); return info;
    }

    private void select(OnlineResourceView value) {
        copyUrl.setVisible(false); copyUrl.setText("");
        if (value == null) {
            detail.setText("选择资源查看详情。"); if (editor != null) editor.startNew(); return;
        }
        detail.setText("资源详情：" + RealUi.text(value.getTitle()) + "　类型："
                + RealUi.text(value.getResourceType()) + "　地址：" + RealUi.text(value.getUrl())
                + "　说明：" + RealUi.text(value.getDescription()));
        if (editor != null) editor.showResource(value);
    }

    private void accessSelected() {
        final OnlineResourceView value = resources.selectedItem();
        if (value == null) { page.showWarning("请先选择要访问的资源。"); return; }
        accessButton.setEnabled(false);
        AsyncTask.run(new AsyncTask.Work<OnlineResourceView>() {
                    @Override public OnlineResourceView run() throws Exception { return service.accessResource(value.getId()); }
                },
                new AsyncTask.Callback<OnlineResourceView>() {
                    @Override public void onSuccess(OnlineResourceView result) {
                        accessButton.setEnabled(true); open(result == null ? value : result);
                    }
                    @Override public void onFailure(Throwable error) {
                        accessButton.setEnabled(true); page.showError(AsyncTask.message(error));
                    }
                });
    }

    private void open(OnlineResourceView value) {
        String url = value == null ? null : value.getUrl();
        if (!isSafeWebUrl(url)) {
            copyUrl.setVisible(false); page.showError("资源地址仅支持 http/https，未自动打开。"); return;
        }
        try {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                fallback(url); return;
            }
            Desktop.getDesktop().browse(new URI(url.trim()));
            page.showSuccess("访问已记录，可在浏览器中查看资源。");
        } catch (Exception ex) { fallback(url); }
    }

    private void fallback(String url) {
        copyUrl.setText(url); copyUrl.setCaretPosition(0); copyUrl.setVisible(true);
        copyUrl.getParent().revalidate(); copyUrl.getParent().repaint();
        page.showWarning("当前环境不支持自动打开，请复制下方地址访问。");
    }

    static boolean isSafeWebUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = new URI(value.trim()); String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme));
        } catch (Exception ex) { return false; }
    }

    private void save(final OnlineResourceUpsertRequest request) {
        AsyncTask.run(new AsyncTask.Work<OnlineResourceView>() {
            @Override public OnlineResourceView run() throws Exception { return service.saveResource(request); }
        }, new AsyncTask.Callback<OnlineResourceView>() {
            @Override public void onSuccess(OnlineResourceView value) {
                page.showSuccess("线上资源已保存。"); resources.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private boolean isLibrarian() { return role == Role.LIBRARIAN; }

    private String[] statusFilters() {
        return isLibrarian() ? new String[]{"全部状态", "已启用", "已停用"} : new String[]{"已启用"};
    }

    private static String resourceStatus(String filter) {
        if ("已启用".equals(filter)) return "ACTIVE";
        return "已停用".equals(filter) ? "INACTIVE" : null;
    }
}
