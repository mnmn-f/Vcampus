package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.InputLimiter;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/** 学生本人订单支付和商店管理员订单查询、详情及合法状态流转。 */
public final class StoreOrdersPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final Role role;
    private final Runnable orderChanged;
    private final JLabel detail = UiFactory.muted("");
    private final AsyncPagedTable<OrderDto> orders;
    private JButton payButton;
    private JButton cancelButton;
    private JButton managerCancelButton;
    private JButton refundButton;
    private final JComboBox<RealUi.CodeOption> shippingStatus = new JComboBox<RealUi.CodeOption>();
    private final JTextField tracking = UiFactory.textField(18);
    private final JTextField shippingRemark = UiFactory.textField(22);
    private final JButton saveShippingButton = new PrimaryButton("保存物流进度");
    private boolean paymentRunning;
    private boolean statusRunning;
    private boolean shippingRunning;
    private long selectedOrderId;

    public StoreOrdersPanel(BasePage page, StoreClientService service, Role role) {
        this(page, service, role, null);
    }

    public StoreOrdersPanel(BasePage page, StoreClientService service, Role role, Runnable orderChanged) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role; this.orderChanged = orderChanged;
        orders = table(); orders.setItemKey(OrderDto::getId); orders.setLiveSelectionUpdates(true); add(orders);
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
        if (role == Role.STORE_MANAGER) add(shippingEditor());
        updateManagerActions(null);
    }

    public void reload() { orders.reload(); }

    private AsyncPagedTable<OrderDto> table() {
        String title = role == Role.STORE_MANAGER ? "订单查询与处理" : "我的订单与支付";
        String subtitle = role == Role.STORE_MANAGER
                ? "查看订单并处理订单状态。"
                : "查看订单并完成支付。";
        AsyncPagedTable<OrderDto> table = new AsyncPagedTable<OrderDto>(title, subtitle,
                "搜索订单号", new String[]{"全部状态", "待支付", "已支付", "已取消", "已退款", "已完成"},
                new String[]{"订单号", "买家", "商品摘要", "金额", "订单状态", "物流状态", "创建时间"},
                new AsyncPagedTable.Loader<OrderDto>() {
                    @Override public PageSlice<OrderDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(role == Role.STORE_MANAGER ? service.searchOrders(new OrderQuery(keyword, null, status(filter), p, 20))
                                : service.getOwnOrders(new OrderQuery(keyword, null, status(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<OrderDto>() {
                    @Override public Object[] values(OrderDto row) { return new Object[]{RealUi.text(row.getOrderNo()), row.getBuyerId(), summary(row),
                            "¥" + RealUi.text(row.getTotalAmount()), RealUi.status(row.getStatus()), shipping(row), RealUi.dateTime(row.getCreatedAt())}; }
                }, new AsyncPagedTable.SelectionListener<OrderDto>() {
                    @Override public void onSelected(OrderDto row) { select(row); }
                });
        if (role == Role.STUDENT) {
            payButton = new PrimaryButton("支付选中订单"); payButton.setEnabled(false); payButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { pay(); }
            }); table.addAction(payButton);
            cancelButton = new DangerButton("取消订单"); cancelButton.setEnabled(false); cancelButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("CANCELLED", true); }
            }); table.addAction(cancelButton);
        } else if (role == Role.STORE_MANAGER) {
            managerCancelButton = new DangerButton("取消订单"); managerCancelButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("CANCELLED", true); }
            }); table.addAction(managerCancelButton);
            refundButton = new DangerButton("办理退款"); refundButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("REFUNDED", true); }
            }); table.addAction(refundButton);
        }
        return table;
    }

    private void select(final OrderDto value) {
        selectedOrderId = value == null ? 0L : value.getId();
        updateStudentActions(value); updateManagerActions(value); updateShippingEditor(value);
        if (value == null) { detail.setText(""); return; }
        detail.setText("订单详情：" + RealUi.text(value.getOrderNo()) + "　金额 ¥" + RealUi.text(value.getTotalAmount())
                + "　状态：" + RealUi.status(value.getStatus()) + "　物流：" + shippingDetail(value) + "　商品：" + summary(value));
        AsyncTask.run(new AsyncTask.Work<OrderDto>() {
            @Override public OrderDto run() throws Exception { return service.getOrderDetail(value.getId()); }
        }, new AsyncTask.Callback<OrderDto>() {
            @Override public void onSuccess(OrderDto result) { if (result == null || result.getId() != selectedOrderId) return; updateStudentActions(result); detail.setText("订单详情：" + RealUi.text(result.getOrderNo())
                    + "　金额 ¥" + RealUi.text(result.getTotalAmount()) + "　状态：" + RealUi.status(result.getStatus())
                    + "　物流：" + shippingDetail(result) + "　明细：" + summary(result)); }
            @Override public void onFailure(Throwable error) { if (selectedOrderId == value.getId()) page.showError(AsyncTask.message(error)); }
        });
    }

    private void pay() {
        final OrderDto value = orders.selectedItem(); if (value == null) { page.showWarning("请先选择订单。"); return; }
        if (!"CREATED".equals(value.getStatus())) { page.showWarning("只有待支付订单可以支付。"); updateStudentActions(value); return; }
        if (paymentRunning) return;
        if (!RealUi.confirm(this, "确认支付订单“" + RealUi.text(value.getOrderNo()) + "”？将从校园账户扣款。")) return;
        paymentRunning = true; updateStudentActions(value);
        final PaymentRequest request = new PaymentRequest(value.getId(), "desktop-order-pay-" + value.getId());
        AsyncTask.run(new AsyncTask.Work<OrderDto>() {
                    @Override public OrderDto run() throws Exception { return service.payOrder(request); }
                },
                new AsyncTask.Callback<OrderDto>() {
                    @Override public void onSuccess(OrderDto result) { paymentRunning = false; updateStudentActions(result); select(result); page.showSuccess("订单支付成功。"); orders.reload(); changed(); }
                    @Override public void onFailure(Throwable error) { paymentRunning = false; updateStudentActions(value); page.showError(AsyncTask.message(error)); }
                });
    }

    private void update(final String target, boolean confirm) {
        final OrderDto value = orders.selectedItem(); if (value == null) { page.showWarning("请先选择订单。"); return; }
        if (role == Role.STUDENT && !"CREATED".equals(value.getStatus())) { page.showWarning("只有待支付订单可以取消。"); updateStudentActions(value); return; }
        if (confirm && !RealUi.confirm(this, "确认将订单“" + RealUi.text(value.getOrderNo()) + "”处理为“" + RealUi.status(target) + "”？")) return;
        statusRunning = true; updateManagerActions(value);
        final OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(value.getId(), target);
        AsyncTask.run(new AsyncTask.Work<OrderDto>() {
                    @Override public OrderDto run() throws Exception { return service.updateOrderStatus(request); }
                },
                new AsyncTask.Callback<OrderDto>() {
                    @Override public void onSuccess(OrderDto result) { statusRunning = false; updateStudentActions(result); select(result); page.showSuccess("订单状态已更新。"); orders.reload(); changed(); }
                    @Override public void onFailure(Throwable error) { statusRunning = false; updateManagerActions(value); page.showError(AsyncTask.message(error)); }
                });
    }

    private void updateStudentActions(OrderDto value) {
        if (role != Role.STUDENT) return;
        boolean created = value != null && "CREATED".equals(value.getStatus());
        if (payButton != null) payButton.setEnabled(created && !paymentRunning);
        if (cancelButton != null) cancelButton.setEnabled(created && !paymentRunning);
    }

    private void changed() { if (orderChanged != null) orderChanged.run(); }

    private JPanel shippingEditor() {
        InputLimiter.code(tracking, 80); InputLimiter.length(shippingRemark, 500);
        saveShippingButton.addActionListener(e -> updateShipping());
        JPanel fields = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(220, 4, 10));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("物流状态", shippingStatus));
        fields.add(UiFactory.labelledField("物流单号（可选）", tracking));
        fields.add(UiFactory.labelledField("物流说明（可选）", shippingRemark));
        fields.add(UiFactory.formActionCell(saveShippingButton));
        SectionCard card = new SectionCard("物流进度", ""); card.setContent(fields); return card;
    }

    private void updateShipping() {
        final OrderDto value = orders.selectedItem(); if (value == null) { page.showWarning("请先选择订单。"); return; }
        if (!canUpdateShipping(value) || shippingRunning) { page.showWarning("当前订单无需更新物流。"); return; }
        shippingRunning = true; updateManagerActions(value);
        final OrderShippingUpdateRequest request = new OrderShippingUpdateRequest(value.getId(), RealUi.code(shippingStatus.getSelectedItem()),
                RealUi.optional(tracking.getText()), RealUi.optional(shippingRemark.getText()));
        AsyncTask.run(() -> service.updateOrderShipping(request), new AsyncTask.Callback<OrderDto>() {
            @Override public void onSuccess(OrderDto result) {
                shippingRunning = false; page.showSuccess("物流状态已更新。送达后订单会自动完成。");
                select(result); orders.reload(); changed();
            }
            @Override public void onFailure(Throwable error) {
                shippingRunning = false; updateManagerActions(value); page.showError(AsyncTask.message(error));
            }
        });
    }

    private void updateManagerActions(OrderDto value) {
        if (role != Role.STORE_MANAGER) return;
        String status = value == null ? "" : value.getStatus();
        boolean idle = !statusRunning && !shippingRunning;
        if (managerCancelButton != null) managerCancelButton.setEnabled(idle && "CREATED".equals(status));
        if (refundButton != null) refundButton.setEnabled(idle && ("PAID".equals(status) || "COMPLETED".equals(status)));
        boolean shipping = idle && canUpdateShipping(value);
        saveShippingButton.setEnabled(shipping); shippingStatus.setEnabled(shipping);
        tracking.setEnabled(shipping); shippingRemark.setEnabled(shipping);
    }

    private void updateShippingEditor(OrderDto value) {
        if (role != Role.STORE_MANAGER) return;
        String current = value == null || value.getShippingStatus() == null ? "PREPARING" : value.getShippingStatus();
        String[] flow = {"PREPARING", "SHIPPED", "IN_TRANSIT", "READY_FOR_PICKUP", "DELIVERED"};
        int start = 0;
        for (int index = 0; index < flow.length; index++) if (flow[index].equals(current)) start = index;
        shippingStatus.removeAllItems();
        for (int index = start; index < flow.length; index++) shippingStatus.addItem(RealUi.option(flow[index]));
        shippingStatus.setSelectedItem(RealUi.option(current));
        tracking.setText(value == null ? "" : RealUi.input(value.getTrackingNo()));
        shippingRemark.setText(value == null ? "" : RealUi.input(value.getShippingRemark()));
        updateManagerActions(value);
    }

    private static boolean canUpdateShipping(OrderDto value) {
        if (value == null || "DELIVERED".equals(value.getShippingStatus())) return false;
        return "PAID".equals(value.getStatus()) || "COMPLETED".equals(value.getStatus());
    }

    private static PageSlice<OrderDto> slice(OrderPage value) { return new PageSlice<OrderDto>(value.getItems(), value.getTotal(), value.getPage(), value.getPageSize()); }
    private static String status(String filter) { if ("待支付".equals(filter)) return "CREATED"; if ("已支付".equals(filter)) return "PAID"; if ("已取消".equals(filter)) return "CANCELLED"; if ("已退款".equals(filter)) return "REFUNDED"; if ("已完成".equals(filter)) return "COMPLETED"; return null; }
    private static String summary(OrderDto value) { if (value == null || value.getItems() == null || value.getItems().isEmpty()) return "--"; edu.seu.vcampus.common.dto.store.OrderItemDto item = value.getItems().get(0); String text = RealUi.text(item.getProductName()) + " ×" + item.getQuantity(); return value.getItems().size() > 1 ? text + " 等" : text; }
    private static String shipping(OrderDto value) { return value.getShippingStatus() == null ? "待发货" : RealUi.status(value.getShippingStatus()); }
    private static String shippingDetail(OrderDto value) { return shipping(value) + (value.getTrackingNo() == null ? "" : "（" + value.getTrackingNo() + "）")
            + (value.getShippingRemark() == null ? "" : " " + value.getShippingRemark()); }
}
