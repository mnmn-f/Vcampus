package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/** 带封面、库存、个人记录和借阅操作的独立详情窗口。 */
public final class LibraryBookDetailDialog extends JDialog {
    private final LibraryClientService service;
    private final long bookId;
    private final Role role;
    private final Runnable changed;
    private final JPanel content = new JPanel(new BorderLayout(20, 16));
    private final JLabel feedback = UiFactory.muted("正在读取图书详情…");
    private final JButton borrow = new PrimaryButton("借阅此书");
    private final JTextArea records = UiFactory.textArea(4, 24);

    public LibraryBookDetailDialog(Component owner, LibraryClientService service, long bookId,
                                   Role role, Runnable changed) {
        super(SwingUtilities.getWindowAncestor(owner), "书籍详情", ModalityType.APPLICATION_MODAL);
        this.service = service; this.bookId = bookId; this.role = role; this.changed = changed;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); content.setBackground(java.awt.Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel shell = new JPanel(new BorderLayout(8, 8));
        shell.add(new JScrollPane(new edu.seu.vcampus.client.ui.WidthTrackingPanel(content)), BorderLayout.CENTER);
        JPanel actions = new JPanel(new edu.seu.vcampus.client.ui.WrapLayout(8));
        if (role == Role.STUDENT) actions.add(borrow);
        JButton back = new SecondaryButton("返回列表"); back.addActionListener(event -> dispose());
        actions.add(back); actions.add(feedback); shell.add(actions, BorderLayout.SOUTH);
        borrow.setEnabled(false); borrow.addActionListener(event -> borrow());
        records.setEditable(false); setContentPane(shell); setMinimumSize(new Dimension(440, 460));
        setSize(860, 640); setLocationRelativeTo(owner); load();
    }
    private void load() {
        AsyncTask.run(() -> service.bookDetail(bookId), new AsyncTask.Callback<BookDetail>() {
            @Override public void onSuccess(BookDetail book) { showBook(book); loadRecords(); }
            @Override public void onFailure(Throwable error) { feedback.setText(AsyncTask.message(error)); }
        });
    }
    private void showBook(BookDetail book) {
        content.removeAll();
        LibraryBookDetailsPanel details = new LibraryBookDetailsPanel(); details.showBook(book);
        content.add(details, BorderLayout.CENTER);
        if (role == Role.STUDENT) content.add(UiFactory.labelledField("我与这本书的借阅记录", records), BorderLayout.SOUTH);
        borrow.setEnabled(role == Role.STUDENT && "ON_SHELF".equals(book.getStatus()) && book.getAvailableCopies() > 0);
        feedback.setText(" "); content.revalidate(); content.repaint();
    }
    private void loadRecords() {
        if (role != Role.STUDENT) return;
        AsyncTask.run(() -> {
            StringBuilder text = new StringBuilder(); boolean active = false;
            for (int page = 1; ; page++) {
                PageResult<BorrowRecordView> result = service.myBorrowings(new BorrowSearchRequest(null, page, 100));
                for (BorrowRecordView row : result.getItems()) if (row.getBookId() == bookId) {
                    text.append(RealUi.status(row.getStatus())).append("　借出：").append(RealUi.dateTime(row.getIssuedAt()))
                            .append("　应还：").append(RealUi.dateTime(row.getDueAt())).append('\n');
                    active |= "BORROWED".equals(row.getStatus()) || "OVERDUE".equals(row.getStatus());
                }
                if ((long) page * result.getPageSize() >= result.getTotal()) break;
            }
            return new String[]{text.length() == 0 ? "暂无这本书的借阅记录。" : text.toString(), String.valueOf(active)};
        }, new AsyncTask.Callback<String[]>() {
            @Override public void onSuccess(String[] result) {
                records.setText(result[0]); if (Boolean.parseBoolean(result[1])) { borrow.setEnabled(false); feedback.setText("你已借阅此书"); }
            }
            @Override public void onFailure(Throwable error) { records.setText(AsyncTask.message(error)); }
        });
    }
    private void borrow() {
        borrow.setEnabled(false);
        AsyncTask.run(() -> service.borrow(new BorrowRequest(bookId)), new AsyncTask.Callback<BorrowRecordView>() {
            @Override public void onSuccess(BorrowRecordView record) { changed.run(); load(); }
            @Override public void onFailure(Throwable error) { feedback.setText(AsyncTask.message(error)); borrow.setEnabled(true); }
        });
    }
}
