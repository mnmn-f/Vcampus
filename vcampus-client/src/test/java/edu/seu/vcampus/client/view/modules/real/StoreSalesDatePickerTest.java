package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import org.threeten.bp.LocalDate;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 销售统计日期通过日历选择，并按选择结果查询。 */
public final class StoreSalesDatePickerTest {
    @Test public void selectedDateRangeIsSentToSalesQuery() throws Exception {
        AtomicReference<StoreSalesQuery> query = new AtomicReference<StoreSalesQuery>();
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{StoreClientService.class},
                (proxy, method, args) -> {
                    if ("searchProducts".equals(method.getName())) {
                        return new ProductPage(null, 1, 100, 0L);
                    }
                    if ("salesReport".equals(method.getName())) {
                        query.set((StoreSalesQuery) args[0]); return null;
                    }
                    return null;
                });
        AtomicReference<StoreSalesPanel> panel = new AtomicReference<StoreSalesPanel>();
        SwingUtilities.invokeAndWait(() -> panel.set(new StoreSalesPanel(page(), service)));
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);
        SwingUtilities.invokeAndWait(() -> {
            panel.get().startDateField().setDate(start);
            panel.get().endDateField().setDate(end);
            click(panel.get(), "查询统计");
        });
        AsyncPagedTableRefreshTest.await(() -> query.get() != null
                && start.equals(query.get().getStartDate()));
        assertNotNull(panel.get().startDateField());
        assertEquals(start, query.get().getStartDate());
        assertEquals(end, query.get().getEndDate());
    }

    private static void click(Component root, String text) {
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) {
            ((AbstractButton) root).doClick(); return;
        }
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            click(child, text);
        }
    }

    private static BasePage page() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(9L, "manager", "商店管理员",
                Role.STORE_MANAGER, "token"));
        return new BasePage(session, "商店", "") { };
    }
}
