package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class StoreShippingServiceTest {
    @Test public void managerAdvancesShippingAndDeliveryCompletesOrder() throws Exception {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addOrder(new OrderDto(1L, "O-1", 10L, new BigDecimal("20.00"),
                "PAID", Collections.emptyList()));
        StoreService service = new StoreService(repository, new InMemoryStoreTransactionRunner(repository));
        SessionContext manager = session(99L, Role.STORE_MANAGER);
        assertEquals("SHIPPED", service.updateOrderShipping(manager,
                new OrderShippingUpdateRequest(1L, "SHIPPED", "YT001", "已交承运方")).getShippingStatus());
        try {
            service.updateOrderShipping(manager, new OrderShippingUpdateRequest(1L, "PREPARING", null, null));
            fail("shipping must not move backwards");
        } catch (StoreServiceException ex) { assertEquals(ResultCodes.CONFLICT, ex.getResultCode()); }
        OrderDto delivered = service.updateOrderShipping(manager,
                new OrderShippingUpdateRequest(1L, "DELIVERED", "YT001", "已送达"));
        assertEquals("DELIVERED", delivered.getShippingStatus());
        assertEquals("COMPLETED", delivered.getStatus());
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token", id, "user", "用户", EnumSet.of(role), role);
    }
}
