package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.EnumSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** 新建商品编码由服务端生成，且连续创建不会重复。 */
public final class StoreAutomaticSkuTest {
    @Test public void serverGeneratesUniqueSkuWhenClientLeavesItBlank() throws Exception {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(7L, "SEU-STATIONERY-001", "中性笔",
                "STATIONERY", null, BigDecimal.TEN, 20, "ON_SALE"));
        StoreService service = new StoreService(repository,
                new InMemoryStoreTransactionRunner(repository));
        ProductDto first = service.createProduct(manager(), request("文件袋"));
        ProductDto second = service.createProduct(manager(), request("笔记本"));
        assertTrue(first.getSku().matches("SEU-STATIONERY-[0-9]{3}"));
        assertTrue(second.getSku().matches("SEU-STATIONERY-[0-9]{3}"));
        assertEquals("SEU-STATIONERY-002", first.getSku());
        assertEquals("SEU-STATIONERY-003", second.getSku());
        assertNotEquals(first.getSku(), second.getSku());
    }

    private static ProductWriteRequest request(String name) {
        return new ProductWriteRequest(0L, null, name, "STATIONERY", null,
                BigDecimal.TEN, 20, "DRAFT");
    }

    private static SessionContext manager() {
        return new SessionContext("store-manager", 9L, "manager", "商店管理员",
                EnumSet.of(Role.STORE_MANAGER), Role.STORE_MANAGER);
    }
}
