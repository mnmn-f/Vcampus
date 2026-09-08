package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.ResponsiveGridLayout;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.*;
import edu.seu.vcampus.common.security.Role;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 馆藏卡片与本人借阅工作区；按钮直接绑定图书和借阅记录 ID。 */
final class LibraryCatalogPanel extends JPanel {
    private final BasePage host; private final LibraryClientService service; private final Role role;
    private final JTextField keyword = LibraryUi.search("搜索书名、作者或 ISBN");
    private final JComboBox<String> status = LibraryUi.combo(new String[]{"全部状态", "在架", "不可借"});
    private final JPanel results = LibraryUi.stack(14), borrowed = LibraryUi.stack(12), history = LibraryUi.stack(12);
    private final JPanel content = new JPanel(new CardLayout());
    private final JLabel total = LibraryUi.muted(""), borrowCount = LibraryUi.muted("");
    private String category; private int pageNumber = 1, historyPage = 1; private long loadSerial, borrowSerial, historySerial;
    private final JButton catalogTab, historyTab;

    LibraryCatalogPanel(BasePage host, LibraryClientService service, Role role) {
        super(new LibraryUi.StackLayout(16)); setOpaque(false); this.host = host; this.service = service; this.role = role;
        add(LibraryUi.heading("图书查阅"));
        JPanel tabs = LibraryUi.row(10);
        catalogTab = LibraryUi.button("馆藏图书", false, this::showCatalog);
        historyTab = LibraryUi.button("我的借阅", false, this::showBorrowings);
        tabs.add(catalogTab); if (role == Role.STUDENT) tabs.add(historyTab); add(tabs);
        content.setOpaque(false); add(content);

        JPanel catalog = LibraryUi.stack(18), filters = LibraryUi.card();
        JButton searchButton = LibraryUi.button("搜索", true, () -> { pageNumber = 1; loadBooks(); });
        keyword.addActionListener(e -> searchButton.doClick()); filters.add(LibraryUi.between(keyword, searchButton));
        JPanel chips = LibraryUi.row(6); List<JButton> categoryButtons = new ArrayList<>();
        for (String value : new String[]{"全部", "文学", "历史", "计算机", "艺术"}) {
            JButton chip = LibraryUi.button(value, false, () -> {
                category = "全部".equals(value) ? null : value; pageNumber = 1;
                for (JButton item : categoryButtons) { item.putClientProperty("library.active", item.getText().equals(value)); item.repaint(); }
                loadBooks();
            });
            if ("全部".equals(value)) chip.putClientProperty("library.active", true);
            categoryButtons.add(chip); chips.add(chip);
        }
        filters.add(LibraryUi.between(chips, status)); status.addActionListener(e -> { pageNumber = 1; loadBooks(); }); catalog.add(filters);
        JPanel listCard = LibraryUi.card(); listCard.add(LibraryUi.between(LibraryUi.label("馆藏图书", 18, true), total)); listCard.add(results);
        JPanel mine = LibraryUi.card(); mine.add(LibraryUi.between(LibraryUi.label("我的借阅", 18, true), LibraryUi.link("全部 →", this::showBorrowings)));
        mine.add(borrowCount); mine.add(borrowed);
        catalog.add(role == Role.STUDENT ? LibraryUi.columns(listCard, mine) : listCard);
        content.add(catalog, "catalog"); content.add(history, "history");
        host.addPropertyChangeListener("library.books.version", e -> { loadBooks(); loadBorrowings(); });
        showCatalog(); loadBooks(); if (role == Role.STUDENT) loadBorrowings();
    }

    void searchFor(String value) { keyword.setText(value == null ? "" : value); category = null; status.setSelectedIndex(0); pageNumber = 1; showCatalog(); loadBooks(); }
    void showCatalog() { ((CardLayout) content.getLayout()).show(content, "catalog"); activeTab(true); revalidate(); repaint(); }
    void showBorrowings() { if (role != Role.STUDENT) return; ((CardLayout) content.getLayout()).show(content, "history"); activeTab(false); loadHistory(); revalidate(); repaint(); }
    private void activeTab(boolean catalog) { catalogTab.putClientProperty("library.active", catalog); historyTab.putClientProperty("library.active", !catalog); catalogTab.repaint(); historyTab.repaint(); }

