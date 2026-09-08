package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.PdfClientService;
import edu.seu.vcampus.client.service.library.PdfFileTransfers;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PageResult;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 两个后台下载任务并行执行，逐文件显示进度和可持久保存的历史。 */
public final class PdfDownloadsPanel extends JPanel {
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, work -> {
        Thread thread = new Thread(work, "library-pdf-download"); thread.setDaemon(true); return thread;
    });
    private final PdfClientService service; private final BasePage page;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"文件", "状态", "进度", "已下载", "结果 / 保存位置"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final List<Task> tasks = new ArrayList<Task>();
    private final AsyncPagedTable<PdfDownloadRecord> history;
    public PdfDownloadsPanel(BasePage page, PdfClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.service = service; this.page = page; table.setRowHeight(34);
        table.getColumnModel().getColumn(2).setCellRenderer((t, value, selected, focus, row, column) -> {
            JProgressBar progress = new JProgressBar(0, 100); progress.setValue(value instanceof Number ? ((Number) value).intValue() : 0);
            progress.setStringPainted(true); return progress;
        });
        SectionCard active = new SectionCard("下载任务", "");
        JPanel content = new JPanel(new BorderLayout(8, 8)); content.setOpaque(false);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setPreferredSize(new java.awt.Dimension(0, 180)); content.add(scroll, BorderLayout.CENTER);
        JButton retry = new SecondaryButton("重试选中失败任务"); retry.addActionListener(event -> retry()); content.add(retry, BorderLayout.SOUTH);
        active.setContent(content); add(active);
        history = new AsyncPagedTable<PdfDownloadRecord>("历史下载记录", "", "搜索下载记录", null,
                new String[]{"资源名称", "文件名", "下载时间", "结果", "已传输", "失败原因"},
                (p, keyword, filter) -> { PageResult<PdfDownloadRecord> result = service.history(new PdfQuery("MINE", null, null, p, 20));
                    return new PageSlice<PdfDownloadRecord>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize()); },
                r -> new Object[]{r.getTitle(), r.getFileName(), PdfUi.date(r.getStartedAt()), PdfUi.state(r.getStatus()), PdfUi.size(r.getTransferred()), RealUi.text(r.getFailureReason())}, null);
        JButton again = new SecondaryButton("重新下载"); again.addActionListener(event -> {
            PdfDownloadRecord record = history.selectedItem(); if (record == null) { page.showWarning("请先选择下载记录。"); return; }
            Path directory = chooseDirectory(this); if (directory != null) enqueue(record.getResourceId(), record.getFileName(), directory, false);
        }); history.addAction(again); add(history);
    }
    public void enqueue(long id, String name, Path directory, boolean open) {
        int row = tasks.size(); Task task = new Task(id, name, directory, open); tasks.add(task);
        model.addRow(new Object[]{name, "等待中", 0, "0 KB", ""});
        WORKERS.submit(() -> {
            SwingUtilities.invokeLater(() -> model.setValueAt("下载中", row, 1));
            try {
                Path saved = new PdfFileTransfers(service).download(id, directory, (bytes, total) -> SwingUtilities.invokeLater(() -> {
                    model.setValueAt((int) (bytes * 100 / Math.max(total, 1)), row, 2);
                    model.setValueAt(PdfUi.size(bytes) + " / " + PdfUi.size(total), row, 3);
                    if (bytes == total) model.setValueAt("正在校验", row, 1);
                }));
                SwingUtilities.invokeLater(() -> {
                    model.setValueAt("已完成", row, 1); model.setValueAt(saved.toString(), row, 4); history.refreshCurrentPage();
                    if (task.open) openFile(saved);
                });
            } catch (Exception ex) { SwingUtilities.invokeLater(() -> {
                task.failed = true; model.setValueAt("失败", row, 1); model.setValueAt(AsyncTask.message(ex), row, 4); history.refreshCurrentPage();
            }); }
        });
    }
    private void retry() {
        int row = table.getSelectedRow();
        if (row < 0 || !tasks.get(row).failed) { page.showWarning("请选择一个失败的下载任务。"); return; }
        Task old = tasks.get(row); enqueue(old.id, old.name, old.directory, old.open);
    }
    private void openFile(Path file) {
        try {
            if (!java.awt.Desktop.isDesktopSupported()) throw new UnsupportedOperationException();
            java.awt.Desktop.getDesktop().open(file.toFile());
        } catch (Exception ex) { page.showInfo("文件已保存，请用 PDF 阅读器打开：" + file); }
    }
    static Path chooseDirectory(Component owner) {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("选择 PDF 保存文件夹"); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return chooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }
    private static final class Task {
        final long id; final String name; final Path directory; final boolean open; boolean failed;
        Task(long id, String name, Path directory, boolean open) { this.id = id; this.name = name; this.directory = directory; this.open = open; }
    }
}
