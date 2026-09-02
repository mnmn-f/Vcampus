package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;

import javax.swing.JButton;
import javax.swing.JPanel;

/** 学生我的借阅和高风险还书操作。 */
public final class LibraryBorrowingsPanel extends JPanel {
    private final BasePage page; private final LibraryClientService service; private final AsyncPagedTable<BorrowRecordView> records;

    public LibraryBorrowingsPanel(BasePage page, LibraryClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; records = table(); add(records);
    }

    private AsyncPagedTable<BorrowRecordView> table() {
        AsyncPagedTable<BorrowRecordView> table = new AsyncPagedTable<BorrowRecordView>("我的借阅记录", "借书和还书会更新库存与借阅记录。",
                "可按状态筛选", new String[]{"全部状态", "借阅中", "逾期", "已归还"},
                new String[]{"记录编号", "图书", "借出时间", "应还时间", "归还时间", "状态"},
                new AsyncPagedTable.Loader<BorrowRecordView>() {
                    @Override public PageSlice<BorrowRecordView> load(int p, String keyword, String filter) throws Exception {
                        PageResult<BorrowRecordView> result = service.myBorrowings(new BorrowSearchRequest(borrowStatus(filter), p, 20));
                        return new PageSlice<BorrowRecordView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<BorrowRecordView>() {
                    @Override public Object[] values(BorrowRecordView row) { return new Object[]{row.getId(), row.getBookTitle(), RealUi.dateTime(row.getIssuedAt()), RealUi.dateTime(row.getDueAt()), RealUi.dateTime(row.getReturnedAt()), RealUi.status(row.getStatus())}; }
                }, null);
        JButton returned = new DangerButton("归还选中图书"); returned.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { returnBook(); }
        }); table.addAction(returned); return table;
    }

    private void returnBook() {
        final BorrowRecordView value = records.selectedItem(); if (value == null) { page.showWarning("请先选择借阅记录。"); return; }
        if (!RealUi.confirm(this, "确认归还“" + value.getBookTitle() + "”？归还后库存会立即增加。")) return;
        AsyncTask.run(new AsyncTask.Work<BorrowRecordView>() {
            @Override public BorrowRecordView run() throws Exception { return service.returnBook(value.getId()); }
        }, new AsyncTask.Callback<BorrowRecordView>() {
            @Override public void onSuccess(BorrowRecordView result) { page.showSuccess("还书成功，库存已更新。"); records.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String borrowStatus(String filter) { return "借阅中".equals(filter) ? "BORROWED" : "逾期".equals(filter) ? "OVERDUE" : "已归还".equals(filter) ? "RETURNED" : null; }
}
