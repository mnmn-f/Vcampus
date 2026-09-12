package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;

import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 商店管理员维护促销规则。 */
public final class StorePromotionPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final JTextField code = UiFactory.textField(8);
    private final JTextField name = UiFactory.textField(10);
    private final JTextField value = UiFactory.textField(6);
    private final JTextField threshold = UiFactory.textField(6);
    private final JComboBox<StoreProductOption> product = new JComboBox<StoreProductOption>();
    private final JComboBox<StoreCategoryOption> category = new JComboBox<StoreCategoryOption>();
    private final JComboBox<RealUi.CodeOption> type = new JComboBox<RealUi.CodeOption>(
            RealUi.options("THRESHOLD", "PERCENT", "FIXED"));
    private final JComboBox<RealUi.CodeOption> scope = new JComboBox<RealUi.CodeOption>(
            RealUi.options("ALL", "PRODUCT", "CATEGORY"));
    private final JComboBox<RealUi.CodeOption> active = new JComboBox<RealUi.CodeOption>(
            RealUi.options("ACTIVE", "INACTIVE"));
    private final JLabel state = UiFactory.muted(" ");
    private final AsyncPagedTable<PromotionDto> table;
    private long selectedId;
    private Long pendingProductId;
    private String pendingCategoryCode;

    public StorePromotionPanel(BasePage page, StoreClientService service) {
        super();
        this.page = page; this.service = service; setOpaque(false);
        table = promotionTable();
        JPanel fields = new JPanel(new GridLayout(0, 4, 8, 8)); fields.setOpaque(false);
        add(fields, "编码", code); add(fields, "名称", name); add(fields, "类型", type);
        add(fields, "优惠值", value); add(fields, "门槛", threshold); add(fields, "范围", scope);
        add(fields, "指定商品", product); add(fields, "指定分类", category); add(fields, "状态", active);
        JButton save = new PrimaryButton("保存促销");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        fields.add(save); fields.add(state);
        scope.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateTargetState(); }
        });
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); add(table); add(fields);
        updateTargetState(); loadCategories(); loadProducts();
    }

    private AsyncPagedTable<PromotionDto> promotionTable() {
        return new AsyncPagedTable<PromotionDto>("促销优惠", "", "促销关键字", new String[0],
                new String[]{"编码", "名称", "类型", "优惠值", "范围", "状态"},
                new AsyncPagedTable.Loader<PromotionDto>() {
                    @Override public PageSlice<PromotionDto> load(int p, String k, String f) throws Exception {
                        PromotionPage value = service.listPromotions();
                        return new PageSlice<PromotionDto>(value == null ? null : value.getItems(),
                                value == null ? 0L : value.getTotal(), 1, 100);
                    }
                }, new AsyncPagedTable.RowMapper<PromotionDto>() {
                    @Override public Object[] values(PromotionDto value) {
                        return new Object[]{value.getCode(), value.getName(), RealUi.status(value.getType()),
                                value.getValue(), RealUi.status(value.getProductScope()), value.isActive() ? "启用" : "停用"};
                    }
                }, new AsyncPagedTable.SelectionListener<PromotionDto>() {
                    @Override public void onSelected(PromotionDto value) { showPromotion(value); }
                });
    }

    private void loadCategories() {
        AsyncTask.run(new AsyncTask.Work<StoreCategoryPage>() {
            @Override public StoreCategoryPage run() throws Exception { return service.listCategories(); }
        }, new AsyncTask.Callback<StoreCategoryPage>() {
            @Override public void onSuccess(StoreCategoryPage value) {
                category.removeAllItems(); category.addItem(StoreCategoryOption.empty());
                if (value != null) for (StoreCategoryDto item : value.getItems()) {
                    if (item != null) category.addItem(StoreCategoryOption.from(item));
                }
                selectCategory(pendingCategoryCode);
            }
            @Override public void onFailure(Throwable error) { category.removeAllItems(); category.addItem(StoreCategoryOption.empty()); }
        });
    }

    private void loadProducts() {
        AsyncTask.run(new AsyncTask.Work<ProductPage>() {
            @Override public ProductPage run() throws Exception {
                return service.searchProducts(new ProductQuery(null, null, null, 1, 100));
            }
        }, new AsyncTask.Callback<ProductPage>() {
            @Override public void onSuccess(ProductPage value) {
                product.removeAllItems(); product.addItem(StoreProductOption.empty());
                if (value != null) for (ProductDto item : value.getItems()) {
                    if (item != null && !"ARCHIVED".equals(item.getStatus())) product.addItem(StoreProductOption.from(item));
                }
                selectProduct(pendingProductId);
            }
            @Override public void onFailure(Throwable error) { product.removeAllItems(); product.addItem(StoreProductOption.empty()); }
        });
    }

    private void showPromotion(PromotionDto value) {
        if (value == null) return;
        selectedId = value.getId(); code.setText(value.getCode()); name.setText(value.getName());
        this.value.setText(String.valueOf(value.getValue()));
        threshold.setText(value.getThreshold() == null ? "" : String.valueOf(value.getThreshold()));
        pendingProductId = value.getProductId(); pendingCategoryCode = value.getCategoryCode();
        selectProduct(pendingProductId); selectCategory(pendingCategoryCode);
        type.setSelectedItem(RealUi.option(value.getType())); scope.setSelectedItem(RealUi.option(value.getProductScope()));
        active.setSelectedItem(RealUi.option(value.isActive() ? "ACTIVE" : "INACTIVE")); updateTargetState();
    }

    private void save() {
        try {
            String promotionType = RealUi.code(type.getSelectedItem());
            String productScope = RealUi.code(scope.getSelectedItem());
            StoreProductOption selectedProduct = (StoreProductOption) product.getSelectedItem();
            StoreCategoryOption selectedCategory = (StoreCategoryOption) category.getSelectedItem();
            Long productId = selectedProduct == null || selectedProduct.getId() == 0L ? null : selectedProduct.getId();
            String categoryCode = selectedCategory == null ? null : selectedCategory.getCode();
            if ("PRODUCT".equals(productScope) && productId == null) throw new IllegalArgumentException("请选择指定商品");
            if ("CATEGORY".equals(productScope) && categoryCode == null) throw new IllegalArgumentException("请选择指定分类");
            final PromotionWriteRequest request = new PromotionWriteRequest(selectedId,
                    RealUi.required(code.getText(), "促销编码"), RealUi.required(name.getText(), "促销名称"),
                    promotionType, decimal(threshold.getText()), decimal(value.getText()), productScope,
                    productId, categoryCode, org.threeten.bp.LocalDateTime.now().minusMinutes(1L),
                    org.threeten.bp.LocalDateTime.now().plusYears(1L), false,
                    "ACTIVE".equals(RealUi.code(active.getSelectedItem())));
            AsyncTask.run(new AsyncTask.Work<PromotionDto>() {
                @Override public PromotionDto run() throws Exception { return service.savePromotion(request); }
            }, new AsyncTask.Callback<PromotionDto>() {
                @Override public void onSuccess(PromotionDto value) { state.setText("促销规则已保存"); table.reload(); }
                @Override public void onFailure(Throwable error) { state.setText(AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
            });
        } catch (Exception error) { state.setText(error.getMessage()); }
    }

    private void selectCategory(String code) {
        pendingCategoryCode = code;
        for (int i = 0; i < category.getItemCount(); i++) {
            StoreCategoryOption item = category.getItemAt(i);
            if (code == null ? item.getCode() == null : code.equals(item.getCode())) { category.setSelectedIndex(i); return; }
        }
        if (code != null) { category.addItem(StoreCategoryOption.fallback(code)); category.setSelectedIndex(category.getItemCount() - 1); }
    }

    private void selectProduct(Long id) {
        pendingProductId = id;
        for (int i = 0; i < product.getItemCount(); i++) {
            StoreProductOption item = product.getItemAt(i);
            if (id == null ? item.getId() == 0L : id.longValue() == item.getId()) { product.setSelectedIndex(i); return; }
        }
        if (id != null) { product.addItem(StoreProductOption.fallback(id)); product.setSelectedIndex(product.getItemCount() - 1); }
    }

    private void updateTargetState() {
        String selected = RealUi.code(scope.getSelectedItem());
        product.setEnabled("PRODUCT".equals(selected)); category.setEnabled("CATEGORY".equals(selected));
    }

    private static void add(JPanel panel, String label, java.awt.Component component) { panel.add(UiFactory.labelledField(label, component)); }
    private static BigDecimal decimal(String value) { String text = RealUi.optional(value); return text == null ? null : new BigDecimal(text); }
    public void reload() { table.reload(); }
    JComboBox<StoreCategoryOption> categoryBox() { return category; }
    JComboBox<StoreProductOption> productBox() { return product; }
    JComboBox<RealUi.CodeOption> scopeBox() { return scope; }
}
