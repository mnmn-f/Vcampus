package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;

import static org.junit.Assert.assertEquals;

/** 客户端商店服务只发送会话令牌并映射 Common DTO。 */
public final class NetworkStoreClientServiceTest {
    @Test
    public void productSearchUsesCurrentSessionToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        NetworkStoreClientService service = new NetworkStoreClientService(
                new NetworkClientService(gateway), session);
        ProductDto expected = new ProductDto(1L, "SKU-1", "商品", null,
                new BigDecimal("2.00"), 3, "ON_SALE");
        gateway.payload = new edu.seu.vcampus.common.dto.store.ProductPage(
                java.util.Collections.singletonList(expected), 1, 20, 1L);

        assertEquals(1L, service.searchProducts(null).getTotal());
        assertEquals(StoreCommands.PRODUCT_SEARCH, gateway.lastRequest.getCommand());
        assertEquals("token-7", gateway.lastRequest.getSessionToken());
    }

    @Test
    public void salesReportMapsCommandQueryAndStrongType() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(8L, "manager", "管理员", Role.STORE_MANAGER, "token-8"));
        NetworkStoreClientService service = new NetworkStoreClientService(
                new NetworkClientService(gateway), session);
        StoreSalesPage expected = new StoreSalesPage(java.util.Collections.<edu.seu.vcampus.common.dto.store.StoreSalesDto>emptyList(),
                2, 10, 0L, 0L, BigDecimal.ZERO);
        StoreSalesQuery query = new StoreSalesQuery(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), "coffee", 2, 10);
        gateway.payload = expected;

        assertEquals(expected, service.salesReport(query));
        assertEquals(StoreCommands.SALES_REPORT, gateway.lastRequest.getCommand());
        assertEquals(query, gateway.lastRequest.getPayload());
        assertEquals("token-8", gateway.lastRequest.getSessionToken());
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message lastRequest;
        private Object payload;

        @Override public Message send(Message request) {
            lastRequest = request;
            return Message.success(request, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
