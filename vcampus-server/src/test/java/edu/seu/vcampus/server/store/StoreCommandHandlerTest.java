package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import edu.seu.vcampus.server.store.registry.StoreCommandRegistry;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/** 路由权限及 activeRole 只授权测试。 */
public final class StoreCommandHandlerTest {
    @Test
    public void inactiveManagerRoleCannotMaintainProduct() {
        InMemoryStoreRecordRepository repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(1L, "S1", "商品", null, null,
                new BigDecimal("1.00"), 1, "ON_SALE"));
        StoreService service = new StoreService(repository,
                new InMemoryStoreTransactionRunner(repository));
        SessionManager sessions = new SessionManager();
        SessionContext session = sessions.createSession(10L, "multi", "多角色",
                EnumSet.of(Role.STUDENT, Role.STORE_MANAGER), Role.STUDENT);
        CommandRouter router = StoreCommandRegistry.registerAll(new CommandRouter(sessions), service);
        Message response = router.route(Message.request(StoreCommands.PRODUCT_UPDATE,
                session.getSessionToken(), new edu.seu.vcampus.common.dto.store.ProductWriteRequest(
                        1L, "S1", "新名", null, null, new BigDecimal("1.00"), 1, "ON_SALE")));
        assertEquals(ResultCodes.FORBIDDEN, response.getResultCode());
    }
}
