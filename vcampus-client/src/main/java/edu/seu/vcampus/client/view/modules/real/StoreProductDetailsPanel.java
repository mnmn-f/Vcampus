package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** 未购买的学生也能查看商品信息和全部公开评价。 */
final class StoreProductDetailsPanel extends JPanel {
    private final StoreClientService service;
    private final ProductImageView image;
    private final JLabel title = UiFactory.sectionTitle("商品详情");
    private final JLabel summary = UiFactory.body(" ");
    private final JLabel state = UiFactory.muted(" ");
    private final JTextArea description = UiFactory.textArea(3, 25);
    private final JPanel reviewList = UiFactory.vertical(0);
    private final JLabel reviewTitle = UiFactory.sectionTitle("用户评价");
    private final JLabel reviewState = UiFactory.muted("正在加载评价…");
    private long productId;
    private int productSerial;
    private int reviewSerial;
    private boolean productLoading;
    private boolean reviewsLoading;
    StoreProductDetailsPanel(StoreClientService service, ProductDto product) {
        this(service, product, false, null);
    }
    StoreProductDetailsPanel(StoreClientService service, ProductDto product, boolean canPurchase, Runnable cartChanged) {
        super(new BorderLayout(8, 12)); setOpaque(false); this.service = service;
        image = new ProductImageView(180, 140, service);
        image.enableFullPreview();
        description.setEditable(false); description.setLineWrap(true); description.setWrapStyleWord(true);
        JPanel text = UiFactory.vertical(8); text.add(title); text.add(summary); text.add(description); text.add(state);
        if (canPurchase) {
            edu.seu.vcampus.client.ui.components.PrimaryButton add = new edu.seu.vcampus.client.ui.components.PrimaryButton("加入购物车");
            add.addActionListener(e -> {
                add.setEnabled(false); state.setText("正在加入购物车…");
                AsyncTask.run(() -> service.addCartItem(new edu.seu.vcampus.common.dto.store.CartItemRequest(productId, 1)), new AsyncTask.Callback<edu.seu.vcampus.common.dto.store.CartDto>() {
                    @Override public void onSuccess(edu.seu.vcampus.common.dto.store.CartDto cart) { add.setEnabled(true); state.setText("已加入购物车"); if (cartChanged != null) cartChanged.run(); }
                    @Override public void onFailure(Throwable error) { add.setEnabled(true); state.setText(AsyncTask.message(error)); }
                });
            });
            JPanel actions = UiFactory.horizontal(6); actions.add(add); text.add(actions);
        }
        JPanel header = new JPanel(new BorderLayout(12, 8)); header.setOpaque(false); header.add(image, BorderLayout.WEST); header.add(text, BorderLayout.CENTER);
        SectionCard information = new SectionCard("商品信息", ""); information.setContent(header); add(information, BorderLayout.NORTH);
        productId = product.getId(); render(product);
        JPanel reviewSection = UiFactory.vertical(8);
        reviewSection.setBorder(BorderFactory.createEmptyBorder(4, 18, 12, 18));
        reviewTitle.setAlignmentX(LEFT_ALIGNMENT); reviewState.setAlignmentX(LEFT_ALIGNMENT);
        reviewList.setAlignmentX(LEFT_ALIGNMENT);
        reviewSection.add(reviewTitle);
        reviewSection.add(reviewState);
        reviewSection.add(reviewList);
        add(reviewSection, BorderLayout.CENTER);
        edu.seu.vcampus.client.ui.TableInteractionPolicy.install(this);
        reload(); edu.seu.vcampus.client.ui.VisibleRefresh.attach(this,
                () -> !productLoading && !reviewsLoading, this::reload);
    }
    void reload() {
        loadProduct(); loadReviews();
    }
    private void loadProduct() {
        final int request = ++productSerial; productLoading = true;
        AsyncTask.run(() -> service.getProductDetail(productId), new AsyncTask.Callback<ProductDto>() {
            @Override public void onSuccess(ProductDto product) { if (request != productSerial) return; productLoading = false; render(product); state.setText(" "); }
            @Override public void onFailure(Throwable error) { if (request != productSerial) return; productLoading = false; state.setText(AsyncTask.message(error)); }
        });
    }
    private void loadReviews() {
        final int request = ++reviewSerial; reviewsLoading = true;
        AsyncTask.run(() -> service.listReviews(new ProductReviewQuery(productId, 1, 100)),
                new AsyncTask.Callback<ProductReviewPage>() {
                    @Override public void onSuccess(ProductReviewPage value) {
                        if (request != reviewSerial) return;
                        reviewsLoading = false;
                        renderReviews(value == null ? Collections.emptyList() : value.getItems());
                    }
                    @Override public void onFailure(Throwable error) {
                        if (request != reviewSerial) return;
                        reviewsLoading = false; reviewState.setText(AsyncTask.message(error));
                    }
                });
    }
    private void renderReviews(List<ProductReviewDto> values) {
        reviewList.removeAll();
        if (values == null || values.isEmpty()) {
            reviewTitle.setText("用户评价"); reviewState.setText("暂无评价");
        }
        else {
            reviewTitle.setText("用户评价（" + values.size() + "）"); reviewState.setText(" ");
            for (ProductReviewDto value : values) reviewList.add(review(value));
        }
        reviewList.revalidate(); reviewList.repaint();
    }
    private static JPanel review(ProductReviewDto value) {
        JPanel item = new JPanel(new BorderLayout(12, 5)); item.setOpaque(false);
        item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT));
        JPanel heading = UiFactory.horizontal(10);
        heading.add(UiFactory.body(RealUi.text(value.getReviewerName())));
        heading.add(UiFactory.body(value.getScore() + " / 5"));
        item.add(heading, BorderLayout.NORTH);
        JTextArea content = new JTextArea(RealUi.text(value.getContent()));
        content.setEditable(false); content.setFocusable(false); content.setOpaque(false);
        content.setLineWrap(true); content.setWrapStyleWord(true);
        content.setFont(DesignTokens.regular(14)); content.setForeground(DesignTokens.TEXT_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(5, 0, 12, 0));
        item.add(content, BorderLayout.CENTER);
        item.add(UiFactory.muted(RealUi.dateTime(value.getCreatedAt())), BorderLayout.EAST);
        return item;
    }
    private void render(ProductDto product) {
        title.setText(product.getName());
        summary.setText("¥" + product.getPrice() + "    库存 " + product.getStockQty() + "    评分 " + product.getRatingAverage() + "（" + product.getRatingCount() + " 条）");
        description.setText(RealUi.text(product.getDescription())); image.load(product.getImageUrl());
    }
}
