package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

/** 列表页面通用的筛选、搜索和操作工具栏。 */
public class DataTableToolbar extends JPanel {
    private boolean responsive;
    private JPanel controls;
    private JPanel left;
    private final JTextField searchField = UiFactory.textField(18);
    private final JComboBox<String> filterBox;
    private final JLabel resultHint = UiFactory.muted("");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel actionRow = new JPanel(new BorderLayout());
    private boolean actionRowAdded;

    public DataTableToolbar(String searchHint, String[] filters) {
        super(new BorderLayout(0, 2));
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 2, 0));
        searchField.setToolTipText(searchHint);
        searchField.putClientProperty("JTextField.placeholderText", searchHint);
        left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(label("搜索"));
        left.add(searchField);
        if (filters != null && filters.length > 0) {
            filterBox = new JComboBox<String>(filters);
            filterBox.setFont(DesignTokens.regular(13));
            filterBox.setPreferredSize(new Dimension(128, 36));
            left.add(label(filterLabel(filters)));
            left.add(filterBox);
        } else {
            filterBox = null;
        }
        controls = new JPanel(new BorderLayout(DesignTokens.SPACE_12, 0));
        controls.setOpaque(false);
        controls.add(left, BorderLayout.WEST);
        controls.add(resultHint, BorderLayout.CENTER);
        actions.setOpaque(false);
        actions.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        actionRow.setOpaque(false);
        // 占满第二行后再在内部排列按钮；这样窄窗口不会把最后一个操作挤出可视区域。
        actionRow.add(actions, BorderLayout.CENTER);
        add(controls, BorderLayout.NORTH);
    }

    public void enableResponsiveLayout() {
        if (responsive) return; responsive = true;
        javax.swing.JButton search = new SecondaryButton("搜索");
        search.addActionListener(event -> searchField.postActionEvent()); left.add(search);
        controls.remove(left); controls.remove(resultHint);
        controls.add(left, BorderLayout.CENTER); controls.add(resultHint, BorderLayout.SOUTH);
        left.setLayout(new edu.seu.vcampus.client.ui.WrapLayout(6));
        searchField.setColumns(14);
        actions.setLayout(new edu.seu.vcampus.client.ui.WrapLayout(6));
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox<String> getFilterBox() {
        return filterBox;
    }

    public void addAction(javax.swing.JButton button) {
        actions.add(button);
        if (!actionRowAdded) {
            add(actionRow, BorderLayout.SOUTH);
            actionRowAdded = true;
        }
        actionRow.revalidate();
        revalidate();
        repaint();
    }

    public void setAdditionalFilters(JComponent filters) {
        if (filters == null) return;
        add(filters, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void setResultHint(String text) {
        resultHint.setText(text == null ? "" : text);
    }

    public void onSearch(ActionListener listener) {
        searchField.addActionListener(listener);
    }

    private JLabel label(String text) {
        JLabel value = UiFactory.body(text);
        value.setFont(DesignTokens.medium(13));
        return value;
    }

    private String filterLabel(String[] filters) {
        String first = filters[0] == null ? "" : filters[0].trim();
        if (first.startsWith("全部") && first.length() > 2) return first.substring(2);
        return "筛选";
    }
}
