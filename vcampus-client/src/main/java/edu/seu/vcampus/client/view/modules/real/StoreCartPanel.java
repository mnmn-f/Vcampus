package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** 学生购物车：异步加载，支持数量更新、删除和事务建单。 */
public final class StoreCartPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final Runnable orderCreated;
    private final DefaultTableModel model = model();
    private final JTable table;
    private final JLabel state = UiFactory.muted("尚未加载");
    private final JLabel total = UiFactory.body("合计：¥0.00");
    private final JTextField quantity = UiFactory.textField(5);
    private final JButton update = new SecondaryButton("更新数量");
    private final JButton remove = new SecondaryButton("移除商品");
    private final JButton createOrder = new PrimaryButton("提交订单");
    private List<CartItemDto> items = Collections.emptyList();

    public StoreCartPanel(BasePage page, StoreClientService service, Runnable orderCreated) {
        super("我的购物车", "提交订单后到订单区支付。");
        this.page = page; this.service = service; this.orderCreated = orderCreated;
        table = table(model);
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) { selectedChanged(e.getValueIsAdjusting()); }
        });
        update.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateQuantity(); }
        }); remove.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { removeItem(); }
        });
        createOrder.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { createOrder(); }
        });
        JPanel actions = UiFactory.horizontal(8);
        actions.add(UiFactory.body("数量")); actions.add(quantity); actions.add(update); actions.add(remove);
        actions.add(total); actions.add(createOrder);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(actions, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setPreferredSize(new Dimension(800, 220)); scroll.setMinimumSize(new Dimension(0, 120));
        content.add(scroll, BorderLayout.CENTER); content.add(state, BorderLayout.SOUTH);
        setContent(content); load();
    }

    public void reload() { load(); }

    private void load() {
        setBusy(true, "正在加载…");
        AsyncTask.run(new AsyncTask.Work<CartDto>() {
            @Override public CartDto run() throws Exception { return service.getCart(); }
        }, new AsyncTask.Callback<CartDto>() {
            @Override public void onSuccess(CartDto value) { render(value); setBusy(false, items.isEmpty() ? "购物车为空" : "已加载"); }
            @Override public void onFailure(Throwable error) { clear(); setBusy(false, "加载失败：" + AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
        });
    }

    private void render(CartDto value) {
        items = value == null ? Collections.<CartItemDto>emptyList() : value.getItems();
        model.setRowCount(0);
        for (CartItemDto item : items) model.addRow(new Object[]{RealUi.text(item.getProductName()),
                money(item.getUnitPrice()), item.getQuantity(), money(item.getLineAmount()),
                item.getStockQty(), RealUi.status(item.getProductStatus())});
        total.setText("合计：¥" + moneyValue(value == null ? BigDecimal.ZERO : value.getTotalAmount()));
        quantity.setText(items.isEmpty() ? "" : String.valueOf(items.get(0).getQuantity()));
    }

    private void updateQuantity() {
        final CartItemDto value = selected(); if (value == null) { page.showWarning("请先选择购物车商品。"); return; }
        try {
            int count = Integer.parseInt(RealUi.required(quantity.getText(), "数量"));
            if (count < 1) throw new IllegalArgumentException("数量必须大于 0");
            change(new CartItemRequest(value.getProductId(), count), false);
        } catch (NumberFormatException ex) { page.showWarning("数量必须是整数。"); }
        catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private void removeItem() {
        final CartItemDto value = selected(); if (value == null) { page.showWarning("请先选择购物车商品。"); return; }
        AsyncTask.run(new AsyncTask.Work<CartDto>() {
            @Override public CartDto run() throws Exception { return service.removeCartItem(value.getProductId()); }
        }, new AsyncTask.Callback<CartDto>() {
            @Override public void onSuccess(CartDto result) { page.showSuccess("商品已移出购物车。"); render(result); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void change(final CartItemRequest request, final boolean add) {
        AsyncTask.run(new AsyncTask.Work<CartDto>() {
                    @Override public CartDto run() throws Exception { return add ? service.addCartItem(request) : service.updateCartItem(request); }
                },
                new AsyncTask.Callback<CartDto>() {
                    @Override public void onSuccess(CartDto result) { page.showSuccess("购物车已更新。"); render(result); }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private void createOrder() {
        if (items.isEmpty()) { page.showWarning("购物车为空，暂不能提交订单。"); return; }
        AsyncTask.run(new AsyncTask.Work<OrderDto>() {
            @Override public OrderDto run() throws Exception { return service.createOrder(); }
        }, new AsyncTask.Callback<OrderDto>() {
            @Override public void onSuccess(OrderDto result) { page.showSuccess("订单已创建：" + RealUi.text(result.getOrderNo())); load(); if (orderCreated != null) orderCreated.run(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private CartItemDto selected() { int row = table.getSelectedRow(); return row < 0 || row >= items.size() ? null : items.get(row); }
    private void selectedChanged(boolean adjusting) { if (!adjusting && selected() != null) quantity.setText(String.valueOf(selected().getQuantity())); }
    private void clear() { items = Collections.emptyList(); model.setRowCount(0); total.setText("合计：¥0.00"); quantity.setText(""); }
    private void setBusy(boolean busy, String text) { state.setText(text); update.setEnabled(!busy); remove.setEnabled(!busy); createOrder.setEnabled(!busy); quantity.setEnabled(!busy); }
    private static DefaultTableModel model() { return new DefaultTableModel(new String[]{"商品", "单价", "数量", "小计", "库存", "状态"}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } }; }
    private static JTable table(DefaultTableModel source) {
        JTable value = new JTable(source) { @Override public String getToolTipText(MouseEvent event) { int row = rowAtPoint(event.getPoint()); int col = columnAtPoint(event.getPoint()); return row < 0 || col < 0 ? null : String.valueOf(getValueAt(row, col)); } };
        value.setToolTipText(""); value.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); value.setRowHeight(38); value.setFillsViewportHeight(true); value.setShowGrid(false); value.setIntercellSpacing(new Dimension(0, 1)); value.setFont(DesignTokens.regular(13)); value.setForeground(DesignTokens.TEXT_PRIMARY); value.setBackground(Color.WHITE); value.getTableHeader().setFont(DesignTokens.medium(13)); value.getTableHeader().setForeground(DesignTokens.TEXT_PRIMARY); value.getTableHeader().setBackground(new Color(0xF0, 0xF4, 0xF2)); value.getTableHeader().setPreferredSize(new Dimension(0, 36)); value.getTableHeader().setMinimumSize(new Dimension(0, 36)); value.getTableHeader().setOpaque(true); value.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER)); value.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() { @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { java.awt.Component component = super.getTableCellRendererComponent(t, v, s, f, r, c); setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); if (!s) component.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFC, 0xFB)); return component; } }); return value;
    }
    private static String money(BigDecimal value) { return value == null ? "--" : "¥" + moneyValue(value); }
    private static String moneyValue(BigDecimal value) { return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
}
