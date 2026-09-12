package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 商店分类和促销范围只使用服务端提供的受控选项。 */
public final class StoreControlledChoicesTest {
    @Test public void editingPromotionPreservesItsScheduleAndCanStartAnotherRule() throws Exception {
        org.threeten.bp.LocalDateTime start = org.threeten.bp.LocalDateTime.of(2026, 9, 1, 8, 30);
        PromotionDto original = new PromotionDto(11L, "PROMO-11", "开学优惠", "FIXED", BigDecimal.ZERO,
                BigDecimal.ONE, "ALL", null, null, start, start.plusMonths(2), true, true);
        AtomicReference<PromotionWriteRequest> sent = new AtomicReference<>();
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
            if (method.getName().equals("listPromotions")) return new PromotionPage(Collections.singletonList(original), 1);
            if (method.getName().equals("savePromotion")) { sent.set((PromotionWriteRequest) args[0]); return original; }
            return null;
        });
        AtomicReference<StorePromotionPanel> holder = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> holder.set(new StorePromotionPanel(new TestPage(), service)));
        StorePromotionPanel panel = holder.get(); AsyncPagedTable<?> table = field(panel, "table");
        AsyncPagedTableRefreshTest.await(() -> table.getTable().getRowCount() == 1);
        AsyncPagedTableRefreshTest.edt(() -> { table.getTable().setRowSelectionInterval(0, 0); click(panel, "保存促销"); });
        AsyncPagedTableRefreshTest.await(() -> sent.get() != null);
        assertEquals(start, sent.get().getStartsAt()); assertEquals(start.plusMonths(2), sent.get().getEndsAt()); assertTrue(sent.get().isStackable());
        AsyncPagedTableRefreshTest.await(() -> !table.isLoading());
        AsyncPagedTableRefreshTest.edt(() -> click(panel, "新建促销"));
        assertEquals("", textField(panel, "code").getText());
    }

    @Test public void finiteCatalogSearchActuallyFiltersAndPaginates() {
        java.util.List<String> values = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) values.add("FOOD" + i); values.add("BOOK");
        PageSlice<String> page = PageSlice.filter(values, "food", 2, 20, v -> v);
        assertEquals(25L, page.getTotal()); assertEquals(5, page.getItems().size()); assertEquals("FOOD20", page.getItems().get(0));
        assertTrue(PageSlice.filter(values, "missing", 1, 20, v -> v).getItems().isEmpty());
    }
    @Test
    public void productEditorDisplaysCategoryNameAndSubmitsCode() throws Exception {
        final AtomicReference<ProductWriteRequest> sent = new AtomicReference<ProductWriteRequest>();
        StoreProductEditorPanel editor = new StoreProductEditorPanel(new StoreProductEditorPanel.Listener() {
            @Override public void onSave(ProductWriteRequest request) { sent.set(request); }
            @Override public void onAdjust(edu.seu.vcampus.common.dto.store.StockAdjustRequest request) { }
        });
        editor.setCategories(Arrays.asList(new StoreCategoryDto(7L, "FOOD", "食品饮料", true)));
        JComboBox<?> category = field(editor, "category");
        assertEquals(2, category.getItemCount());
        assertEquals("食品饮料", String.valueOf(category.getItemAt(1)));
        category.setSelectedIndex(1); textField(editor, "name").setText("测试商品");
        textField(editor, "price").setText("2.50"); textField(editor, "stock").setText("3");
        click(editor, "保存商品");
        assertNotNull(sent.get()); assertEquals("FOOD", sent.get().getCategory());
    }

    @Test
    public void productWithoutCategoryIsRejectedBeforeSubmission() throws Exception {
        final AtomicReference<ProductWriteRequest> sent = new AtomicReference<ProductWriteRequest>();
        StoreProductEditorPanel editor = new StoreProductEditorPanel(new StoreProductEditorPanel.Listener() {
            @Override public void onSave(ProductWriteRequest request) { sent.set(request); }
            @Override public void onAdjust(edu.seu.vcampus.common.dto.store.StockAdjustRequest request) { }
        });
        textField(editor, "name").setText("未分类商品"); textField(editor, "price").setText("2");
        textField(editor, "stock").setText("1"); click(editor, "保存商品");
        assertEquals(null, sent.get());
    }

    @Test
    public void promotionUsesCatalogOptionsAndHidesDatabaseIds() throws Exception {
        final AtomicReference<PromotionWriteRequest> sent = new AtomicReference<PromotionWriteRequest>();
        final CountDownLatch choices = new CountDownLatch(2), saved = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(
                StoreClientService.class.getClassLoader(), new Class<?>[]{StoreClientService.class},
                (proxy, method, args) -> {
                    if ("listCategories".equals(method.getName())) { choices.countDown(); return categories(); }
                    if ("searchProducts".equals(method.getName())) { choices.countDown(); return products(); }
                    if ("listPromotions".equals(method.getName())) return new PromotionPage(null, 0L);
                    if ("savePromotion".equals(method.getName())) { sent.set((PromotionWriteRequest) args[0]); saved.countDown(); return null; }
                    return null;
                });
        StorePromotionPanel panel = new StorePromotionPanel(new TestPage(), service);
        assertTrue(choices.await(2, TimeUnit.SECONDS));
        waitForChoices(panel);
        JComboBox<?> category = field(panel, "category"); JComboBox<?> product = field(panel, "product");
        assertEquals("食品饮料", String.valueOf(category.getItemAt(1)));
        assertFalse("77".equals(String.valueOf(product.getItemAt(1))));
        textField(panel, "code").setText("P-TEST"); textField(panel, "name").setText("食品优惠");
        textField(panel, "value").setText("1"); combo(panel, "scope").setSelectedItem(RealUi.option("CATEGORY"));
        category.setSelectedIndex(1); click(panel, "保存促销");
        assertTrue(saved.await(2, TimeUnit.SECONDS)); assertEquals("FOOD", sent.get().getCategoryCode());
        assertEquals(null, sent.get().getProductId());
    }

    @Test
    public void productFilterUsesStableCategoryCodeAndShowsCategoryName() throws Exception {
        final AtomicReference<ProductQuery> lastQuery = new AtomicReference<ProductQuery>();
        final CountDownLatch catalog = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(
                StoreClientService.class.getClassLoader(), new Class<?>[]{StoreClientService.class},
                (proxy, method, args) -> {
                    if ("listCategories".equals(method.getName())) { catalog.countDown(); return categories(); }
                    if ("searchProducts".equals(method.getName())) { lastQuery.set((ProductQuery) args[0]); return products(); }
                    return null;
                });
        StoreProductsPanel panel = new StoreProductsPanel(new TestPage(), service, Role.STUDENT);
        assertTrue(catalog.await(2, TimeUnit.SECONDS)); waitForProductRows(panel);
        JComboBox<?> categories = field(panel, "category");
        assertEquals("食品饮料", String.valueOf(categories.getItemAt(1)));
        assertFalse(String.valueOf(categories.getItemAt(1)).contains("FOOD"));
        AsyncPagedTable<?> table = field(panel, "products");
        assertEquals("食品饮料", String.valueOf(table.getTable().getValueAt(0, table.getTable().getColumnModel().getColumnIndex("分类"))));
        categories.setSelectedIndex(1); click(panel, "按分类筛选");
        for (int i = 0; i < 40 && (lastQuery.get() == null || !"FOOD".equals(lastQuery.get().getCategory())); i++) {
            Thread.sleep(25L); SwingUtilities.invokeAndWait(new Runnable() { @Override public void run() { } });
        }
        assertEquals("FOOD", lastQuery.get().getCategory());
    }

    private static StoreCategoryPage categories() {
        return new StoreCategoryPage(Arrays.asList(new StoreCategoryDto(7L, "FOOD", "食品饮料", true)), 1L);
    }
    private static ProductPage products() {
        ProductDto row = new ProductDto(77L, "SKU-77", "校园水杯", "FOOD", "", BigDecimal.ONE, 8, "ON_SALE");
        return new ProductPage(Collections.singletonList(row), 1, 100, 1L);
    }
    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name); value.setAccessible(true); return (T) value.get(target);
    }
    private static JTextField textField(Object target, String name) throws Exception { return field(target, name); }
    private static JComboBox<?> combo(Object target, String name) throws Exception { return field(target, name); }
    private static void waitForChoices(final StorePromotionPanel panel) throws Exception {
        for (int i = 0; i < 40; i++) {
            SwingUtilities.invokeAndWait(new Runnable() { @Override public void run() { } });
            if (combo(panel, "category").getItemCount() > 1 && combo(panel, "product").getItemCount() > 1) return;
            Thread.sleep(25L);
        }
    }
    private static void waitForProductRows(final StoreProductsPanel panel) throws Exception {
        for (int i = 0; i < 40; i++) {
            SwingUtilities.invokeAndWait(new Runnable() { @Override public void run() { } });
            AsyncPagedTable<?> table = field(panel, "products");
            if (combo(panel, "category").getItemCount() > 1 && table.getTable().getRowCount() > 0
                    && "食品饮料".equals(String.valueOf(table.getTable().getValueAt(0, 2)))) return;
            Thread.sleep(25L);
        }
    }
    private static void click(Component root, String text) {
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) { ((AbstractButton) root).doClick(); return; }
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) click(child, text);
    }
    private static final class TestPage extends BasePage {
        TestPage() { super(session(), "商店", ""); }
        private static ClientSession session() { ClientSession value = new ClientSession(); value.open(new LoginResult(1L, "manager", "管理员", Role.STORE_MANAGER, "token")); return value; }
    }
}
