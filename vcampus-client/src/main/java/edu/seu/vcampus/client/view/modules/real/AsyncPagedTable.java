package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DataTableToolbar;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.ui.components.TableViewport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

/** 可复用的分页表格，统一筛选、刷新、空态、错误与翻页状态。 */
public final class AsyncPagedTable<T> extends SectionCard {
    public interface Loader<T> { PageSlice<T> load(int page, String keyword, String filter) throws Exception; }
    public interface RowMapper<T> { Object[] values(T row); }
    public interface SelectionListener<T> { void onSelected(T row); }
    public interface FilterCondition { boolean isActive(); }

    private final DataTableToolbar toolbar;
    private final DefaultTableModel model;
    private final JTable table;
    private final TableViewport viewport;
    private final JLabel state = UiFactory.muted("等待更新");
    private final JButton previous = new SecondaryButton("上一页");
    private final JButton next = new SecondaryButton("下一页");
    private final JButton retry = new SecondaryButton("刷新");
    private final Loader<T> loader;
    private final RowMapper<T> mapper;
    private final SelectionListener<T> selectionListener;
    private List<T> items = Collections.emptyList();
    private java.util.function.Function<T, Object> itemKey;
    private boolean checkboxSelection;
    private int page = 1;
    private int requestSerial;
    private boolean hasNext;
    private FilterCondition additionalCondition;
    private boolean columnsFill;
    private boolean busy;
    private boolean replacingRows;
    private boolean liveSelectionUpdates;
    /** 锁定为「按内容定宽、横向滚动」，之后的 {@link #setColumnsFill(boolean)} 一律忽略。 */
    private boolean naturalWidths;
    private int maxColumnWidth = -1;
    /** 上次按哪个宽度排的列；窗口没变宽就不重排，免得和滚动条显隐来回打架。 */
    private int lastFitWidth = -1;

