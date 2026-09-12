package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemDto;
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
import javax.swing.JTable;
import org.junit.Test;
import static org.junit.Assert.*;

public class StoreCartWorkflowTest {
    @Test public void removeCannotBeDoubleSubmittedAndReaddedProductAppearsImmediately() throws Exception {
        AtomicReference<CartDto> remote = new AtomicReference<>(cart(true));
        AtomicInteger removals = new AtomicInteger(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
            if (method.getName().equals("getCart")) return remote.get();
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

    private static CartDto cart(boolean item) {
        return new CartDto(1, 2, "ACTIVE", item ? Collections.singletonList(new CartItemDto(3, "SKU", "测试商品", BigDecimal.TEN, 1)) : Collections.emptyList(), item ? BigDecimal.TEN : BigDecimal.ZERO);
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
}
