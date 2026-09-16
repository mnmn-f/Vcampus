package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
import edu.seu.vcampus.common.security.Role;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

/** 管理员只维护物流；送达后由服务端自动完成订单。 */
public final class StoreOrdersWorkflowTest {
    @Test public void inlineShippingUpdateCompletesWithoutModalOrManualCompletion() throws Exception {
        AtomicReference<OrderDto> remote = new AtomicReference<>(order("PAID", "PREPARING"));
        AtomicReference<OrderShippingUpdateRequest> sent = new AtomicReference<>();
        AtomicInteger changed = new AtomicInteger();
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
                    if ("searchOrders".equals(method.getName())) return new OrderPage(
                            Collections.singletonList(remote.get()), 1, 20, 1L);
                    if ("getOrderDetail".equals(method.getName())) return remote.get();
                    if ("updateOrderShipping".equals(method.getName())) {
                        sent.set((OrderShippingUpdateRequest) args[0]);
                        remote.set(order("COMPLETED", "DELIVERED")); return remote.get();
                    }
                    return null;
                });
        StoreOrdersPanel panel = new StoreOrdersPanel(new TestPage(), service, Role.STORE_MANAGER,
                changed::incrementAndGet);
        AsyncPagedTable<OrderDto> table = field(panel, "orders");
        AsyncPagedTableRefreshTest.await(() -> table.getTable().getRowCount() == 1);
        assertNull(button(panel, "标记已完成")); assertNull(button(panel, "更新物流"));
        AbstractButton save = button(panel, "保存物流进度"); assertNotNull(save);
        AsyncPagedTableRefreshTest.edt(() -> {
            table.getTable().setRowSelectionInterval(0, 0);
            JComboBox<RealUi.CodeOption> status = fieldUnchecked(panel, "shippingStatus");
            status.setSelectedItem(RealUi.option("DELIVERED"));
            ((JTextField) fieldUnchecked(panel, "tracking")).setText("SEU-20260914-1");
            save.doClick();
        });
        AsyncPagedTableRefreshTest.await(() -> sent.get() != null && changed.get() == 1);
        assertEquals("DELIVERED", sent.get().getShippingStatus());
        assertEquals("COMPLETED", remote.get().getStatus());
        AsyncPagedTableRefreshTest.edt(() -> assertFalse(save.isEnabled()));
    }

    private static OrderDto order(String status, String shipping) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 18, 0);
        return new OrderDto(7L, "VC-20260914-0007", 10L, new BigDecimal("25.00"),
                new BigDecimal("25.00"), BigDecimal.ZERO, null, null, "SELF", status,
                shipping, "SEU-20260914-1", "校园驿站", now, now,
                null, "COMPLETED".equals(status) ? now : null, Collections.emptyList());
    }

    @SuppressWarnings("unchecked") private static <T> T field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return (T) field.get(target);
    }
    @SuppressWarnings("unchecked") private static <T> T fieldUnchecked(Object target, String name) {
        try { return (T) field(target, name); } catch (Exception error) { throw new AssertionError(error); }
    }
    private static AbstractButton button(Component root, String text) {
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) return (AbstractButton) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            AbstractButton found = button(child, text); if (found != null) return found;
        }
        return null;
    }
    private static final class TestPage extends BasePage {
        TestPage() { super(session(), "商店管理", ""); }
        private static ClientSession session() {
            ClientSession value = new ClientSession(); value.open(new LoginResult(
                    9L, "store_manager", "商店管理员", Role.STORE_MANAGER, "token")); return value;
        }
    }
}
