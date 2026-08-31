package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 销售统计只纳入已支付状态，日期、关键字和数据范围保持稳定。 */
public final class StoreSalesServiceTest {
    private StoreService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(1L, "SKU-1", "纪念杯", "文创", null,
                new BigDecimal("10.00"), 5, "ON_SALE"));
        repository.addProduct(new ProductDto(2L, "SKU-2", "校园贴纸", "文创", null,
                new BigDecimal("5.00"), 5, "ON_SALE"));
        add(repository, 1L, "PAID", at(10), at(10), item(1L, "纪念杯", 2, "20.00"));
        add(repository, 2L, "COMPLETED", at(23, 59, 59), at(23, 59, 59),
                item(1L, "纪念杯", 1, "10.00"), item(2L, "校园贴纸", 3, "15.00"));
        add(repository, 3L, "CREATED", null, at(11), item(1L, "纪念杯", 9, "90.00"));
        add(repository, 4L, "CANCELLED", at(12), at(12), item(2L, "校园贴纸", 9, "45.00"));
        add(repository, 5L, "REFUNDED", at(13), at(13), item(1L, "纪念杯", 9, "90.00"));
        add(repository, 6L, "PAID", at(0, 0, 0).plusDays(1), at(0, 0, 0).plusDays(1),
                item(1L, "纪念杯", 7, "70.00"));
        service = new StoreService(repository, new InMemoryStoreTransactionRunner(repository));
        manager = session(99L, Role.STORE_MANAGER);
        student = session(10L, Role.STUDENT);
    }

    @Test
    public void paidAndCompletedRowsRespectDateBoundaryAndSummary() throws Exception {
        StoreSalesPage page = service.salesReport(manager,
                new StoreSalesQuery(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 29),
                        null, null, 1, 20));
        assertEquals(2L, page.getTotal());
        assertEquals(6L, page.getTotalQuantity());
        assertEquals(new BigDecimal("45.00"), page.getTotalAmount());
        assertEquals(1L, page.getItems().get(0).getProductId());
        assertEquals(3L, page.getItems().get(0).getQuantitySold());
    }

    @Test
    public void productAndKeywordFiltersAndEmptyPageAreConsistent() throws Exception {
        StoreSalesPage filtered = service.salesReport(manager,
                new StoreSalesQuery(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 29),
                        Long.valueOf(2L), "SKU-2", 1, 20));
        assertEquals(1L, filtered.getTotal());
        assertEquals(new BigDecimal("15.00"), filtered.getItems().get(0).getSalesAmount());
        StoreSalesPage empty = service.salesReport(manager,
                new StoreSalesQuery(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                        null, null, 1, 20));
        assertEquals(0L, empty.getTotal());
        assertEquals(BigDecimal.ZERO, empty.getTotalAmount());
    }

    @Test
    public void studentCannotReadManagerSalesReport() throws Exception {
        try {
            service.salesReport(student, null);
            fail("student must not read sales report");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private static void add(InMemoryStoreRecordRepository repository, long id, String status,
                            LocalDateTime paid, LocalDateTime created, OrderItemDto... items) {
        repository.addOrder(new OrderDto(id, "ORDER-" + id, 10L, sum(items), status, created, paid,
                null, "COMPLETED".equals(status) ? paid : null, Arrays.asList(items)));
    }

    private static OrderItemDto item(long id, String name, int quantity, String amount) {
        return new OrderItemDto(id, name, new BigDecimal(amount).divide(BigDecimal.valueOf(quantity)),
                quantity, new BigDecimal(amount));
    }

    private static BigDecimal sum(OrderItemDto... items) {
        BigDecimal value = BigDecimal.ZERO;
        for (OrderItemDto item : items) value = value.add(item.getLineAmount());
        return value;
    }

    private static LocalDateTime at(int hour) { return at(hour, 0, 0); }
    private static LocalDateTime at(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 8, 29, hour, minute, second);
    }
    private static SessionContext session(long id, Role role) {
        return new SessionContext("sales-" + id, id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
