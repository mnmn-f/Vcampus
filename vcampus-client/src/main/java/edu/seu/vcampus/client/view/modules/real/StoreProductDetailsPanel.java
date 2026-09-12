package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import java.awt.BorderLayout;
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
    private final JTextArea reviewText = UiFactory.textArea(4, 25);
    private final AsyncPagedTable<ProductReviewDto> reviews;
    private long productId;
    private int serial;
    private boolean loading;
    StoreProductDetailsPanel(StoreClientService service, ProductDto product) {
        this(service, product, false, null);
    }
    StoreProductDetailsPanel(StoreClientService service, ProductDto product, boolean canPurchase, Runnable cartChanged) {
        super(new BorderLayout(8, 12)); setOpaque(false); this.service = service;
        image = new ProductImageView(180, 140, service);
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
        reviews = new AsyncPagedTable<>("用户评价", "", "搜索评价内容", new String[0],
                new String[]{"用户", "评分", "评价内容", "时间"},
                (page, keyword, filter) -> {
                    ProductReviewPage result = service.listReviews(new ProductReviewQuery(productId, page, 20, keyword));
                    return new PageSlice<>(result.getItems(), result.getTotal(), page, 20);
                }, row -> new Object[]{row.getReviewerName(), row.getScore() + " / 5", RealUi.text(row.getContent()), RealUi.dateTime(row.getCreatedAt())},
                row -> reviewText.setText(row == null ? "" : RealUi.text(row.getContent())));
        reviewText.setEditable(false); reviewText.setLineWrap(true); reviewText.setWrapStyleWord(true);
        reviews.setItemKey(ProductReviewDto::getId); add(reviews, BorderLayout.CENTER);
        reviews.getTable().setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] widths = {90, 65, 245, 160};
        for (int i = 0; i < widths.length; i++) reviews.getTable().getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        add(UiFactory.labelledField("评价全文", new javax.swing.JScrollPane(reviewText)), BorderLayout.SOUTH);
        edu.seu.vcampus.client.ui.TableInteractionPolicy.install(this);
        reload(); edu.seu.vcampus.client.ui.VisibleRefresh.attach(this, () -> !loading, this::reload);
    }
    void reload() {
        final int request = ++serial; loading = true;
        AsyncTask.run(() -> service.getProductDetail(productId), new AsyncTask.Callback<ProductDto>() {
            @Override public void onSuccess(ProductDto product) { if (request != serial) return; loading = false; render(product); state.setText(" "); }
            @Override public void onFailure(Throwable error) { if (request != serial) return; loading = false; state.setText(AsyncTask.message(error)); }
        });
    }
    private void render(ProductDto product) {
        title.setText(product.getName());
        summary.setText("¥" + product.getPrice() + "    库存 " + product.getStockQty() + "    评分 " + product.getRatingAverage() + "（" + product.getRatingCount() + " 条）");
        description.setText(RealUi.text(product.getDescription())); image.load(product.getImageUrl());
    }
}
