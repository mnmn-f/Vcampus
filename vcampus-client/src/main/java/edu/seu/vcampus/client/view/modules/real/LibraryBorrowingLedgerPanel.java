package edu.seu.vcampus.client.view.modules.real;
import edu.seu.vcampus.client.service.library.CsvEncoder;
import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.PageResult;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/** 图书管理员借阅台账；查询和 CSV 导出均复用管理员分页服务。 */
public final class LibraryBorrowingLedgerPanel extends JPanel {
    static final int EXPORT_PAGE_SIZE = 100;
    static final int EXPORT_MAX_ROWS = 5000;
    private static final String[] CSV_HEADER = {"记录编号", "图书编号", "图书", "学生",
            "借出时间", "应还时间", "归还时间", "状态", "续借次数", "备注"};
    private final BasePage page;
    private final LibraryClientService service;
    private final AsyncPagedTable<BorrowRecordView> records;
    private final JButton export = new PrimaryButton("导出当前筛选 CSV");
    public LibraryBorrowingLedgerPanel(BasePage page, LibraryClientService service) {
        super();
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        records = table();
        add(records);
    }
    private AsyncPagedTable<BorrowRecordView> table() {
        final AsyncPagedTable<BorrowRecordView> table = new AsyncPagedTable<BorrowRecordView>(
                "借阅管理", "", "搜索书名或学生姓名", new String[]{"全部状态", "借阅中", "逾期", "已归还", "遗失"},
                new String[]{"记录编号", "图书", "学生", "借出时间", "应还时间", "归还时间", "状态"},
                new AsyncPagedTable.Loader<BorrowRecordView>() {
                    @Override public PageSlice<BorrowRecordView> load(int p, String keyword, String filter) throws Exception {
                        BorrowAdminSearchRequest q = query(p, 20, keyword, filter); PageResult<BorrowRecordView> result = service.adminBorrowings(q);
                        return new PageSlice<BorrowRecordView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<BorrowRecordView>() {
                    @Override public Object[] values(BorrowRecordView row) { return new Object[]{row.getId(), row.getBookTitle(),
                            RealUi.text(row.getBorrowerName()), RealUi.dateTime(row.getIssuedAt()),
                            RealUi.dateTime(row.getDueAt()), RealUi.dateTime(row.getReturnedAt()), RealUi.status(row.getStatus())}; }
                }, null);
        export.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { exportCsv(table); }
        });
        table.addAction(export);
        return table;
    }

    private void exportCsv(final AsyncPagedTable<BorrowRecordView> table) {
        final Path path = choosePath();
        if (path == null) return;
        if (needsOverwriteConfirmation(path)
                && !RealUi.confirm(this, "文件已存在，将覆盖已有文件“" + path.getFileName() + "”？")) {
            return;
        }
        export.setEnabled(false);
        final BorrowAdminSearchRequest first = query(1, EXPORT_PAGE_SIZE,
                table.getSearchKeyword(), table.getSelectedFilter(), null);
        AsyncTask.run(new AsyncTask.Work<Integer>() {
            @Override public Integer run() throws Exception {
                List<BorrowRecordView> rows = fetchAll(first); Files.write(path, CsvEncoder.encode(csvRows(rows))); return Integer.valueOf(rows.size());
            }
        }, new AsyncTask.Callback<Integer>() {
            @Override public void onSuccess(Integer count) {
                export.setEnabled(true);
                page.showSuccess("借阅台账已导出。");
            }
            @Override public void onFailure(Throwable error) {
                export.setEnabled(true);
                page.showError(AsyncTask.message(error));
            }
        });
    }

    private Path choosePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出借阅台账");
        chooser.setSelectedFile(new File("借阅台账.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 文件", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File selected = chooser.getSelectedFile();
        String name = selected.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
                ? selected.getName() : selected.getName() + ".csv";
        return new File(selected.getParentFile(), name).toPath();
    }

    static boolean needsOverwriteConfirmation(Path path) {
        return path != null && Files.exists(path);
    }

    private List<BorrowRecordView> fetchAll(BorrowAdminSearchRequest first) throws Exception {
        List<BorrowRecordView> rows = new ArrayList<BorrowRecordView>();
        long total = -1L;
        for (int page = 1; page <= EXPORT_MAX_ROWS / EXPORT_PAGE_SIZE; page++) {
            BorrowAdminSearchRequest q = new BorrowAdminSearchRequest(first.getStatus(),
                    first.getStudentId(), first.getKeyword(), page, EXPORT_PAGE_SIZE);
            PageResult<BorrowRecordView> result = service.adminBorrowings(q);
            if (result == null) throw new IllegalStateException("分页响应为空");
            if (total < 0L) total = result.getTotal();
            if (total > EXPORT_MAX_ROWS) {
                throw new IllegalArgumentException("匹配记录超过导出上限 " + EXPORT_MAX_ROWS + " 条，请缩小筛选范围。");
            }
            rows.addAll(result.getItems());
            if (!result.hasNext() || rows.size() >= total) return rows;
        }
        throw new IllegalArgumentException("导出分页超过安全上限，请缩小筛选范围。");
    }

    static List<String[]> csvRows(List<BorrowRecordView> values) {
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(CSV_HEADER.clone());
        if (values != null) for (BorrowRecordView value : values) rows.add(new String[]{
                String.valueOf(value.getId()), String.valueOf(value.getBookId()), value.getBookTitle(),
                value.getBorrowerName(),
                RealUi.dateTime(value.getIssuedAt()), RealUi.dateTime(value.getDueAt()),
                RealUi.dateTime(value.getReturnedAt()), RealUi.status(value.getStatus()),
                String.valueOf(value.getRenewCount()), value.getRemark()});
        return rows;
    }

    private BorrowAdminSearchRequest query(int page, int pageSize, String keyword, String filter) {
        return query(page, pageSize, keyword, filter, null);
    }

    private static BorrowAdminSearchRequest query(int page, int pageSize, String keyword,
                                                  String filter, Long student) {
        return new BorrowAdminSearchRequest(status(filter), student, keyword, page, pageSize);
    }

    private static String status(String filter) {
        return "借阅中".equals(filter) ? "BORROWED" : "逾期".equals(filter) ? "OVERDUE"
                : "已归还".equals(filter) ? "RETURNED" : "遗失".equals(filter) ? "LOST" : null;
    }
}
