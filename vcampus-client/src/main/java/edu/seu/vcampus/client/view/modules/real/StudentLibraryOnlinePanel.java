package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.*;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.*;
import edu.seu.vcampus.common.security.Role;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 学生线上资源工作区：单层页签、文件列表、上传、网站和下载记录。 */
final class StudentLibraryOnlinePanel extends JPanel {
    private final BasePage host; private final LibraryClientService library; private final PdfClientService pdf;
    private final JPanel cards = new JPanel(new CardLayout()), pdfList = LibraryUi.stack(8), mineList = LibraryUi.stack(8), webList = LibraryUi.stack(8);
    private final JTextField pdfKeyword = LibraryUi.search("搜索资源名称或关键词");
    private final Map<Long, JCheckBox> checks = new LinkedHashMap<>(); private final Map<Long, PdfResourceView> visible = new LinkedHashMap<>();
    private final JLabel pdfTotal = LibraryUi.muted(""), selectedCount = LibraryUi.muted("已选 0 项");
    private final PdfDownloadsPanel downloads; private int pdfPage = 1, minePage = 1; private long pdfSerial, mineSerial, webSerial;
    private final List<JButton> tabButtons = new ArrayList<>();

    StudentLibraryOnlinePanel(BasePage host, LibraryClientService library, ClientSession session) {
        super(new LibraryUi.StackLayout(16)); setOpaque(false); this.host = host; this.library = library; this.pdf = library.pdf(session);
        downloads = pdf == null ? null : new PdfDownloadsPanel(host, pdf);
        add(LibraryUi.between(LibraryUi.heading("线上资源"), LibraryUi.button("上传资源", true, () -> show("upload"))));
        JPanel tabs = LibraryUi.row(8); addTab(tabs, "PDF 资源", "pdf"); addTab(tabs, "网站资源", "web"); addTab(tabs, "我的上传", "mine"); addTab(tabs, "下载记录", "downloads"); add(tabs); cards.setOpaque(false); add(cards);
        cards.add(pdfWorkspace(), "pdf"); cards.add(webWorkspace(), "web");
        if (pdf != null) { PdfUploadPanel upload = new PdfUploadPanel(host, pdf); LibraryUi.styleLegacy(upload); cards.add(upload, "upload"); cards.add(mineWorkspace(), "mine"); LibraryUi.styleLegacy(downloads); cards.add(downloads, "downloads"); }
        else { cards.add(LibraryUi.state("当前服务未提供 PDF 功能", null), "upload"); cards.add(LibraryUi.state("当前服务未提供 PDF 功能", null), "mine"); cards.add(LibraryUi.state("当前服务未提供 PDF 功能", null), "downloads"); }
        host.addPropertyChangeListener("library.pdf.version", e -> { loadPdfs(); loadMine(); }); show("pdf"); loadPdfs(); loadWeb();
    }
    private void addTab(JPanel tabs, String text, String card) { JButton b = LibraryUi.button(text, false, () -> show(card)); b.putClientProperty("library.card", card); tabButtons.add(b); tabs.add(b); }
    private void show(String name) {
        if ("upload".equals(name)) name = "upload"; ((CardLayout) cards.getLayout()).show(cards, name);
        for (JButton b : tabButtons) { boolean active = name.equals(b.getClientProperty("library.card")) || ("upload".equals(name) && "mine".equals(b.getClientProperty("library.card"))); b.putClientProperty("library.active", active); b.repaint(); }
        if ("mine".equals(name)) loadMine(); revalidate(); repaint();
    }
    private JPanel pdfWorkspace() {
        JPanel page = LibraryUi.stack(16), filters = LibraryUi.card(); JButton search = LibraryUi.button("搜索", true, () -> { pdfPage = 1; loadPdfs(); }); pdfKeyword.addActionListener(e -> search.doClick()); filters.add(LibraryUi.between(pdfKeyword, search)); page.add(filters);
        JPanel card = LibraryUi.card(); card.add(LibraryUi.between(LibraryUi.label("可下载资源", 18, true), pdfTotal));
        JPanel toolbar = new LibraryUi.Surface(DesignTokens.PRIMARY_LIGHT, 10); toolbar.setLayout(new BorderLayout(10, 0)); toolbar.add(selectedCount, BorderLayout.CENTER); toolbar.add(LibraryUi.button("下载选中", true, this::downloadSelected), BorderLayout.EAST); card.add(toolbar); card.add(pdfList); page.add(card); return page;
    }
    private void loadPdfs() {
        if (pdf == null) { LibraryUi.replace(pdfList, LibraryUi.state("当前服务未提供 PDF 功能", null)); return; }
        final long request = ++pdfSerial; final int page = pdfPage; checks.clear(); visible.clear(); selectedCount.setText("已选 0 项"); LibraryUi.replace(pdfList, LibraryUi.state("正在加载 PDF 资源…", null));
        AsyncTask.run(() -> pdf.list(new PdfQuery("PUBLIC", pdfKeyword.getText().trim(), null, page, 8)), new AsyncTask.Callback<PageResult<PdfResourceView>>() {
            public void onSuccess(PageResult<PdfResourceView> data) { if (request != pdfSerial) return; pdfList.removeAll(); pdfTotal.setText("共 " + data.getTotal() + " 个文件"); pdfList.add(pdfHeader()); for (PdfResourceView row : data.getItems()) { visible.put(row.getId(), row); pdfList.add(pdfRow(row, true)); } if (data.getItems().isEmpty()) pdfList.add(LibraryUi.state("暂无可下载资源", null)); pdfList.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { pdfPage = n; loadPdfs(); })); pdfList.revalidate(); pdfList.repaint(); }
            public void onFailure(Throwable error) { if (request == pdfSerial) { pdfTotal.setText(""); LibraryUi.replace(pdfList, LibraryUi.state("PDF 资源暂未加载", StudentLibraryOnlinePanel.this::loadPdfs)); } }
        });
    }
    private JPanel pdfHeader() {
        JCheckBox all = new JCheckBox(); all.setOpaque(false);
        all.addActionListener(e -> { for (JCheckBox value : checks.values()) value.setSelected(all.isSelected()); updateSelected(); });
        return resourceCells(all, "资源名称", "大小", LibraryUi.label("操作", 12, true), true);
    }
    private JPanel pdfRow(PdfResourceView row, boolean selectable) {
        JCheckBox check = new JCheckBox(); check.setOpaque(false); if (selectable) { checks.put(row.getId(), check); check.addActionListener(e -> updateSelected()); }
        JPanel name = LibraryUi.stack(3); name.add(LibraryUi.label(row.getTitle(), 13, true)); name.add(LibraryUi.muted(row.getFileName() + " · " + PdfUi.date(row.getUploadedAt())));
        JButton download = LibraryUi.link("↓  下载", () -> downloadOne(row)); return resourceCells(check, name, PdfUi.size(row.getFileSize()), download, false);
    }
    private JPanel resourceCells(Component select, String name, String size, Component action, boolean header) { return resourceCells(select, LibraryUi.label(name, 12, header), size, action, header); }
    private JPanel resourceCells(Component select, Component name, String size, Component action, boolean header) {
        JPanel row = new JPanel(new GridBagLayout()); row.setOpaque(true); row.setBackground(header ? DesignTokens.PRIMARY_LIGHT : Color.WHITE); row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT));
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(9, 9, 9, 9); c.anchor = GridBagConstraints.WEST; c.gridx = 0; row.add(select, c); c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; row.add(name, c); c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; row.add(LibraryUi.label(size, 12, header), c); c.gridx = 3; row.add(action, c); return row;
    }
    private void updateSelected() { int n = 0; for (JCheckBox b : checks.values()) if (b.isSelected()) n++; selectedCount.setText("已选 " + n + " 项"); }
    private void downloadSelected() { List<PdfResourceView> rows = new ArrayList<>(); for (Map.Entry<Long, JCheckBox> e : checks.entrySet()) if (e.getValue().isSelected()) rows.add(visible.get(e.getKey())); if (rows.isEmpty()) { host.showWarning("请先勾选要下载的 PDF"); return; } download(rows); }
    private void downloadOne(PdfResourceView row) { download(java.util.Collections.singletonList(row)); }
    private void download(List<PdfResourceView> rows) { Path directory = PdfDownloadsPanel.chooseDirectory(this); if (directory == null) return; for (PdfResourceView row : rows) downloads.enqueue(row.getId(), row.getFileName(), directory, false); show("downloads"); }

    private JPanel mineWorkspace() { JPanel card = LibraryUi.card(); card.add(LibraryUi.between(LibraryUi.label("我的上传", 18, true), LibraryUi.link("刷新", this::loadMine))); card.add(mineList); return card; }
    private void loadMine() {
        if (pdf == null) return; final long request = ++mineSerial; LibraryUi.replace(mineList, LibraryUi.state("正在加载上传记录…", null));
        AsyncTask.run(() -> pdf.list(new PdfQuery("MINE", null, null, minePage, 10)), new AsyncTask.Callback<PageResult<PdfResourceView>>() {
            public void onSuccess(PageResult<PdfResourceView> data) { if (request != mineSerial) return; mineList.removeAll(); for (PdfResourceView row : data.getItems()) mineList.add(mineRow(row)); if (data.getItems().isEmpty()) mineList.add(LibraryUi.state("暂无上传记录", null)); mineList.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { minePage = n; loadMine(); })); mineList.revalidate(); mineList.repaint(); }
            public void onFailure(Throwable error) { if (request == mineSerial) LibraryUi.replace(mineList, LibraryUi.state("上传记录暂未加载", StudentLibraryOnlinePanel.this::loadMine)); }
        });
    }
    private JPanel mineRow(PdfResourceView row) { JPanel line = LibraryUi.stack(6); line.add(LibraryUi.between(LibraryUi.label(row.getTitle(), 14, true), LibraryUi.badge(PdfUi.state(row.getStatus()), "APPROVED".equals(row.getStatus())))); line.add(LibraryUi.muted(row.getFileName() + " · " + PdfUi.size(row.getFileSize()) + " · " + PdfUi.date(row.getUploadedAt()))); if (row.getRejectionReason() != null && !row.getRejectionReason().isBlank()) line.add(LibraryUi.muted("未通过原因：" + row.getRejectionReason())); line.setBorder(BorderFactory.createEmptyBorder(7, 4, 12, 4)); return line; }

    private JPanel webWorkspace() { JPanel card = LibraryUi.card(); card.add(LibraryUi.between(LibraryUi.label("网站资源", 18, true), LibraryUi.link("刷新", this::loadWeb))); card.add(webList); return card; }
    private void loadWeb() {
        final long request = ++webSerial; LibraryUi.replace(webList, LibraryUi.state("正在加载网站资源…", null));
        AsyncTask.run(() -> library.searchResources(new OnlineResourceSearchRequest(null, null, "ACTIVE", 1, 30)), new AsyncTask.Callback<PageResult<OnlineResourceView>>() {
            public void onSuccess(PageResult<OnlineResourceView> data) { if (request != webSerial) return; webList.removeAll(); for (OnlineResourceView row : data.getItems()) webList.add(webRow(row)); if (data.getItems().isEmpty()) webList.add(LibraryUi.state("暂无网站资源", null)); webList.revalidate(); webList.repaint(); }
            public void onFailure(Throwable error) { if (request == webSerial) LibraryUi.replace(webList, LibraryUi.state("网站资源暂未加载", StudentLibraryOnlinePanel.this::loadWeb)); }
        });
    }
    private JPanel webRow(OnlineResourceView row) { JPanel line = LibraryUi.stack(5); line.add(LibraryUi.between(LibraryUi.label(row.getTitle(), 14, true), LibraryUi.button("打开", false, () -> openWeb(row)))); line.add(LibraryUi.muted(LibraryUi.plain(row.getDescription()))); line.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(8, 5, 12, 5))); return line; }
    private void openWeb(OnlineResourceView row) {
        AsyncTask.run(() -> library.accessResource(row.getId()), new AsyncTask.Callback<OnlineResourceView>() {
            public void onSuccess(OnlineResourceView value) { try { OnlineResourceView resource = value == null ? row : value; URI uri = new URI(resource.getUrl()); if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException(); Desktop.getDesktop().browse(uri); } catch (Exception ex) { host.showWarning("当前环境无法打开该网站"); } }
            public void onFailure(Throwable error) { host.showError(AsyncTask.message(error)); }
        });
    }
}
