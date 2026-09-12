package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.ReviewCandidateDto;
import edu.seu.vcampus.common.dto.store.ReviewCandidatePage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

/** 待评价商品使用服务端分页，已评价明细提交后退出列表。 */
public final class StoreReviewPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final AsyncPagedTable<ReviewCandidateDto> candidates;
    private final JSpinner score = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
    private final JTextArea content = UiFactory.textArea(3, 30);
    private final JLabel selected = UiFactory.body("请选择待评价商品");
    private final JLabel state = UiFactory.muted(" ");
    private final PrimaryButton submit = new PrimaryButton("提交评价");
    private ReviewCandidateDto current;
    private boolean submitting;

    public StoreReviewPanel(BasePage page, StoreClientService service) {
        super("商品评价", ""); this.page = page; this.service = service;
        candidates = new AsyncPagedTable<>("待评价商品", "", "商品名称或订单号", new String[0],
                new String[]{"订单号", "商品", "数量"},
                (p, keyword, filter) -> {
                    ReviewCandidatePage result = service.reviewCandidates(new ProductReviewQuery(0, p, 20, keyword));
                    return new PageSlice<>(result.getItems(), result.getTotal(), p, 20);
                }, value -> new Object[]{value.getOrderNo(), value.getProductName(), value.getQuantity()}, this::select);
        candidates.setItemKey(value -> value.getOrderId() + ":" + value.getProductId());
        candidates.getTable().setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] widths = {240, 260, 70};
        for (int i = 0; i < widths.length; i++) candidates.getTable().getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        submit.setEnabled(false); submit.addActionListener(e -> save());
        JPanel actions = UiFactory.horizontal(8); actions.add(UiFactory.labelledField("评分（1–5）", score)); actions.add(submit); actions.add(state);
        JPanel editor = UiFactory.vertical(8); editor.add(selected); editor.add(UiFactory.labelledField("评价内容", content)); editor.add(actions);
        JPanel body = UiFactory.vertical(12); body.add(candidates); body.add(editor); setContent(body);
    }
    public void reloadCandidates() { candidates.refreshCurrentPage(); }
    private void select(ReviewCandidateDto value) {
        boolean changed = current == null || value == null || current.getOrderId() != value.getOrderId() || current.getProductId() != value.getProductId();
        current = value; selected.setText(value == null ? "请选择待评价商品" : value.getOrderNo() + " · " + value.getProductName());
        if (changed && !submitting) { content.setText(""); score.setValue(5); state.setText(" "); }
        submit.setEnabled(value != null && !submitting);
    }
    private void save() {
        final ReviewCandidateDto item = current;
        if (item == null || submitting) return;
        String text = RealUi.optional(content.getText());
        if (text != null && text.length() > 1000) { state.setText("评价内容不能超过 1000 字"); return; }
        ProductReviewWriteRequest request = new ProductReviewWriteRequest(item.getOrderId(), item.getProductId(), ((Number) score.getValue()).intValue(), text);
        submitting = true; submit.setEnabled(false); content.setEnabled(false); score.setEnabled(false);
        AsyncTask.run(() -> service.addReview(request), new AsyncTask.Callback<ProductReviewDto>() {
            @Override public void onSuccess(ProductReviewDto value) {
                finish(); content.setText(""); state.setText("评价已提交"); page.showSuccess("评价已提交"); candidates.refreshCurrentPage();
            }
            @Override public void onFailure(Throwable error) { finish(); state.setText(AsyncTask.message(error)); candidates.refreshCurrentPage(); }
        });
    }
    private void finish() { submitting = false; content.setEnabled(true); score.setEnabled(true); submit.setEnabled(false); }
}
