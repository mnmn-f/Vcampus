package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.DataTableToolbar;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

public class StoreAccountSearchTest {
    @Test public void accountSearchPassesKeywordAndShowsUsefulColumns() throws Exception {
        AtomicReference<AccountLedgerQuery> query = new AtomicReference<AccountLedgerQuery>();
        AccountTransactionDto row = new AccountTransactionDto(12, 1, "PURCHASE", new BigDecimal("-25.00"),
                new BigDecimal("100.00"), new BigDecimal("75.00"), "STORE_ORDER", 8L, "key", 1L,
                "开学用品", LocalDateTime.of(2026, 9, 14, 12, 30));
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(StoreClientService.class.getClassLoader(),
                new Class<?>[]{StoreClientService.class}, (proxy, method, args) -> {
                    if ("getAccount".equals(method.getName())) return new AccountDto(1, 1, BigDecimal.TEN, "ACTIVE");
                    if ("getAccountLedger".equals(method.getName())) { query.set((AccountLedgerQuery) args[0]); return new AccountLedgerPage(Collections.singletonList(row), 1, 20, 1); }
                    return null;
                });
        AtomicReference<StoreAccountPanel> panel = new AtomicReference<StoreAccountPanel>();
        AsyncPagedTableRefreshTest.edt(() -> panel.set(new StoreAccountPanel(new BasePage(new ClientSession(), "商店", "") { }, service)));
        AsyncPagedTable<?> table = field(panel.get(), "ledger");
        AsyncPagedTableRefreshTest.await(() -> table.getTable().getRowCount() == 1);
        DataTableToolbar toolbar = field(table, "toolbar");
        AsyncPagedTableRefreshTest.edt(() -> { toolbar.getSearchField().setText("充值"); toolbar.getSearchButton().doClick(); });
        AsyncPagedTableRefreshTest.await(() -> query.get() != null && "充值".equals(query.get().getKeyword()));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("商店订单", table.getTable().getValueAt(0, table.getTable().getColumn("关联业务").getModelIndex()));
            assertEquals("开学用品", table.getTable().getValueAt(0, table.getTable().getColumn("备注").getModelIndex()));
        });
    }
    @SuppressWarnings("unchecked") private static <T> T field(Object target, String name) throws Exception {
        java.lang.reflect.Field value = target.getClass().getDeclaredField(name); value.setAccessible(true); return (T) value.get(target);
    }
}
