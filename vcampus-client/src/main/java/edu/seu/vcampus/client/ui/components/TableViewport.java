package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Dimension;

/** 表格内容区：按记录数收缩，避免少量数据占满大片空白。 */
public final class TableViewport extends JPanel {
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";
    private final CardLayout cards = new CardLayout();
    private final JScrollPane tableScroll;
    private final JTable table;
    private final EmptyStatePanel empty = new EmptyStatePanel("暂无记录", "", null);

    public TableViewport(JTable table) {
        super();
        setLayout(cards);
        setOpaque(false);
        this.table = table;
        tableScroll = new JScrollPane(table);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER_LIGHT));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setPreferredSize(new Dimension(800, 84));
        add(tableScroll, TABLE);
        add(empty, EMPTY);
        showLoading();
    }

    public void showRows(int count) {
        int visible = Math.max(1, Math.min(8, count));
        int tableHeight = 38 + Math.max(1, count) * 38;
        table.setPreferredScrollableViewportSize(new Dimension(800, tableHeight));
        tableScroll.setPreferredSize(new Dimension(800, 38 + visible * 38));
        cards.show(this, TABLE);
        revalidate();
    }

    public void showEmpty(String title, String reason) {
        empty.setCopy(title, reason);
        cards.show(this, EMPTY);
        revalidate();
    }

    public void showLoading() {
        showEmpty("正在更新", "");
    }

    public void showError() {
        showEmpty("暂时无法显示", "请检查连接后点击“重试”。");
    }
}
