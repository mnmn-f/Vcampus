package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.ResponsiveGridLayout;
import edu.seu.vcampus.client.ui.components.StatusBadge;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.*;
import edu.seu.vcampus.common.security.Role;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 馆藏卡片与本人借阅工作区；完整借阅记录使用全宽表格展示。 */
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
        tabs.add(catalogTab); if (canBorrow()) tabs.add(historyTab); add(tabs);
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
        catalog.add(canBorrow() ? LibraryUi.columns(listCard, mine) : listCard);
        content.add(catalog, "catalog"); content.add(history, "history");
        host.addPropertyChangeListener("library.books.version", e -> { loadBooks(); loadBorrowings(); });
        showCatalog(); loadBooks(); if (canBorrow()) loadBorrowings();
    }

    void searchFor(String value) { keyword.setText(value == null ? "" : value); category = null; status.setSelectedIndex(0); pageNumber = 1; showCatalog(); loadBooks(); }
    void showCatalog() { ((CardLayout) content.getLayout()).show(content, "catalog"); activeTab(true); revalidate(); repaint(); }
    void showBorrowings() { if (!canBorrow()) return; ((CardLayout) content.getLayout()).show(content, "history"); activeTab(false); loadHistory(); revalidate(); repaint(); }
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
        if (canBorrow()) {
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
    static boolean isOverdue(BorrowRecordView row) {
        if (row == null || row.getReturnedAt() != null) return false;
        if ("OVERDUE".equals(row.getStatus())) return true;
        return "BORROWED".equals(row.getStatus()) && row.getDueAt() != null
                && row.getDueAt().isBefore(org.threeten.bp.LocalDateTime.now());
    }
    static String borrowingStatus(BorrowRecordView row) {
        return isOverdue(row) ? "已逾期" : RealUi.status(row == null ? null : row.getStatus());
    }
    private void loadBorrowings() {
        if (!canBorrow()) return; final long request = ++borrowSerial;
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
        JLabel due = LibraryUi.muted("应还 " + (record.getDueAt() == null ? "—" : record.getDueAt().toLocalDate()));
        if (isOverdue(record)) {
            due.setText(due.getText() + "　·　已逾期");
            due.setForeground(DesignTokens.ERROR);
            due.setFont(DesignTokens.medium(12));
        }
        line.add(due);
        JPanel action = LibraryUi.row(7); action.add(borrowingBadge(record));
        if (outstanding(record)) { JButton back = LibraryUi.button("归还", false, () -> { }); clearActions(back); back.addActionListener(e -> returnBook(record, back)); action.add(back); }
        line.add(action); line.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(3, 0, 11, 0))); return line;
    }
    private static JLabel borrowingBadge(BorrowRecordView record) {
        JLabel badge = LibraryUi.badge(borrowingStatus(record), !isOverdue(record));
        if (isOverdue(record)) {
            badge.setForeground(DesignTokens.ERROR);
            badge.setBackground(DesignTokens.ERROR_BACKGROUND);
        }
        return badge;
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
                card.add(LibraryUi.muted("借阅、应还和归还时间集中展示；逾期记录使用红色醒目标识。"));
                if (data.getItems().isEmpty()) card.add(LibraryUi.state("暂无借阅记录", null));
                else card.add(borrowingTable(data.getItems()));
                card.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { historyPage = n; loadHistory(); })); LibraryUi.replace(history, card);
            }
            public void onFailure(Throwable error) { if (request == historySerial) LibraryUi.replace(history, LibraryUi.state("借阅记录暂未加载", LibraryCatalogPanel.this::loadHistory)); }
        });
    }

    private JComponent borrowingTable(List<BorrowRecordView> rows) {
        BorrowHistoryModel model = new BorrowHistoryModel(rows);
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(48);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setForeground(DesignTokens.TEXT_PRIMARY);
        table.setFont(DesignTokens.regular(13));
        table.setSelectionBackground(DesignTokens.PRIMARY_LIGHT);
        table.setSelectionForeground(DesignTokens.TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setFont(DesignTokens.medium(13));
        table.getTableHeader().setForeground(DesignTokens.TEXT_PRIMARY);
        table.getTableHeader().setBackground(DesignTokens.TABLE_HEADER_BACKGROUND);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, DesignTokens.TABLE_GRID));
        table.setDefaultRenderer(Object.class, new BorrowCellRenderer());
        table.setDefaultRenderer(BorrowRecordView.class, new BorrowStatusRenderer());
        table.setDefaultRenderer(BorrowAction.class, new BorrowActionRenderer());
        table.setDefaultEditor(BorrowAction.class, new BorrowActionEditor());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        int visibleRows = Math.max(4, Math.min(8, rows.size()));
        int tableHeight = visibleRows * table.getRowHeight();
        table.setPreferredScrollableViewportSize(new Dimension(0, tableHeight));
        scroll.setPreferredSize(new Dimension(0, tableHeight + 42));
        scroll.setMinimumSize(new Dimension(0, tableHeight + 42));
        scroll.getViewport().addChangeListener(event ->
                applyBorrowColumnWidths(table, scroll.getViewport().getExtentSize().width));
        SwingUtilities.invokeLater(() ->
                applyBorrowColumnWidths(table, scroll.getViewport().getExtentSize().width));
        return scroll;
    }

    private static void applyBorrowColumnWidths(JTable table, int viewportWidth) {
        if (viewportWidth <= 0 || table.getColumnCount() != 6) return;
        int[] widths = borrowingColumnWidths(viewportWidth);
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
            table.getColumnModel().getColumn(column).setWidth(widths[column]);
        }
        table.revalidate();
    }

    /** 按当前表格视口分配六列；小于 650px 时由表格自己的横向滚动条承接。 */
    static int[] borrowingColumnWidths(int viewportWidth) {
        int available = Math.max(650, viewportWidth);
        int action = Math.max(82, Math.min(105, available / 12));
        int status = Math.max(88, Math.min(120, available / 10));
        int flexible = available - action - status;
        int title = Math.max(172, flexible * 36 / 100);
        int oneTime = (flexible - title) / 3;
        int lastTime = flexible - title - oneTime * 2;
        return new int[]{title, oneTime, oneTime, lastTime, status, action};
    }

    private static final class BorrowHistoryModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "图书名称", "借阅时间", "应还时间", "归还时间", "状态", "操作"
        };
        private final List<BorrowRecordView> rows;

        BorrowHistoryModel(List<BorrowRecordView> rows) {
            this.rows = new ArrayList<BorrowRecordView>(rows);
        }

        BorrowRecordView recordAt(int row) { return rows.get(row); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Class<?> getColumnClass(int column) {
            if (column == 4) return BorrowRecordView.class;
            if (column == 5) return BorrowAction.class;
            return String.class;
        }
        @Override public boolean isCellEditable(int row, int column) {
            return column == 5 && outstanding(rows.get(row));
        }
        @Override public Object getValueAt(int row, int column) {
            BorrowRecordView record = rows.get(row);
            switch (column) {
                case 0: return LibraryUi.plain(record.getBookTitle());
                case 1: return RealUi.dateTime(record.getIssuedAt());
                case 2: return RealUi.dateTime(record.getDueAt());
                case 3: return RealUi.dateTime(record.getReturnedAt());
                case 4: return record;
                case 5: return new BorrowAction(record);
                default: return "";
            }
        }
    }

    private static final class BorrowAction {
        private final BorrowRecordView record;
        BorrowAction(BorrowRecordView record) { this.record = record; }
    }

    private static final class BorrowCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            BorrowHistoryModel model = (BorrowHistoryModel) table.getModel();
            BorrowRecordView record = model.recordAt(table.convertRowIndexToModel(row));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setFont(column == 0 ? DesignTokens.medium(13) : DesignTokens.regular(13));
            if (!selected) {
                cell.setBackground(isOverdue(record) ? DesignTokens.ERROR_BACKGROUND
                        : row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFC, 0xF9));
            }
            cell.setForeground(isOverdue(record) && column == 2
                    ? DesignTokens.ERROR : DesignTokens.TEXT_PRIMARY);
            return cell;
        }
    }

    private static final class BorrowStatusRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            BorrowRecordView record = (BorrowRecordView) value;
            StatusBadge.Type type = isOverdue(record) ? StatusBadge.Type.ERROR
                    : "RETURNED".equals(record.getStatus()) ? StatusBadge.Type.SUCCESS
                    : "BORROWED".equals(record.getStatus()) ? StatusBadge.Type.INFO
                    : "LOST".equals(record.getStatus()) ? StatusBadge.Type.ERROR
                    : StatusBadge.Type.NEUTRAL;
            JPanel holder = tableCellHolder(table, selected, row, record);
            holder.add(new StatusBadge(borrowingStatus(record), type));
            return holder;
        }
    }

    private static final class BorrowActionRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            BorrowRecordView record = ((BorrowAction) value).record;
            JPanel holder = tableCellHolder(table, selected, row, record);
            if (outstanding(record)) holder.add(LibraryUi.button("归还", false, () -> { }));
            else holder.add(LibraryUi.muted("—"));
            return holder;
        }
    }

    private final class BorrowActionEditor extends AbstractCellEditor implements TableCellEditor {
        private BorrowAction action;

        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                boolean selected, int row, int column) {
            action = (BorrowAction) value;
            JPanel holder = tableCellHolder(table, true, row, action.record);
            JButton button = LibraryUi.button("归还", false, () -> { });
            clearActions(button);
            button.addActionListener(event -> {
                stopCellEditing();
                returnBook(action.record, button);
            });
            holder.add(button);
            return holder;
        }

        @Override public Object getCellEditorValue() { return action; }
    }

    private static JPanel tableCellHolder(JTable table, boolean selected, int row,
                                          BorrowRecordView record) {
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        holder.setOpaque(true);
        holder.setBackground(selected ? table.getSelectionBackground()
                : isOverdue(record) ? DesignTokens.ERROR_BACKGROUND
                : row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFC, 0xF9));
        return holder;
    }

    private boolean canBorrow() {
        return role == Role.STUDENT || role == Role.TEACHER;
    }
}
