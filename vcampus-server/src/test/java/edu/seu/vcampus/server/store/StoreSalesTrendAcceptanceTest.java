package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 销售趋势按自然日只统计 PAID/COMPLETED 订单及其实付金额。 */
public final class StoreSalesTrendAcceptanceTest {
    private StoreService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(1L, "SKU-1", "校园杯", "CULTURE", null,
                new BigDecimal("10.00"), 20, "ON_SALE"));
        repository.addOrder(order(1L, "PAID", 2, "20.00", at(10)));
        repository.addOrder(order(2L, "COMPLETED", 3, "15.00", at(23, 59, 59)));
        repository.addOrder(order(3L, "CREATED", 9, "90.00", null));
        repository.addOrder(order(4L, "REFUNDED", 4, "40.00", at(12)));
        service = new StoreService(repository, new InMemoryStoreTransactionRunner(repository));
        manager = session(99L, Role.STORE_MANAGER);
        student = session(10L, Role.STUDENT);
    }

    @Test
    public void trendGroupsOnlyPaidAndCompletedRowsByPaidDate() throws Exception {
        StoreSalesTrendPage page = service.salesTrend(manager,
                new StoreSalesTrendQuery(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 29)));
        assertEquals(1, page.getItems().size());
        assertEquals(LocalDate.of(2026, 8, 29), page.getItems().get(0).getDate());
        assertEquals(5L, page.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("35.00"), page.getItems().get(0).getAmount());
    }

    @Test
    public void studentsCannotReadSalesTrend() throws Exception {
        try {
            service.salesTrend(student, new StoreSalesTrendQuery());
            fail("student must not read sales trend");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private static OrderDto order(long id, String status, int quantity, String total,
                                  LocalDateTime paid) {
        LocalDateTime created = paid == null ? at(11) : paid;
        return new OrderDto(id, "TREND-" + id, 10L, new BigDecimal(total), status,
                created, paid, null, "COMPLETED".equals(status) ? paid : null,
                Collections.singletonList(new OrderItemDto(1L, "校园杯",
                        new BigDecimal("10.00"), quantity)));
    }

    private static LocalDateTime at(int hour) { return at(hour, 0, 0); }
    private static LocalDateTime at(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 8, 29, hour, minute, second);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("trend-" + id, id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
