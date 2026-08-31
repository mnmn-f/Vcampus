package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionRevokeRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 跨模块命令路由的 activeRole、数据归属和协议错误边界。 */
public final class CrossModuleSecurityIntegrationTest {
    private IntegrationFixture fixture;

    @Before
    public void setUp() {
        fixture = new IntegrationFixture();
    }

    @Test
    public void activeRoleChangesMenuPermissionsAndSystemAdminStaysBusinessReadOnly() {
        String token = login(IntegrationFixture.MULTI_ACCOUNT,
                IntegrationFixture.MULTI_PASSWORD);
        ProductWriteRequest edit = product("改名");

        Message read = route(StoreCommands.PRODUCT_SEARCH, token, null);
        assertTrue(read.isSuccess());
        assertEquals(ResultCodes.FORBIDDEN,
                route(StoreCommands.PRODUCT_UPDATE, token, edit).getResultCode());

        Message switched = route(Commands.AUTH_SWITCH_ROLE, token,
                new SwitchRoleRequest(Role.STORE_MANAGER));
        assertTrue(switched.isSuccess());
        assertTrue(route(StoreCommands.PRODUCT_UPDATE, token, edit).isSuccess());

        assertTrue(route(Commands.AUTH_SWITCH_ROLE, token,
                new SwitchRoleRequest(Role.SYSTEM_ADMIN)).isSuccess());
        assertEquals(ResultCodes.FORBIDDEN,
                route(StoreCommands.PRODUCT_UPDATE, token, edit).getResultCode());
        assertEquals(ResultCodes.FORBIDDEN,
                route(StoreCommands.PRODUCT_SEARCH, token, null).getResultCode());
    }

    @Test
    public void forgedBuyerIdDoesNotChangeOwnOrderScope() {
        String token = login(IntegrationFixture.MULTI_ACCOUNT,
                IntegrationFixture.MULTI_PASSWORD);
        OrderQuery forged = new OrderQuery(null, 2L, null, 1, 20);
        Message response = route(StoreCommands.ORDER_MINE, token, forged);

        assertTrue(response.isSuccess());
        OrderPage page = (OrderPage) response.getPayload();
        assertEquals(1L, page.getTotalElements());
        OrderDto order = page.getItems().get(0);
        assertEquals(1L, order.getBuyerId());
    }

    @Test
    public void unknownMalformedAndWrongPayloadReturnStableCodes() {
        String token = login(IntegrationFixture.MULTI_ACCOUNT,
                IntegrationFixture.MULTI_PASSWORD);
        assertEquals("COMMON.UNKNOWN_COMMAND",
                route("integration.unknown", token, "payload").getResultCode());
        assertEquals(ResultCodes.INVALID_INPUT,
                route(StudentCommands.SELF_GRADES, token, "not-a-query").getResultCode());
        assertEquals(ResultCodes.INVALID_INPUT,
                route(StoreCommands.PRODUCT_DETAIL, token, "not-an-id").getResultCode());
        Message malformed = fixture.router.route(Message.request(null, token, null));
        assertNotNull(malformed);
        assertEquals("COMMON.MALFORMED_REQUEST", malformed.getResultCode());
    }

    @Test
    public void adminListsAndRevokesFreshLoginWithoutIdentityObservation() {
        String target = login("student2", IntegrationFixture.MULTI_PASSWORD);
        String admin = login("sysadmin", IntegrationFixture.MULTI_PASSWORD);
        SessionPage sessions = (SessionPage) route(IdentityCommands.SESSION_LIST,
                admin, null).getPayload();
        SessionDto targetSession = findSession(sessions, 2L);
        assertNotNull(targetSession);

        Message revoked = route(IdentityCommands.SESSION_REVOKE, admin,
                new SessionRevokeRequest(targetSession.getId()));
        assertTrue(revoked.isSuccess());
        assertFalse(fixture.sessions.contains(target));
        assertEquals(ResultCodes.UNAUTHORIZED,
                route(IdentityCommands.PROFILE_SELF, target, null).getResultCode());
    }

    private Message route(String command, String token, java.io.Serializable payload) {
        return fixture.router.route(Message.request(command, token, payload));
    }

    private String login(String account, String password) {
        Message response = fixture.router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest(account, password)));
        assertTrue(response.isSuccess());
        return ((LoginResult) response.getPayload()).getSessionToken();
    }

    private static ProductWriteRequest product(String name) {
        return new ProductWriteRequest(1L, "SKU-1", name, "测试",
                null, new BigDecimal("5.00"), 1, "ON_SALE");
    }

    private static SessionDto findSession(SessionPage page, long userId) {
        for (SessionDto value : page.getItems()) {
            if (value.getUserId() == userId) return value;
        }
        return null;
    }
}
