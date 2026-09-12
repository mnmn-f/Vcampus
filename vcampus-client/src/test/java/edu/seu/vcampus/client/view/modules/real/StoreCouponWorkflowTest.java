package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

public class StoreCouponWorkflowTest {
    @Test public void cannotClaimExpiredCouponOrSendDuplicateClaim() throws Exception {
        AtomicInteger calls = new AtomicInteger(); CountDownLatch release = new CountDownLatch(1);
        AtomicReference<CouponDto> available = new AtomicReference<>(coupon(1, false, false));
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("listCoupons")) return new CouponPage(Arrays.asList(available.get(), coupon(2, false, true)), 2);
                    if (method.getName().equals("claimCoupon")) {
                        calls.incrementAndGet(); assertTrue(release.await(5, TimeUnit.SECONDS));
                        available.set(coupon(1, true, false)); return available.get();
                    }
                    return null;
                });
        AtomicReference<StoreCouponPanel> panel = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> panel.set(new StoreCouponPanel(new BasePage(new ClientSession(), "商店", "") { }, service)));
        AsyncPagedTable<?> table = field(panel.get(), "table"); JButton claim = field(panel.get(), "claim");
        AsyncPagedTableRefreshTest.await(() -> table.getTable().getRowCount() == 2);
        try {
            AsyncPagedTableRefreshTest.edt(() -> {
                table.getTable().setRowSelectionInterval(1, 1); assertFalse(claim.isEnabled());
                assertEquals("已过期", table.getTable().getValueAt(1, 5));
                table.getTable().setRowSelectionInterval(0, 0); claim.doClick(); claim.doClick(); assertFalse(claim.isEnabled());
            });
            AsyncPagedTableRefreshTest.await(() -> calls.get() == 1); release.countDown();
            AsyncPagedTableRefreshTest.await(() -> "已领取".equals(table.getTable().getValueAt(0, 5)));
            AsyncPagedTableRefreshTest.edt(() -> assertFalse(claim.isEnabled())); assertEquals(1, calls.get());
        } finally { release.countDown(); }
    }

    private static CouponDto coupon(long id, boolean claimed, boolean expired) {
        return new CouponDto(id, "C" + id, "优惠券" + id, BigDecimal.TEN, BigDecimal.ONE,
                expired ? LocalDateTime.now().minusDays(1) : LocalDateTime.now().plusDays(30), claimed, false);
    }
    @SuppressWarnings("unchecked") private static <T> T field(Object object, String name) throws Exception {
        java.lang.reflect.Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return (T) field.get(object);
    }
}
