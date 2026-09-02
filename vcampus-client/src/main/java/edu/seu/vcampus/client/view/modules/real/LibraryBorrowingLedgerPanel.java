package edu.seu.vcampus.client.view.modules.real;
import edu.seu.vcampus.client.service.library.CsvEncoder;
import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.PageResult;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
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
    private static final String[] CSV_HEADER = {"记录编号", "图书编号", "图书", "学生编号", "学生",
            "借出时间", "应还时间", "归还时间", "状态", "续借次数", "备注"};
    private final BasePage page;
    private final LibraryClientService service;
    private final AsyncPagedTable<BorrowRecordView> records;
    private final JTextField studentId = UiFactory.textField(12);
    private final JLabel exportState = UiFactory.muted("导出上限 5000 条");
    private final JButton export = new PrimaryButton("导出当前筛选 CSV");
    public LibraryBorrowingLedgerPanel(BasePage page, LibraryClientService service) {
        super();
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        add(filters());
        records = table();
        add(records);
    }
    private JPanel filters() {
        SectionCard card = new SectionCard("借阅台账筛选", "按状态和关键字筛选；学生编号仅管理员可筛选。");
        JPanel controls = UiFactory.horizontal(8);
        controls.add(UiFactory.labelledField("学生编号", studentId));
        JButton apply = new PrimaryButton("应用筛选");
        apply.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reloadAfterValidation(); }
        });
        JButton clear = new SecondaryButton("清除学生编号");
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { studentId.setText(""); records.reload(); }
        });
        controls.add(apply);
        controls.add(clear);
        controls.add(exportState);
        card.setContent(controls);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }
    private AsyncPagedTable<BorrowRecordView> table() {
        final AsyncPagedTable<BorrowRecordView> table = new AsyncPagedTable<BorrowRecordView>(
                "借阅管理", "图书管理员可按状态、学生编号或书名/学生关键字查询全部借阅记录。",
                "搜索书名、学生姓名或编号", new String[]{"全部状态", "借阅中", "逾期", "已归还", "遗失"},
                new String[]{"记录编号", "图书", "学生", "借出时间", "应还时间", "归还时间", "状态"},
                new AsyncPagedTable.Loader<BorrowRecordView>() {
                    @Override public PageSlice<BorrowRecordView> load(int p, String keyword, String filter) throws Exception {
                        BorrowAdminSearchRequest q = query(p, 20, keyword, filter); PageResult<BorrowRecordView> result = service.adminBorrowings(q);
                        return new PageSlice<BorrowRecordView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<BorrowRecordView>() {
                    @Override public Object[] values(BorrowRecordView row) { return new Object[]{row.getId(), row.getBookTitle(),
                            RealUi.text(row.getBorrowerName()) + "（" + row.getBorrowerUserId() + "）", RealUi.dateTime(row.getIssuedAt()),
                            RealUi.dateTime(row.getDueAt()), RealUi.dateTime(row.getReturnedAt()), RealUi.status(row.getStatus())}; }
                }, null);
        export.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { exportCsv(table); }
        });
        table.addAction(export);
        return table;
    }
    private void reloadAfterValidation() {
        try { parseStudentId(); records.reload(); }
        catch (IllegalArgumentException ex) { page.showError(ex.getMessage()); }
    }

    private void exportCsv(final AsyncPagedTable<BorrowRecordView> table) {
        final Long student = parseStudentIdSafely();
        if (student == null && studentId.getText().trim().length() > 0) return;
        final Path path = choosePath();
        if (path == null) return;
        if (needsOverwriteConfirmation(path)
                && !RealUi.confirm(this, "文件已存在，将覆盖已有文件“" + path.getFileName() + "”？")) {
            exportState.setText("已取消导出");
            return;
        }
        export.setEnabled(false);
        exportState.setText("正在导出…");
        final BorrowAdminSearchRequest first = query(1, EXPORT_PAGE_SIZE,
                table.getSearchKeyword(), table.getSelectedFilter(), student);
        AsyncTask.run(new AsyncTask.Work<Integer>() {
            @Override public Integer run() throws Exception {
                List<BorrowRecordView> rows = fetchAll(first); Files.write(path, CsvEncoder.encode(csvRows(rows))); return Integer.valueOf(rows.size());
            }
        }, new AsyncTask.Callback<Integer>() {
            @Override public void onSuccess(Integer count) {
                export.setEnabled(true); exportState.setText("已导出 " + count + " 条");
                page.showSuccess("借阅台账已导出。");
            }
            @Override public void onFailure(Throwable error) {
                export.setEnabled(true); exportState.setText("导出失败");
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
                String.valueOf(value.getBorrowerUserId()), value.getBorrowerName(),
                RealUi.dateTime(value.getIssuedAt()), RealUi.dateTime(value.getDueAt()),
                RealUi.dateTime(value.getReturnedAt()), RealUi.status(value.getStatus()),
                String.valueOf(value.getRenewCount()), value.getRemark()});
        return rows;
    }

    private BorrowAdminSearchRequest query(int page, int pageSize, String keyword, String filter) {
        return query(page, pageSize, keyword, filter, parseStudentId());
    }

    private static BorrowAdminSearchRequest query(int page, int pageSize, String keyword,
                                                  String filter, Long student) {
        return new BorrowAdminSearchRequest(status(filter), student, keyword, page, pageSize);
    }

    private Long parseStudentIdSafely() {
        try { return parseStudentId(); }
        catch (IllegalArgumentException ex) { page.showError(ex.getMessage()); return null; }
    }

    private Long parseStudentId() {
        String text = studentId.getText().trim();
        if (text.length() == 0) return null;
        try {
            long value = Long.parseLong(text);
            if (value <= 0L) throw new NumberFormatException();
            return Long.valueOf(value);
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("学生编号必须是正整数"); }
    }

    private static String status(String filter) {
        return "借阅中".equals(filter) ? "BORROWED" : "逾期".equals(filter) ? "OVERDUE"
                : "已归还".equals(filter) ? "RETURNED" : "遗失".equals(filter) ? "LOST" : null;
    }
}
