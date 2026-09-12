package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
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
import javax.swing.JOptionPane;
import java.awt.GridLayout;
import java.awt.BorderLayout;

/** 学生本人订单支付和商店管理员订单查询、详情及合法状态流转。 */
public final class StoreOrdersPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final Role role;
    private final Runnable orderChanged;
    private final JLabel detail = UiFactory.muted("选择订单查看详情。");
    private final AsyncPagedTable<OrderDto> orders;
    private JButton payButton;
    private JButton cancelButton;
    private boolean paymentRunning;
    private long selectedOrderId;

    public StoreOrdersPanel(BasePage page, StoreClientService service, Role role) {
        this(page, service, role, null);
    }

    public StoreOrdersPanel(BasePage page, StoreClientService service, Role role, Runnable orderChanged) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role; this.orderChanged = orderChanged;
        orders = table(); orders.setItemKey(OrderDto::getId); orders.setLiveSelectionUpdates(true); add(orders);
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
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
            JButton complete = new PrimaryButton("标记已完成"); complete.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("COMPLETED", false); }
            }); table.addAction(complete);
            JButton cancel = new DangerButton("取消订单"); cancel.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("CANCELLED", true); }
            }); table.addAction(cancel);
            JButton refund = new DangerButton("办理退款"); refund.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { update("REFUNDED", true); }
            }); table.addAction(refund);
            JButton shipping = new PrimaryButton("更新物流"); shipping.addActionListener(e -> updateShipping()); table.addAction(shipping);
        }
        return table;
    }

    private void select(final OrderDto value) {
        selectedOrderId = value == null ? 0L : value.getId();
        updateStudentActions(value);
        if (value == null) { detail.setText("选择订单查看详情。"); return; }
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
        final OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(value.getId(), target);
        AsyncTask.run(new AsyncTask.Work<OrderDto>() {
                    @Override public OrderDto run() throws Exception { return service.updateOrderStatus(request); }
                },
                new AsyncTask.Callback<OrderDto>() {
                    @Override public void onSuccess(OrderDto result) { updateStudentActions(result); select(result); page.showSuccess("订单状态已更新。"); orders.reload(); changed(); }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private void updateStudentActions(OrderDto value) {
        if (role != Role.STUDENT) return;
        boolean created = value != null && "CREATED".equals(value.getStatus());
        if (payButton != null) payButton.setEnabled(created && !paymentRunning);
        if (cancelButton != null) cancelButton.setEnabled(created && !paymentRunning);
    }

    private void changed() { if (orderChanged != null) orderChanged.run(); }

    private void updateShipping() {
        final OrderDto value = orders.selectedItem(); if (value == null) { page.showWarning("请先选择订单。"); return; }
        JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(RealUi.options(
                "PREPARING", "SHIPPED", "IN_TRANSIT", "READY_FOR_PICKUP", "DELIVERED"));
        status.setSelectedItem(RealUi.option(value.getShippingStatus() == null ? "PREPARING" : value.getShippingStatus()));
        JTextField tracking = UiFactory.textField(18); tracking.setText(RealUi.input(value.getTrackingNo()));
        JTextField remark = UiFactory.textField(22); remark.setText(RealUi.input(value.getShippingRemark()));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 8)); fields.add(UiFactory.labelledField("物流状态", status));
        fields.add(UiFactory.labelledField("物流单号（可选）", tracking)); fields.add(UiFactory.labelledField("物流说明（可选）", remark));
        if (JOptionPane.showConfirmDialog(this, fields, "更新订单物流", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        final OrderShippingUpdateRequest request = new OrderShippingUpdateRequest(value.getId(), RealUi.code(status.getSelectedItem()),
                RealUi.optional(tracking.getText()), RealUi.optional(remark.getText()));
        AsyncTask.run(() -> service.updateOrderShipping(request), new AsyncTask.Callback<OrderDto>() {
            @Override public void onSuccess(OrderDto result) { page.showSuccess("物流状态已更新。"); select(result); orders.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static PageSlice<OrderDto> slice(OrderPage value) { return new PageSlice<OrderDto>(value.getItems(), value.getTotal(), value.getPage(), value.getPageSize()); }
    private static String status(String filter) { if ("待支付".equals(filter)) return "CREATED"; if ("已支付".equals(filter)) return "PAID"; if ("已取消".equals(filter)) return "CANCELLED"; if ("已退款".equals(filter)) return "REFUNDED"; if ("已完成".equals(filter)) return "COMPLETED"; return null; }
    private static String summary(OrderDto value) { if (value == null || value.getItems() == null || value.getItems().isEmpty()) return "--"; edu.seu.vcampus.common.dto.store.OrderItemDto item = value.getItems().get(0); String text = RealUi.text(item.getProductName()) + " ×" + item.getQuantity(); return value.getItems().size() > 1 ? text + " 等" : text; }
    private static String shipping(OrderDto value) { return value.getShippingStatus() == null ? "待发货" : RealUi.status(value.getShippingStatus()); }
    private static String shippingDetail(OrderDto value) { return shipping(value) + (value.getTrackingNo() == null ? "" : "（" + value.getTrackingNo() + "）")
            + (value.getShippingRemark() == null ? "" : " " + value.getShippingRemark()); }
}
