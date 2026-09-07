package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 从本人已完成订单中选择商品并提交一次评价。 */
public final class StoreReviewPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final JComboBox<Candidate> candidates = new JComboBox<Candidate>();
    private final JSpinner score = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
    private final JTextArea content = UiFactory.textArea(3, 30);
    private final JLabel state = UiFactory.muted("正在加载可评价商品…");
    private final AsyncPagedTable<ProductReviewDto> reviews;

    public StoreReviewPanel(BasePage page, StoreClientService service) {
        super("商品评价", "从已完成订单中选择商品，无需填写订单或商品编号。");
        this.page = page; this.service = service; reviews = reviewTable();
        candidates.addActionListener(e -> reviews.reload());
        JPanel form = new JPanel(new GridLayout(1, 3, 8, 8)); form.setOpaque(false);
        form.add(UiFactory.labelledField("已购买商品", candidates));
        form.add(UiFactory.labelledField("评分（1–5）", score));
        PrimaryButton submit = new PrimaryButton("提交评价"); submit.addActionListener(e -> save()); form.add(submit);
        JPanel editor = new JPanel(new BorderLayout(0, 8)); editor.setOpaque(false);
        editor.add(form, BorderLayout.NORTH); editor.add(UiFactory.labelledField("评价内容", content), BorderLayout.CENTER);
        editor.add(state, BorderLayout.SOUTH);
        JPanel body = UiFactory.vertical(10); body.add(editor); body.add(reviews); setContent(body); reloadCandidates();
    }

    public void reloadCandidates() {
        state.setText("正在加载可评价商品…");
        AsyncTask.run(() -> service.getOwnOrders(new OrderQuery(null, null, "COMPLETED", 1, 100)),
                new AsyncTask.Callback<OrderPage>() {
                    @Override public void onSuccess(OrderPage value) {
                        candidates.removeAllItems();
                        for (OrderDto order : value.getItems())
                            for (OrderItemDto item : order.getItems()) candidates.addItem(new Candidate(order, item));
                        state.setText(candidates.getItemCount() == 0 ? "暂无已完成订单商品。" : "请选择商品后评价。");
                        reviews.reload();
                    }
                    @Override public void onFailure(Throwable error) { state.setText("加载失败：" + AsyncTask.message(error)); }
                });
    }

    private AsyncPagedTable<ProductReviewDto> reviewTable() {
        return new AsyncPagedTable<ProductReviewDto>("商品评价记录", "显示当前所选商品的公开评价。", "", new String[0],
                new String[]{"商品", "评分", "内容", "时间"},
                (p, k, f) -> {
                    Candidate selected = (Candidate) candidates.getSelectedItem();
                    if (selected == null) return new PageSlice<ProductReviewDto>(null, 0, 1, 20);
                    ProductReviewPage value = service.listReviews(new ProductReviewQuery(selected.item.getProductId(), p, 20));
                    return new PageSlice<ProductReviewDto>(value.getItems(), value.getTotal(), p, 20);
                }, value -> new Object[]{value.getProductName(), value.getScore(), RealUi.text(value.getContent()),
                        RealUi.dateTime(value.getCreatedAt())}, null);
    }

    private void save() {
        final Candidate selected = (Candidate) candidates.getSelectedItem();
        if (selected == null) { page.showWarning("暂无可评价商品。"); return; }
        ProductReviewWriteRequest request = new ProductReviewWriteRequest(selected.order.getId(), selected.item.getProductId(),
                ((Number) score.getValue()).intValue(), RealUi.optional(content.getText()));
        AsyncTask.run(() -> service.addReview(request), new AsyncTask.Callback<ProductReviewDto>() {
            @Override public void onSuccess(ProductReviewDto value) {
                page.showSuccess("评价已提交。"); content.setText(""); reviews.reload(); reloadCandidates();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static final class Candidate {
        private final OrderDto order; private final OrderItemDto item;
        private Candidate(OrderDto order, OrderItemDto item) { this.order = order; this.item = item; }
        @Override public String toString() { return order.getOrderNo() + " · " + item.getProductName(); }
    }
}
