package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemDto;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.OrderDto;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

public class StoreCartWorkflowTest {
    @Test public void submittedOrderClearsRowsBeforeServerRefreshReturns() throws Exception {
        AtomicInteger reads = new AtomicInteger(); CountDownLatch refreshEntered = new CountDownLatch(1), release = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
            if (method.getName().equals("listCoupons")) return new CouponPage(Collections.emptyList(), 0);
            if (method.getName().equals("getCart")) { if (reads.incrementAndGet() == 1) return cart(true); refreshEntered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); return cart(false); }
            if (method.getName().equals("confirmCheckout")) return new OrderDto(9L, "VC-20260914-0009", 2L, BigDecimal.TEN, "CREATED", Collections.emptyList());
            throw new UnsupportedOperationException(method.getName());
        });
        StoreCartPanel panel = create(service); JTable table = find(panel, JTable.class);
        AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 1);
        try {
            java.lang.reflect.Method confirm = StoreCartPanel.class.getDeclaredMethod("confirmOrder", String.class); confirm.setAccessible(true);
            AsyncPagedTableRefreshTest.edt(() -> { try { confirm.invoke(panel, new Object[]{null}); } catch (Exception error) { throw new RuntimeException(error); } });
            assertTrue(refreshEntered.await(2, TimeUnit.SECONDS));
            AsyncPagedTableRefreshTest.edt(() -> { assertEquals(0, table.getRowCount()); assertFalse(button(panel, "提交订单").isEnabled()); });
        } finally { release.countDown(); }
    }

    @Test public void removeCannotBeDoubleSubmittedAndReaddedProductAppearsImmediately() throws Exception {
        AtomicReference<CartDto> remote = new AtomicReference<>(cart(true));
        AtomicInteger removals = new AtomicInteger(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
            if (method.getName().equals("getCart")) return remote.get();
            if (method.getName().equals("listCoupons")) return new CouponPage(Collections.emptyList(), 0);
            if (method.getName().equals("removeCartItem")) { removals.incrementAndGet(); entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS)); remote.set(cart(false)); return remote.get(); }
            throw new UnsupportedOperationException(method.getName());
        });
        StoreCartPanel panel = create(service); JTable table = find(panel, JTable.class);
        AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 1);
        try {
            AsyncPagedTableRefreshTest.edt(() -> { table.setRowSelectionInterval(0, 0); button(panel, "移除商品").doClick(); button(panel, "移除商品").doClick(); });
            assertTrue(entered.await(2, TimeUnit.SECONDS)); assertEquals(1, removals.get()); release.countDown();
            AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 0);
            remote.set(cart(true)); AsyncPagedTableRefreshTest.edt(panel::reload);
            AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 1);
            AsyncPagedTableRefreshTest.edt(() -> assertTrue(button(panel, "提交订单").isEnabled()));
        } finally { release.countDown(); }
    }

    @Test public void delayedReadCannotRestoreRemovedCartItems() throws Exception {
        AtomicInteger reads = new AtomicInteger(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
            if (method.getName().equals("listCoupons")) return new CouponPage(Collections.emptyList(), 0);
            if (!method.getName().equals("getCart")) throw new UnsupportedOperationException(method.getName());
            int read = reads.incrementAndGet();
            if (read == 2) { entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); }
            return cart(read <= 2);
        });
        StoreCartPanel panel = create(service); JTable table = find(panel, JTable.class);
        AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 1);
        try {
            AsyncPagedTableRefreshTest.edt(panel::reload); assertTrue(entered.await(2, TimeUnit.SECONDS));
            AsyncPagedTableRefreshTest.edt(panel::reload); AsyncPagedTableRefreshTest.await(() -> table.getRowCount() == 0);
            release.countDown(); Thread.sleep(120);
            AsyncPagedTableRefreshTest.edt(() -> { assertEquals(0, table.getRowCount()); assertFalse(button(panel, "提交订单").isEnabled()); });
        } finally { release.countDown(); }
    }

    @Test public void availableCouponCanBeSelectedAndAppliedWithoutTypingCode() throws Exception {
        AtomicInteger claims = new AtomicInteger(); AtomicInteger previews = new AtomicInteger();
        AtomicReference<String> code = new AtomicReference<>();
        CouponDto available = new CouponDto(1, "SAVE10", "新生券", new BigDecimal("50"),
                new BigDecimal("10"), LocalDateTime.now().plusDays(10), false, false);
        CouponDto tooExpensive = new CouponDto(2, "SAVE30", "满额券", new BigDecimal("200"),
                new BigDecimal("30"), LocalDateTime.now().plusDays(10), true, false);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getCart")) return cart(new BigDecimal("100"));
                    if (method.getName().equals("listCoupons")) return new CouponPage(
                            java.util.Arrays.asList(available, tooExpensive), 2);
                    if (method.getName().equals("claimCoupon")) {
                        claims.incrementAndGet(); code.set(((CouponClaimRequest) args[0]).getCode());
                        return new CouponDto(1, "SAVE10", "新生券", new BigDecimal("50"),
                                new BigDecimal("10"), available.getExpiresAt(), true, false);
                    }
                    if (method.getName().equals("checkoutPreview")) {
                        previews.incrementAndGet(); code.set((String) args[0]);
                        return new CheckoutPreviewDto(Collections.emptyList(), new BigDecimal("100"),
                                BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("90"),
                                null, "SAVE10", true);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        StoreCartPanel panel = create(service); JComboBox<?> choices = find(panel, JComboBox.class);
        AsyncPagedTableRefreshTest.await(() -> choices.getItemCount() == 2);
        AsyncPagedTableRefreshTest.edt(() -> {
            assertEquals("不使用优惠券", choices.getItemAt(0).toString());
            assertTrue(choices.getItemAt(1).toString().contains("满¥50.00减¥10.00"));
            choices.setSelectedIndex(1);
        });
        AsyncPagedTableRefreshTest.await(() -> claims.get() == 1 && previews.get() == 1);
        assertEquals("SAVE10", code.get());
        AsyncPagedTableRefreshTest.await(() -> hasLabel(panel, "原价 ¥100.00　优惠 ¥10.00　应付 ¥90.00"));
    }

    private static CartDto cart(boolean item) {
        return new CartDto(1, 2, "ACTIVE", item ? Collections.singletonList(new CartItemDto(3, "SKU", "测试商品", BigDecimal.TEN, 1)) : Collections.emptyList(), item ? BigDecimal.TEN : BigDecimal.ZERO);
    }
    private static CartDto cart(BigDecimal total) {
        return new CartDto(1, 2, "ACTIVE", Collections.singletonList(
                new CartItemDto(3, "SKU", "校园纪念品", total, 1)), total);
    }
    private static StoreCartPanel create(StoreClientService service) throws Exception {
        AtomicReference<StoreCartPanel> result = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> result.set(new StoreCartPanel(new BasePage(new ClientSession(), "测试", "") { }, service, null)));
        return result.get();
    }
    private static JButton button(Container parent, String label) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton && label.equals(((JButton) child).getText())) return (JButton) child;
            if (child instanceof Container) { JButton found = button((Container) child, label); if (found != null) return found; }
        }
        return null;
    }
    private static <T> T find(Container parent, Class<T> type) {
        if (type.isInstance(parent)) return type.cast(parent);
        for (Component child : parent.getComponents()) if (child instanceof Container) { T found = find((Container) child, type); if (found != null) return found; }
        return null;
    }
    private static boolean hasLabel(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText())) return true;
            if (child instanceof Container && hasLabel((Container) child, text)) return true;
        }
        return false;
    }
}