    private void loadBooks() {
        final long request = ++loadSerial; final int requestedPage = pageNumber;
        BookSearchRequest query = new BookSearchRequest(keyword.getText().trim(), category,
                status.getSelectedIndex() == 1 ? "ON_SHELF" : status.getSelectedIndex() == 2 ? "UNAVAILABLE" : null, requestedPage, 4);
        LibraryUi.replace(results, LibraryUi.state("正在加载馆藏…", null)); total.setText("");
        AsyncTask.run(() -> service.searchBooks(query), new AsyncTask.Callback<PageResult<BookDetail>>() {
            public void onSuccess(PageResult<BookDetail> data) {
                if (request != loadSerial) return; results.removeAll(); total.setText("共 " + data.getTotal() + " 本");
                JPanel grid = new JPanel(new ResponsiveGridLayout(285, 2, 12)); grid.setOpaque(false);
                for (BookDetail book : data.getItems()) grid.add(bookCard(book));
                results.add(data.getItems().isEmpty() ? LibraryUi.state("没有找到相关图书", null) : grid);
                results.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { pageNumber = n; loadBooks(); }));
                results.revalidate(); results.repaint();
            }
            public void onFailure(Throwable error) { if (request == loadSerial) { total.setText(""); LibraryUi.replace(results, LibraryUi.state("图书暂未加载", LibraryCatalogPanel.this::loadBooks)); } }
        });
    }
    private JPanel bookCard(BookDetail book) {
        JPanel card = LibraryUi.card(); JPanel information = LibraryUi.stack(7);
        information.add(LibraryUi.label(book.getTitle(), 16, true)); information.add(LibraryUi.muted(LibraryUi.plain(book.getAuthor())));
        information.add(LibraryUi.muted(LibraryUi.plain(book.getCategory()))); information.add(LibraryUi.muted(LibraryUi.plain(book.getLocation())));
        JPanel upper = new JPanel(new BorderLayout(13, 0)); upper.setOpaque(false); upper.add(new LibraryUi.Cover(book, 78, 114), BorderLayout.WEST); upper.add(information, BorderLayout.CENTER); card.add(upper);
        JPanel actions = LibraryUi.row(6); boolean canBorrow = LibraryUi.borrowable(book);
        actions.add(LibraryUi.badge(canBorrow ? "可借 " + book.getAvailableCopies() + " / " + book.getTotalCopies() : "暂无可借", canBorrow));
        if (role == Role.STUDENT) {
            JButton borrow = LibraryUi.button("借阅此书", true, () -> { }); clearActions(borrow);
            borrow.addActionListener(e -> borrow(book, borrow)); borrow.setEnabled(canBorrow); actions.add(borrow);
        }
        actions.add(LibraryUi.link("详情", () -> showDetails(book))); card.add(actions); return card;
    }
    private static void clearActions(JButton button) { for (java.awt.event.ActionListener listener : button.getActionListeners()) button.removeActionListener(listener); }
    private void showDetails(BookDetail book) {
        new LibraryBookDetailDialog(this, service, book.getId(), role, this::changed).setVisible(true);
    }
    private void borrow(BookDetail book, JButton button) {
        button.setEnabled(false);
        AsyncTask.run(() -> service.borrow(new BorrowRequest(book.getId())), new AsyncTask.Callback<BorrowRecordView>() {
            public void onSuccess(BorrowRecordView record) { host.showSuccess("借阅成功，应还日期：" + record.getDueAt().toLocalDate()); changed(); }
            public void onFailure(Throwable error) { button.setEnabled(true); host.showError(AsyncTask.message(error)); }
        });
    }
    private void changed() { host.putClientProperty("library.books.version", System.nanoTime()); }

    static List<BorrowRecordView> allBorrowings(LibraryClientService service) throws Exception {
        List<BorrowRecordView> rows = new ArrayList<>();
        for (int p = 1; ; p++) {
            PageResult<BorrowRecordView> data = service.myBorrowings(new BorrowSearchRequest(null, p, 100)); rows.addAll(data.getItems());
            if (data.getItems().isEmpty() || (long) p * data.getPageSize() >= data.getTotal()) return rows;
        }
    }
    static boolean outstanding(BorrowRecordView row) { return "BORROWED".equals(row.getStatus()) || "OVERDUE".equals(row.getStatus()); }
    private void loadBorrowings() {
        if (role != Role.STUDENT) return; final long request = ++borrowSerial;
        AsyncTask.run(() -> allBorrowings(service), new AsyncTask.Callback<List<BorrowRecordView>>() {
            public void onSuccess(List<BorrowRecordView> rows) {
                if (request != borrowSerial) return; List<BorrowRecordView> active = new ArrayList<>();
                for (BorrowRecordView row : rows) if (outstanding(row)) active.add(row);
                active.sort(java.util.Comparator.comparing(BorrowRecordView::getDueAt)); borrowCount.setText("当前借阅 " + active.size() + " 本"); borrowed.removeAll();
                for (int i = 0; i < Math.min(3, active.size()); i++) borrowed.add(borrowRow(active.get(i)));
                if (active.isEmpty()) borrowed.add(LibraryUi.state("暂无待归还图书", null)); borrowed.revalidate(); borrowed.repaint();
            }
            public void onFailure(Throwable error) { if (request == borrowSerial) { borrowCount.setText(""); LibraryUi.replace(borrowed, LibraryUi.state("借阅记录暂未加载", LibraryCatalogPanel.this::loadBorrowings)); } }
        });
    }
    private JPanel borrowRow(BorrowRecordView record) {
        JPanel line = LibraryUi.stack(7); line.add(LibraryUi.label(record.getBookTitle(), 14, true));
        line.add(LibraryUi.muted("应还 " + (record.getDueAt() == null ? "—" : record.getDueAt().toLocalDate())));
        JPanel action = LibraryUi.row(7); action.add(LibraryUi.badge(RealUi.status(record.getStatus()), !"OVERDUE".equals(record.getStatus())));
        if (outstanding(record)) { JButton back = LibraryUi.button("归还", false, () -> { }); clearActions(back); back.addActionListener(e -> returnBook(record, back)); action.add(back); }
        line.add(action); line.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(3, 0, 11, 0))); return line;
    }
    private void returnBook(BorrowRecordView record, JButton button) {
        if (!RealUi.confirm(this, "确认归还《" + record.getBookTitle() + "》？")) return; button.setEnabled(false);
        AsyncTask.run(() -> service.returnBook(record.getId()), new AsyncTask.Callback<BorrowRecordView>() {
            public void onSuccess(BorrowRecordView value) { host.showSuccess("归还成功"); changed(); loadHistory(); }
            public void onFailure(Throwable error) { button.setEnabled(true); host.showError(AsyncTask.message(error)); }
        });
    }
    private void loadHistory() {
        final long request = ++historySerial; LibraryUi.replace(history, LibraryUi.state("正在加载借阅记录…", null));
        AsyncTask.run(() -> service.myBorrowings(new BorrowSearchRequest(null, historyPage, 8)), new AsyncTask.Callback<PageResult<BorrowRecordView>>() {
            public void onSuccess(PageResult<BorrowRecordView> data) {
                if (request != historySerial) return; JPanel card = LibraryUi.card();
                card.add(LibraryUi.between(LibraryUi.label("我的借阅记录", 18, true), LibraryUi.link("返回馆藏", LibraryCatalogPanel.this::showCatalog)));
                for (BorrowRecordView row : data.getItems()) card.add(borrowRow(row)); if (data.getItems().isEmpty()) card.add(LibraryUi.state("暂无借阅记录", null));
                card.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { historyPage = n; loadHistory(); })); LibraryUi.replace(history, card);
            }
            public void onFailure(Throwable error) { if (request == historySerial) LibraryUi.replace(history, LibraryUi.state("借阅记录暂未加载", LibraryCatalogPanel.this::loadHistory)); }
        });
    }
}