    public AsyncPagedTable(String title, String subtitle, String searchHint, String[] filters,
                           String[] columns, Loader<T> loader, RowMapper<T> mapper,
                           SelectionListener<T> selectionListener) {
        super(title, subtitle);
        if (loader == null || mapper == null) throw new IllegalArgumentException("table loader required");
        this.loader = loader; this.mapper = mapper; this.selectionListener = selectionListener;
        toolbar = new DataTableToolbar(searchHint, filters); model = model(columns);
        table = new JTable(model) {
            @Override public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
                if (row < 0 || column < 0) return null;
                Object value = getValueAt(row, column); return value == null ? null : String.valueOf(value);
            }
        };
        table.setToolTipText(""); configureTable();
        previous.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(page - 1); }
        });
        next.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(page + 1); }
        });
        retry.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(page); }
        });
        toolbar.onSearch(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(1); }
        });
        if (toolbar.getFilterBox() != null) toolbar.getFilterBox().addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(1); }
        });
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) { selectedChanged(e.getValueIsAdjusting()); }
        });
        JPanel footer = new JPanel(new BorderLayout(DesignTokens.SPACE_8, 0)); footer.setOpaque(false);
        footer.add(state, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        actions.add(retry); actions.add(previous); actions.add(next); footer.add(actions, BorderLayout.EAST);
        JPanel content = new JPanel(new BorderLayout(0, DesignTokens.SPACE_8)); content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH); viewport = new TableViewport(table);
        content.add(viewport, BorderLayout.CENTER); content.add(footer, BorderLayout.SOUTH); setContent(content);
        // 首次布局时 viewport 宽度还是 0，那一遍列宽只能按内容定；等它真正拿到宽度
        // （以及之后每次窗口变宽变窄）再重排一次，否则窄栏里的表永远是按内容那份宽度。
        viewport.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) { refitColumns(); }
        });
        load(1);
        edu.seu.vcampus.client.ui.VisibleRefresh.attach(this,
                () -> !busy && (table.getSelectedRow() < 0 || itemKey != null),
                () -> load(page, true));
    }

    private void refitColumns() {
        if (items.isEmpty()) return;
        int available = viewport.tableWidth();
        if (available <= 0 || available == lastFitWidth) return;
        lastFitWidth = available;
        if (columnsFill) DormTables.fitColumnsWithin(table, available);
        else DormTables.fitColumns(table, available, maxColumnWidth);
    }

    public void addAction(JButton button) { toolbar.addAction(button); }

    /** 表格最少占几行高，数据不足时用空行补足；见 {@link TableViewport#setMinVisibleRows(int)}。 */
    public void setMinVisibleRows(int rows) { viewport.setMinVisibleRows(rows); }

    /**
     * 列宽装不下时按比例压缩，而不是横向滚动。
     *
     * <p>默认是按内容定宽、装不下就横向滚动——列多的台账表这样最好读。但放在窄栏里的
     * 表格另说：{@code BasePage} 的滚动面板禁了横向滚动条，超出的列不是「滚一下能看到」，
     * 而是直接被裁掉。宿舍模块的表统一走这一档，由 {@code DormUi.flatten} 一次性打开。</p>
     */
    public void setColumnsFill(boolean fill) {
        if (naturalWidths) return;
        columnsFill = fill;
        lastFitWidth = -1;
        refitColumns();
    }

    /**
     * 列宽只由内容决定，装不下就在表格自己的滚动条里左右滚，不压缩。
     *
     * <p>这是 {@link #setColumnsFill} 的反向开关，而且是锁：{@code DormUi.flatten} 会在
     * 页面搭好后把宿舍模块所有表统一切成压缩模式，对「理由就是正文」的表来说压缩等于
     * 全变省略号。锁上之后 flatten 的那一下不再生效。</p>
     *
     * @param maxColumnWidth 单列最宽像素；不大于 0 用默认上限
     */
    public void setNaturalColumnWidths(int maxColumnWidth) {
        naturalWidths = true;
        columnsFill = false;
        this.maxColumnWidth = maxColumnWidth;
        lastFitWidth = -1;
        refitColumns();
    }
    public void setAdditionalFilters(JComponent filters, FilterCondition condition) {
        toolbar.setAdditionalFilters(filters);
        additionalCondition = condition;
    }
    public void setItemKey(java.util.function.Function<T, Object> key) { itemKey = key; }
    public void setLiveSelectionUpdates(boolean enabled) { liveSelectionUpdates = enabled; }
    public void refreshCurrentPage() { load(page); }
    public void enableCheckboxSelection() {
        checkboxSelection = true;
        if (table.getColumnCount() > 0) table.getColumnModel().getColumn(0).setPreferredWidth(45);
        model.fireTableStructureChanged();
        configureTable();
    }
    public List<T> checkedItems() {
        List<T> result = new java.util.ArrayList<T>();
        for (int i = 0; i < items.size(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0))) result.add(items.get(i));
        }
        return result;
    }
    public List<T> selectedItems() {
        List<T> result = new java.util.ArrayList<T>();
        for (int row : table.getSelectedRows()) {
            int index = table.convertRowIndexToModel(row);
            if (index >= 0 && index < items.size()) result.add(items.get(index));
        }
        return result;
    }
    public void reload() { load(1); }
    public void resetFilters() {
        toolbar.getSearchField().setText("");
        if (toolbar.getFilterBox() != null && toolbar.getFilterBox().getSelectedIndex() != 0) {
            toolbar.getFilterBox().setSelectedIndex(0);
        } else {
            load(1);
        }
    }
    public T selectedItem() {
        int row = table.getSelectedRow();
        int index = row < 0 ? -1 : table.convertRowIndexToModel(row);
        return index < 0 || index >= items.size() ? null : items.get(index);
    }
    public int getPage() { return page; }
    public boolean isLoading() { return busy; }
    public JTable getTable() { return table; }
    public String getSearchKeyword() { return toolbar.getSearchField().getText(); }
    public String getSelectedFilter() { return toolbar.getFilterBox() == null ? "" : String.valueOf(toolbar.getFilterBox().getSelectedItem()); }

    private void load(final int targetPage) {
        load(targetPage, false);
    }

    private void load(final int targetPage, final boolean background) {
        if (targetPage < 1) return;
        page = targetPage; final int serial = ++requestSerial; setBusy(true, "正在更新…");
        final String keyword = toolbar.getSearchField().getText();
        final String filter = toolbar.getFilterBox() == null ? "" : String.valueOf(toolbar.getFilterBox().getSelectedItem());
        new SwingWorker<PageSlice<T>, Void>() {
            @Override protected PageSlice<T> doInBackground() throws Exception { return loader.load(targetPage, keyword, filter); }
            @Override protected void done() {
                if (serial != requestSerial) return;
                try {
                    PageSlice<T> result = get();
                    if (result.getItems().isEmpty() && targetPage > 1) {
                        load(targetPage - 1, background); return;
                    }
                    java.util.Set<Object> selectedKeys = new java.util.HashSet<Object>();
                    java.util.Set<Object> checkedKeys = new java.util.HashSet<Object>();
                    if (itemKey != null) {
                        for (T item : selectedItems()) selectedKeys.add(itemKey.apply(item));
                        for (T item : checkedItems()) checkedKeys.add(itemKey.apply(item));
                    }
                    replacingRows = true;
                    items = result.getItems(); page = result.getPage(); hasNext = result.hasNext();
                    model.setRowCount(0);
                    for (T item : items) {
                        Object[] values = mapper.values(item);
                        if (checkboxSelection && itemKey != null && values.length > 0) {
                            values[0] = checkedKeys.contains(itemKey.apply(item));
                        }
                        model.addRow(values);
                    }
                    for (int index = 0; index < items.size(); index++) {
                        if (itemKey != null && selectedKeys.contains(itemKey.apply(items.get(index)))) {
                            int view = table.convertRowIndexToView(index);
                            if (view >= 0) table.addRowSelectionInterval(view, view);
                        }
                    }
                    toolbar.setResultHint("共 " + result.getTotal() + " 条");
                    replacingRows = false;
                    if (!background || liveSelectionUpdates || (!selectedKeys.isEmpty() && selectedItem() == null)) selectedChanged(false);
                    if (items.isEmpty()) viewport.showEmpty(hasCondition(keyword, filter) ? "没有找到匹配记录" : "暂无记录", "");
                    else { viewport.showRows(items.size()); lastFitWidth = -1; refitColumns(); }
                    retry.setText("刷新"); setBusy(false, items.isEmpty() ? "当前页无记录" : "第 " + page + " 页");
                } catch (Exception ex) {
                    replacingRows = false;
                    if (!background) { table.clearSelection(); items = Collections.emptyList(); model.setRowCount(0); viewport.showError(); }
                    retry.setText("重试"); setBusy(false, "暂时无法更新，请重试");
                }
            }
        }.execute();
    }

    private boolean hasCondition(String keyword, String filter) {
        return keyword != null && keyword.trim().length() > 0
                || filter != null && filter.length() > 0 && !filter.startsWith("全部")
                || additionalCondition != null && additionalCondition.isActive();
    }
    private void selectedChanged(boolean adjusting) { if (!adjusting && !replacingRows && selectionListener != null) selectionListener.onSelected(selectedItem()); }
    private void setBusy(boolean busy, String text) {
        this.busy = busy;
        state.setText(text); previous.setEnabled(!busy && page > 1); next.setEnabled(!busy && hasNext); retry.setEnabled(!busy);
        toolbar.getSearchField().setEnabled(!busy); if (toolbar.getFilterBox() != null) toolbar.getFilterBox().setEnabled(!busy);
    }
    private DefaultTableModel model(String[] columns) {
        final String[] safe = columns == null ? new String[0] : columns.clone();
        return new DefaultTableModel(safe, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return checkboxSelection && column == 0;
            }
            @Override public Class<?> getColumnClass(int column) {
                return checkboxSelection && column == 0 ? Boolean.class : Object.class;
            }
        };
    }
    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setFillsViewportHeight(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(DesignTokens.regular(14));
        table.setSelectionBackground(DesignTokens.PRIMARY_LIGHT); table.setSelectionForeground(DesignTokens.TEXT_PRIMARY);
        DormTables.style(table);
        table.setDefaultRenderer(Object.class, DormTables.cellRenderer());
    }
}
