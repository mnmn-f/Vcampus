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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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
    private int page = 1;
    private int requestSerial;
    private boolean hasNext;
    private FilterCondition additionalCondition;

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
        content.add(viewport, BorderLayout.CENTER); content.add(footer, BorderLayout.SOUTH); setContent(content); load(1);
    }

    public void addAction(JButton button) { toolbar.addAction(button); }
    public void setAdditionalFilters(JComponent filters, FilterCondition condition) {
        toolbar.setAdditionalFilters(filters);
        additionalCondition = condition;
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
    public T selectedItem() { int row = table.getSelectedRow(); return row < 0 || row >= items.size() ? null : items.get(row); }
    public int getPage() { return page; }
    public JTable getTable() { return table; }
    public String getSearchKeyword() { return toolbar.getSearchField().getText(); }
    public String getSelectedFilter() { return toolbar.getFilterBox() == null ? "" : String.valueOf(toolbar.getFilterBox().getSelectedItem()); }

    private void load(final int targetPage) {
        if (targetPage < 1) return;
        page = targetPage; final int serial = ++requestSerial; setBusy(true, "正在更新…");
        final String keyword = toolbar.getSearchField().getText();
        final String filter = toolbar.getFilterBox() == null ? "" : String.valueOf(toolbar.getFilterBox().getSelectedItem());
        new SwingWorker<PageSlice<T>, Void>() {
            @Override protected PageSlice<T> doInBackground() throws Exception { return loader.load(targetPage, keyword, filter); }
            @Override protected void done() {
                if (serial != requestSerial) return;
                try {
                    PageSlice<T> result = get(); items = result.getItems(); page = result.getPage(); hasNext = result.hasNext();
                    model.setRowCount(0); for (T item : items) model.addRow(mapper.values(item));
                    toolbar.setResultHint("共 " + result.getTotal() + " 条");
                    if (items.isEmpty()) viewport.showEmpty(hasCondition(keyword, filter) ? "没有找到匹配记录" : "暂无记录", "");
                    else viewport.showRows(items.size());
                    retry.setText("刷新"); setBusy(false, items.isEmpty() ? "当前页无记录" : "第 " + page + " 页");
                } catch (Exception ex) { viewport.showError(); retry.setText("重试"); setBusy(false, "暂时无法更新"); }
            }
        }.execute();
    }

    private boolean hasCondition(String keyword, String filter) {
        return keyword != null && keyword.trim().length() > 0
                || filter != null && filter.length() > 0 && !filter.startsWith("全部")
                || additionalCondition != null && additionalCondition.isActive();
    }
    private void selectedChanged(boolean adjusting) { if (!adjusting && selectionListener != null) selectionListener.onSelected(selectedItem()); }
    private void setBusy(boolean busy, String text) {
        state.setText(text); previous.setEnabled(!busy && page > 1); next.setEnabled(!busy && hasNext); retry.setEnabled(!busy);
        toolbar.getSearchField().setEnabled(!busy); if (toolbar.getFilterBox() != null) toolbar.getFilterBox().setEnabled(!busy);
    }
    private DefaultTableModel model(String[] columns) {
        final String[] safe = columns == null ? new String[0] : columns.clone();
        return new DefaultTableModel(safe, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }
    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(38); table.setFillsViewportHeight(false);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 1)); table.setFont(DesignTokens.regular(13));
        table.setForeground(DesignTokens.TEXT_PRIMARY); table.setBackground(Color.WHITE); table.setSelectionBackground(DesignTokens.PRIMARY_LIGHT);
        table.setSelectionForeground(DesignTokens.TEXT_PRIMARY); JTableHeader header = table.getTableHeader();
        header.setFont(DesignTokens.medium(13)); header.setForeground(DesignTokens.TEXT_PRIMARY); header.setBackground(new Color(0xF0, 0xF4, 0xF2));
        header.setPreferredSize(new Dimension(0, 36)); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean selected, boolean focus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(t, v, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); if (!selected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFC, 0xFB)); return c;
            }
        });
    }
}
