package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 商品检索、详情、购物车入口和商店管理员库存维护。 */
public final class StoreProductsPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final Role role;
    private final JLabel detail = UiFactory.muted("选择商品查看详情。");
    private final ProductImageView image = new ProductImageView();
    private final JComboBox<StoreCategoryOption> category = new JComboBox<StoreCategoryOption>();
    private final AsyncPagedTable<ProductDto> products;
    private final StoreProductEditorPanel editor;
    private final Runnable cartChanged;
    private final Map<String, String> categoryNames = new HashMap<String, String>();
    private long selectedProductId;

    public StoreProductsPanel(BasePage page, StoreClientService service, Role role) {
        this(page, service, role, null);
    }

    public StoreProductsPanel(BasePage page, StoreClientService service, Role role,
                              Runnable cartChanged) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role; this.cartChanged = cartChanged;
        editor = role == Role.STORE_MANAGER ? new StoreProductEditorPanel(new EditorListener()) : null;
        JPanel filter = UiFactory.horizontal(8); filter.add(UiFactory.body("商品分类")); filter.add(category);
        JButton apply = new edu.seu.vcampus.client.ui.components.SecondaryButton("按分类筛选");
        apply.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { products.reload(); } });
        filter.add(apply); add(filter);
        products = table(); add(products);
        JPanel info = new JPanel(new BorderLayout(12, 0)); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); info.add(image, BorderLayout.EAST); add(info);
        if (editor != null) add(editor);
        loadCategories();
    }

    private AsyncPagedTable<ProductDto> table() {
        AsyncPagedTable<ProductDto> table = new AsyncPagedTable<ProductDto>("商品检索与库存", 
                "搜索商品并查看库存。",
                "搜索商品名称、编码或说明", role == Role.STUDENT
                        ? new String[]{"全部商品", "在售"}
                        : new String[]{"全部状态", "在售", "已下架", "草稿", "已归档"},
                new String[]{"编码", "商品", "分类", "单价", "库存", "状态"},
                new AsyncPagedTable.Loader<ProductDto>() {
                    @Override public PageSlice<ProductDto> load(int p, String keyword, String filter) throws Exception {
                        StoreCategoryOption selected = (StoreCategoryOption) category.getSelectedItem();
                        String code = selected == null ? null : selected.getCode();
                        return slice(service.searchProducts(new ProductQuery(keyword, code, status(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<ProductDto>() {
                    @Override public Object[] values(ProductDto row) { return new Object[]{RealUi.text(row.getSku()), row.getName(), categoryLabel(row.getCategory()),
                            "¥" + RealUi.text(row.getPrice()), lowStock(row.getStockQty()), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<ProductDto>() {
                    @Override public void onSelected(ProductDto row) { select(row); }
                });
        if (role == Role.STUDENT) {
            JButton add = new PrimaryButton("加入购物车"); add.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { addCart(); }
            }); table.addAction(add);
        } else if (role == Role.STORE_MANAGER) {
            JButton create = new PrimaryButton("新建商品"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); table.addAction(create);
        }
        return table;
    }

    private void select(final ProductDto value) {
        if (value == null) { selectedProductId = 0L; detail.setText("选择商品查看详情。"); if (editor != null) editor.startNew(); return; }
        selectedProductId = value.getId();
        image.load(value.getImageUrl());
        detail.setText("商品详情：" + RealUi.text(value.getName()) + "　编码 " + RealUi.text(value.getSku())
                + "　单价 ¥" + RealUi.text(value.getPrice()) + "　库存 " + value.getStockQty()
                + "　评分 " + value.getRatingAverage() + "（" + value.getRatingCount() + " 条）　说明：" + RealUi.text(value.getDescription()));
        if (editor != null) editor.showProduct(value);
        AsyncTask.run(new AsyncTask.Work<ProductDto>() {
            @Override public ProductDto run() throws Exception { return service.getProductDetail(value.getId()); }
        }, new AsyncTask.Callback<ProductDto>() {
            @Override public void onSuccess(ProductDto result) { if (result.getId() != selectedProductId) return; image.load(result.getImageUrl()); detail.setText("商品详情：" + RealUi.text(result.getName())
                    + "　单价 ¥" + RealUi.text(result.getPrice()) + "　库存 " + result.getStockQty()
                    + "　评分 " + result.getRatingAverage() + "（" + result.getRatingCount() + " 条）　状态：" + RealUi.status(result.getStatus())); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void addCart() {
        final ProductDto value = products.selectedItem();
        if (value == null) { page.showWarning("请先选择商品。"); return; }
        AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.store.CartDto>() {
            @Override public edu.seu.vcampus.common.dto.store.CartDto run() throws Exception { return service.addCartItem(new CartItemRequest(value.getId(), 1)); }
        },
                new AsyncTask.Callback<edu.seu.vcampus.common.dto.store.CartDto>() {
                    @Override public void onSuccess(edu.seu.vcampus.common.dto.store.CartDto result) {
                        page.showSuccess("商品已加入购物车。"); if (cartChanged != null) cartChanged.run();
                    }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private void loadCategories() {
        category.removeAllItems(); category.addItem(StoreCategoryOption.all());
        AsyncTask.run(new AsyncTask.Work<StoreCategoryPage>() {
            @Override public StoreCategoryPage run() throws Exception { return service.listCategories(); }
        }, new AsyncTask.Callback<StoreCategoryPage>() {
            @Override public void onSuccess(StoreCategoryPage result) {
                categoryNames.clear();
                category.removeAllItems(); category.addItem(StoreCategoryOption.all());
                if (result != null) for (StoreCategoryDto value : result.getItems()) {
                    if (value != null) {
                        categoryNames.put(normalize(value.getCode()), value.getName());
                        if (value.isActive()) category.addItem(StoreCategoryOption.from(value));
                    }
                }
                if (editor != null) editor.setCategories(result == null ? null : result.getItems());
                products.reload();
            }
            @Override public void onFailure(Throwable error) {
                if (editor != null) editor.setCategories(null);
            }
        });
    }

    private String categoryLabel(String code) {
        String label = code == null ? null : categoryNames.get(normalize(code));
        return label == null || label.trim().isEmpty() ? RealUi.status(code) : label;
    }
    private static String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }

    private void save(final ProductWriteRequest request) {
        AsyncTask.run(new AsyncTask.Work<ProductDto>() {
            @Override public ProductDto run() throws Exception { return service.saveProduct(request); }
        }, new AsyncTask.Callback<ProductDto>() {
            @Override public void onSuccess(ProductDto result) {
                page.showSuccess("商品已保存。");
                select(result);
                products.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void adjust(final StockAdjustRequest request) {
        AsyncTask.run(new AsyncTask.Work<ProductDto>() {
            @Override public ProductDto run() throws Exception { return service.adjustProductStock(request); }
        }, new AsyncTask.Callback<ProductDto>() {
            @Override public void onSuccess(ProductDto result) {
                page.showSuccess("库存已调整。");
                select(result);
                products.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static PageSlice<ProductDto> slice(ProductPage value) {
        return new PageSlice<ProductDto>(value.getItems(), value.getTotal(), value.getPage(), value.getPageSize());
    }
    private static String status(String filter) {
        if ("在售".equals(filter)) return "ON_SALE"; if ("已下架".equals(filter)) return "OFF_SALE";
        if ("草稿".equals(filter)) return "DRAFT"; if ("已归档".equals(filter)) return "ARCHIVED"; return null;
    }
    private static String lowStock(int value) { return value <= 5 ? value + "（低库存）" : String.valueOf(value); }
    private final class EditorListener implements StoreProductEditorPanel.Listener {
        @Override public void onSave(ProductWriteRequest request) { save(request); }
        @Override public void onAdjust(StockAdjustRequest request) { adjust(request); }
    }
}
