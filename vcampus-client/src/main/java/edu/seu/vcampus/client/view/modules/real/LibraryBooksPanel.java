package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 图书实时检索、详情、学生借阅和管理员维护。 */
public final class LibraryBooksPanel extends JPanel {
    private final BasePage page; private final LibraryClientService service; private final Role role;
    private final JLabel detail = UiFactory.muted("选择图书查看详情。"); private final AsyncPagedTable<BookDetail> books;
    private final LibraryBookEditorPanel editor;
    private long detailSerial;

    public LibraryBooksPanel(BasePage page, LibraryClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role; books = table(); add(books);
        if (role == Role.LIBRARIAN) { editor = new LibraryBookEditorPanel(new LibraryBookEditorPanel.Listener() {
            @Override public void onSave(BookUpsertRequest request) { saveBook(request); }
        }); add(editor); }
        else editor = null;
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
    }

    private AsyncPagedTable<BookDetail> table() {
        AsyncPagedTable<BookDetail> table = new AsyncPagedTable<BookDetail>("图书检索与库存", "按书名、作者或 ISBN 搜索。",
                "搜索书名、作者或 ISBN", new String[]{"全部状态", "在架", "不可借", "归档"},
                new String[]{"ISBN", "书名", "作者", "可借/总量", "位置", "状态"},
                new AsyncPagedTable.Loader<BookDetail>() {
                    @Override public PageSlice<BookDetail> load(int p, String keyword, String filter) throws Exception {
                        PageResult<BookDetail> result = service.searchBooks(new BookSearchRequest(keyword, null, bookStatus(filter), p, 20));
                        return new PageSlice<BookDetail>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<BookDetail>() {
                    @Override public Object[] values(BookDetail row) { return new Object[]{RealUi.text(row.getIsbn()), row.getTitle(), row.getAuthor(), row.getAvailableCopies() + "/" + row.getTotalCopies(),
                            RealUi.text(row.getLocation()), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<BookDetail>() {
                    @Override public void onSelected(BookDetail row) { selectBook(row); }
                });
        if (role == Role.STUDENT) { JButton borrow = new PrimaryButton("借阅"); borrow.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { borrow(); }
        }); table.addAction(borrow); }
        if (role == Role.LIBRARIAN) { JButton create = new PrimaryButton("新建图书"); create.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
        }); table.addAction(create); }
        return table;
    }

    private void selectBook(final BookDetail value) {
        if (value == null) { detailSerial++; detail.setText("选择图书查看详情。"); if (editor != null) editor.startNew(); return; }
        detail.setText("图书详情：" + RealUi.text(value.getTitle()) + "　ISBN " + RealUi.text(value.getIsbn()) + "　简介：" + RealUi.text(value.getDescription()));
        if (editor != null) editor.showBook(value);
        final long serial = ++detailSerial;
        AsyncTask.run(new AsyncTask.Work<BookDetail>() {
            @Override public BookDetail run() throws Exception { return service.bookDetail(value.getId()); }
        }, new AsyncTask.Callback<BookDetail>() {
            @Override public void onSuccess(BookDetail result) { if (serial != detailSerial) return; detail.setText("图书详情：" + RealUi.text(result.getTitle()) + "　可借 " + result.getAvailableCopies() + "/" + result.getTotalCopies() + "　位置：" + RealUi.text(result.getLocation())); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void borrow() {
        final BookDetail value = books.selectedItem(); if (value == null) { page.showWarning("请先选择图书。"); return; }
        AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.library.BorrowRecordView>() {
            @Override public edu.seu.vcampus.common.dto.library.BorrowRecordView run() throws Exception { return service.borrow(new BorrowRequest(value.getId())); }
        }, new AsyncTask.Callback<edu.seu.vcampus.common.dto.library.BorrowRecordView>() {
            @Override public void onSuccess(edu.seu.vcampus.common.dto.library.BorrowRecordView result) { page.showSuccess("借书成功，应还时间：" + RealUi.dateTime(result.getDueAt())); books.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void saveBook(final BookUpsertRequest request) {
        AsyncTask.run(new AsyncTask.Work<BookDetail>() {
            @Override public BookDetail run() throws Exception { return service.saveBook(request); }
        }, new AsyncTask.Callback<BookDetail>() {
            @Override public void onSuccess(BookDetail result) { page.showSuccess("图书已保存。"); books.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String bookStatus(String filter) { return "在架".equals(filter) ? "ON_SHELF" : "不可借".equals(filter) ? "UNAVAILABLE" : "归档".equals(filter) ? "ARCHIVED" : null; }
}
