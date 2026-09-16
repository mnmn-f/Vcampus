package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.InputLimiter;
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

import java.awt.BorderLayout;
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
    private final JLabel valueLabel = UiFactory.body("减免金额（元）");
    private final JLabel thresholdLabel = UiFactory.body("满多少元");
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
    private PromotionDto selectedPromotion;
    private String displayedType = "THRESHOLD";
    private boolean showingPromotion;

    public StorePromotionPanel(BasePage page, StoreClientService service) {
        super();
        this.page = page; this.service = service; setOpaque(false);
        InputLimiter.code(code, 64); InputLimiter.length(name, 120);
        InputLimiter.decimal(value, 10, 2); InputLimiter.decimal(threshold, 10, 2);
        table = promotionTable();
        table.setItemKey(PromotionDto::getId);
        JButton create = new PrimaryButton("新建促销"); create.addActionListener(e -> startNew()); table.addAction(create);
        JPanel fields = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(185, 4, 8)); fields.setOpaque(false);
        add(fields, "编码", code); add(fields, "名称", name); add(fields, "类型", type);
        fields.add(labelledField(valueLabel, value)); fields.add(labelledField(thresholdLabel, threshold)); add(fields, "范围", scope);
        add(fields, "指定商品", product); add(fields, "指定分类", category); add(fields, "状态", active);
        JButton save = new PrimaryButton("保存促销");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        fields.add(UiFactory.formActionCell(save, state));
        scope.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateTargetState(); }
        });
        type.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { updateTypeState(true); }
        });
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); add(table); add(fields);
        updateTypeState(false); updateTargetState(); loadCategories(); loadProducts();
    }

    private AsyncPagedTable<PromotionDto> promotionTable() {
        return new AsyncPagedTable<PromotionDto>("促销优惠", "", "促销关键字", new String[0],
                new String[]{"编码", "名称", "类型", "优惠内容", "范围", "状态"},
                new AsyncPagedTable.Loader<PromotionDto>() {
                    @Override public PageSlice<PromotionDto> load(int p, String k, String f) throws Exception {
                        PromotionPage value = service.listPromotions();
                        return PageSlice.filter(value == null ? null : value.getItems(), k, p, 20,
                                row -> row.getCode() + " " + row.getName());
                    }
                }, new AsyncPagedTable.RowMapper<PromotionDto>() {
                    @Override public Object[] values(PromotionDto value) {
                        return new Object[]{value.getCode(), value.getName(), RealUi.status(value.getType()),
                                promotionText(value), RealUi.status(value.getProductScope()), value.isActive() ? "启用" : "停用"};
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
        showingPromotion = true;
        selectedPromotion = value;
        selectedId = value.getId(); code.setText(value.getCode()); name.setText(value.getName());
        pendingProductId = value.getProductId(); pendingCategoryCode = value.getCategoryCode();
        selectProduct(pendingProductId); selectCategory(pendingCategoryCode);
        type.setSelectedItem(RealUi.option(value.getType())); displayedType = value.getType(); updateTypeState(false);
        this.value.setText(inputValue(value));
        threshold.setText(value.getThreshold() == null || value.getThreshold().signum() <= 0 ? "" : number(value.getThreshold()));
        scope.setSelectedItem(RealUi.option(value.getProductScope()));
        active.setSelectedItem(RealUi.option(value.isActive() ? "ACTIVE" : "INACTIVE")); updateTargetState();
        showingPromotion = false;
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
            BigDecimal promotionValue = requestValue(promotionType, value.getText());
            BigDecimal promotionThreshold = requestThreshold(promotionType, threshold.getText());
            if ("THRESHOLD".equals(promotionType) && promotionValue.compareTo(promotionThreshold) >= 0) {
                throw new IllegalArgumentException("减免金额必须小于满减门槛");
            }
            final PromotionWriteRequest request = new PromotionWriteRequest(selectedId,
                    RealUi.required(code.getText(), "促销编码"), RealUi.required(name.getText(), "促销名称"),
                    promotionType, promotionThreshold, promotionValue, productScope,
                    productId, categoryCode, selectedPromotion == null ? org.threeten.bp.LocalDateTime.now().minusMinutes(1L) : selectedPromotion.getStartsAt(),
                    selectedPromotion == null ? org.threeten.bp.LocalDateTime.now().plusYears(1L) : selectedPromotion.getEndsAt(),
                    selectedPromotion != null && selectedPromotion.isStackable(),
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

    private void updateTypeState(boolean clearChangedValue) {
        String selected = RealUi.code(type.getSelectedItem());
        if (selected == null || selected.isEmpty()) selected = "THRESHOLD";
        if (clearChangedValue && !showingPromotion && !selected.equals(displayedType)) {
            value.setText(""); state.setText(" ");
        }
        displayedType = selected;
        if ("PERCENT".equals(selected)) {
            valueLabel.setText("折扣（折）");
            thresholdLabel.setText("消费门槛（元，可选）");
            value.putClientProperty("JTextField.placeholderText", "例如：8.5");
        } else if ("FIXED".equals(selected)) {
            valueLabel.setText("立减金额（元）");
            thresholdLabel.setText("消费门槛（元，可选）");
            value.putClientProperty("JTextField.placeholderText", "例如：5.00");
        } else {
            valueLabel.setText("减免金额（元）");
            thresholdLabel.setText("满多少元");
            value.putClientProperty("JTextField.placeholderText", "例如：5.00");
        }
        threshold.putClientProperty("JTextField.placeholderText",
                "THRESHOLD".equals(selected) ? "例如：30.00" : "不限制可留空");
        value.setToolTipText("PERCENT".equals(selected) ? "请输入折数，例如 8.5 表示八五折" : null);
    }

    private static void add(JPanel panel, String label, java.awt.Component component) { panel.add(UiFactory.labelledField(label, component)); }
    private static JPanel labelledField(JLabel label, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH); panel.add(component, BorderLayout.CENTER); return panel;
    }
    private static BigDecimal requestValue(String type, String text) {
        BigDecimal input = decimal(RealUi.required(text, "促销数值"));
        if (input.signum() <= 0) throw new IllegalArgumentException("促销数值必须大于0");
        if ("PERCENT".equals(type)) {
            if (input.compareTo(BigDecimal.TEN) > 0) throw new IllegalArgumentException("折扣必须大于0折且不超过10折");
            return input.multiply(BigDecimal.TEN);
        }
        return input;
    }
    private static BigDecimal requestThreshold(String type, String text) {
        String input = RealUi.optional(text);
        if (input == null) {
            if ("THRESHOLD".equals(type)) throw new IllegalArgumentException("请填写满减门槛");
            return null;
        }
        BigDecimal result = decimal(input);
        if (result.signum() <= 0) throw new IllegalArgumentException("消费门槛必须大于0");
        return result;
    }
    static String promotionText(PromotionDto promotion) {
        if (promotion == null) return "--";
        String type = promotion.getType(); String prefix = promotion.getThreshold() != null
                && promotion.getThreshold().signum() > 0 ? "满" + number(promotion.getThreshold()) + "元" : "";
        if ("PERCENT".equalsIgnoreCase(type)) return prefix.isEmpty()
                ? inputValue(promotion) + "折" : prefix + "享" + inputValue(promotion) + "折";
        if ("THRESHOLD".equalsIgnoreCase(type)) return prefix + "减" + number(promotion.getValue()) + "元";
        return prefix + "立减" + number(promotion.getValue()) + "元";
    }
    private static String inputValue(PromotionDto promotion) {
        if (promotion == null || promotion.getValue() == null) return "";
        return "PERCENT".equalsIgnoreCase(promotion.getType())
                ? number(promotion.getValue().divide(BigDecimal.TEN)) : number(promotion.getValue());
    }
    private static String number(BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString(); }
    private static BigDecimal decimal(String value) {
        try { return new BigDecimal(value.trim()); }
        catch (RuntimeException error) { throw new IllegalArgumentException("请输入有效数字"); }
    }
    public void reload() { table.reload(); }
    private void startNew() {
        showingPromotion = true;
        table.getTable().clearSelection(); selectedPromotion = null; selectedId = 0;
        code.setText(""); name.setText(""); value.setText(""); threshold.setText("");
        selectProduct(null); selectCategory(null); scope.setSelectedIndex(0); type.setSelectedIndex(0); active.setSelectedIndex(0);
        displayedType = RealUi.code(type.getSelectedItem()); updateTypeState(false); showingPromotion = false;
        state.setText(" "); loadProducts(); loadCategories();
    }
    JComboBox<StoreCategoryOption> categoryBox() { return category; }
    JComboBox<StoreProductOption> productBox() { return product; }
    JComboBox<RealUi.CodeOption> scopeBox() { return scope; }
}
