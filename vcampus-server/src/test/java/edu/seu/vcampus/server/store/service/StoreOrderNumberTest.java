package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 订单号包含下单日和当日流水，且同日创建严格递增。 */
public final class StoreOrderNumberTest {
    @Test
    public void formatsReadableDailyOrderNumber() {
        assertEquals("VC-20260914-0027",
                StoreOrderNumber.create(LocalDate.of(2026, 9, 14), 27));
    }

    @Test
    public void rejectsOutOfRangeSequence() {
        try {
            StoreOrderNumber.create(LocalDate.of(2026, 9, 14), 0);
            fail("zero sequence must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("1到9999"));
        }
    }

    @Test
    public void serviceAllocatesConsecutiveNumbersAcrossOrders() throws Exception {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(1L, "SKU-1", "校园笔记本", "STATIONERY",
                null, new BigDecimal("10.00"), 10, "ON_SALE"));
        StoreService service = new StoreService(repository,
                new InMemoryStoreTransactionRunner(repository));
        SessionContext student = new SessionContext("student-token", 10L, "student10",
                "王晨茜", EnumSet.of(Role.STUDENT), Role.STUDENT);

        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto first = service.createOrder(student);
        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto second = service.createOrder(student);

        String prefix = "VC-" + LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        assertEquals(prefix + "0001", first.getOrderNo());
        assertEquals(prefix + "0002", second.getOrderNo());
    }
}
