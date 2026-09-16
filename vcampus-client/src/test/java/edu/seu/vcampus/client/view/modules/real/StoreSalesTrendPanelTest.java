package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.StoreSalesDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import org.threeten.bp.LocalDate;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 销售趋势日期选择、滚动宽度和畅销商品占比。 */
public final class StoreSalesTrendPanelTest {
    @Test public void dateRangeReloadsScrollableTrendAndPieChart() throws Exception {
        AtomicReference<StoreSalesTrendQuery> query =
                new AtomicReference<StoreSalesTrendQuery>();
        List<StoreSalesTrendDto> days = days(19);
        List<StoreSalesDto> products = Arrays.asList(
                new StoreSalesDto(1, "A", "笔记本", 12, BigDecimal.TEN),
                new StoreSalesDto(2, "B", "马克杯", 8, BigDecimal.TEN),
                new StoreSalesDto(3, "C", "帆布袋", 4, BigDecimal.TEN));
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{StoreClientService.class},
                (proxy, method, args) -> {
                    if ("salesTrend".equals(method.getName())) {
                        query.set((StoreSalesTrendQuery) args[0]);
                        return new StoreSalesTrendPage(days);
                    }
                    if ("salesReport".equals(method.getName())) {
                        return new StoreSalesPage(products, 1, 100, 3, 24, BigDecimal.TEN);
                    }
                    return null;
                });
        AtomicReference<StoreSalesTrendPanel> holder =
                new AtomicReference<StoreSalesTrendPanel>();
        SwingUtilities.invokeAndWait(() -> holder.set(
                new StoreSalesTrendPanel(page(), service)));
        StoreSalesTrendPanel panel = holder.get();
        AsyncPagedTableRefreshTest.await(() -> panel.trendChart().itemCount() == 19);
        assertEquals(3, panel.pieChart().itemCount());
        assertTrue(panel.trendChart().getPreferredSize().width >= 19 * 88);

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        query.set(null);
        SwingUtilities.invokeAndWait(() -> {
            panel.startDateField().setDate(start);
            panel.endDateField().setDate(end);
            click(panel, "查询");
        });
        AsyncPagedTableRefreshTest.await(() -> query.get() != null
                && start.equals(query.get().getStartDate()));
        assertEquals(end, query.get().getEndDate());
    }

    private static List<StoreSalesTrendDto> days(int count) {
        List<StoreSalesTrendDto> values = new ArrayList<StoreSalesTrendDto>();
        LocalDate first = LocalDate.of(2026, 8, 1);
        for (int i = 0; i < count; i++) {
            values.add(new StoreSalesTrendDto(first.plusDays(i), i + 1,
                    BigDecimal.valueOf((i + 1) * 10L)));
        }
        return values;
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
