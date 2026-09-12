package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.network.TcpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 通过真实 TCP ObjectStream 验证登录、业务、切换职责和登出链路。 */
public final class TcpProtocolSecurityIntegrationTest {
    private IntegrationFixture fixture;
    private TcpServer server;

    @Before
    public void setUp() throws Exception {
        fixture = new IntegrationFixture();
        server = new TcpServer(0, 4, fixture.router);
        server.startAsync();
        assertTrue(server.isRunning());
        assertTrue(server.getBoundPort() > 0);
    }

    @After
    public void tearDown() {
        if (server != null) server.stop();
    }

    @Test
    public void tcpLoginBusinessSwitchAndLogoutUseOneSession() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(3000);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            Message login = exchange(output, input, Message.request(Commands.AUTH_LOGIN, null,
                    new LoginRequest(IntegrationFixture.MULTI_ACCOUNT,
                            IntegrationFixture.MULTI_PASSWORD)));
            assertTrue(login.isSuccess());
            LoginResult result = (LoginResult) login.getPayload();
            assertNotNull(result);
            String token = result.getSessionToken();

            Message business = exchange(output, input, Message.request(
                    StoreCommands.PRODUCT_SEARCH, token, new ProductQuery()));
            assertTrue(business.isSuccess());
            assertTrue(exchange(output, input, Message.request(Commands.AUTH_SWITCH_ROLE,
                    token, new SwitchRoleRequest(Role.STORE_MANAGER))).isSuccess());
            Message update = exchange(output, input, Message.request(StoreCommands.PRODUCT_UPDATE,
                    token, product("TCP修改")));
            assertTrue(update.isSuccess());

            assertTrue(exchange(output, input,
                    Message.request(Commands.AUTH_LOGOUT, token, null)).isSuccess());
            Message afterLogout = exchange(output, input, Message.request(
                    StoreCommands.PRODUCT_SEARCH, token, new ProductQuery()));
            assertEquals(ResultCodes.UNAUTHORIZED, afterLogout.getResultCode());
        }
    }

    @Test
    public void twoTcpClientsReadTheSameServerStateAfterManagerUpdate() throws Exception {
        try (Connection manager = connect(); Connection student = connect()) {
            String managerToken = login(manager, IntegrationFixture.MULTI_ACCOUNT);
            assertTrue(manager.exchange(Message.request(Commands.AUTH_SWITCH_ROLE, managerToken,
                    new SwitchRoleRequest(Role.STORE_MANAGER))).isSuccess());
            String studentToken = login(student, "student2");

            assertTrue(manager.exchange(Message.request(StoreCommands.PRODUCT_UPDATE,
                    managerToken, product("局域网同步商品"))).isSuccess());
            Message refreshed = student.exchange(Message.request(StoreCommands.PRODUCT_SEARCH,
                    studentToken, new ProductQuery("局域网同步商品", null, "ON_SALE", 1, 20)));
            assertTrue(refreshed.isSuccess());
            edu.seu.vcampus.common.dto.store.ProductPage page =
                    (edu.seu.vcampus.common.dto.store.ProductPage) refreshed.getPayload();
            assertEquals("局域网同步商品", page.getItems().get(0).getName());
        }
    }

    private Connection connect() throws Exception {
        return new Connection(new Socket("127.0.0.1", server.getBoundPort()));
    }

    @Test public void threeConnectionsShareCartPaymentAndShippingWithoutDoubleDebit() throws Exception {
        try (Connection manager = connect(); Connection first = connect(); Connection second = connect()) {
            String m = login(manager, IntegrationFixture.MULTI_ACCOUNT), a = login(first, "student2"), b = login(second, "student2");
            assertTrue(manager.exchange(Message.request(Commands.AUTH_SWITCH_ROLE, m, new SwitchRoleRequest(Role.STORE_MANAGER))).isSuccess());
            assertTrue(first.exchange(Message.request(StoreCommands.CART_ADD_ITEM, a, new edu.seu.vcampus.common.dto.store.CartItemRequest(1, 1))).isSuccess());
            edu.seu.vcampus.common.dto.store.CartDto cart = (edu.seu.vcampus.common.dto.store.CartDto)
                    second.exchange(Message.request(StoreCommands.CART_GET, b, null)).getPayload();
            assertEquals(1, cart.getItems().size());
            assertTrue(second.exchange(Message.request(StoreCommands.CART_REMOVE_ITEM, b, Long.valueOf(1))).isSuccess());
            cart = (edu.seu.vcampus.common.dto.store.CartDto) first.exchange(Message.request(StoreCommands.CART_GET, a, null)).getPayload();
            assertTrue(cart.getItems().isEmpty());
            assertTrue(first.exchange(Message.request(StoreCommands.CART_ADD_ITEM, a, new edu.seu.vcampus.common.dto.store.CartItemRequest(1, 1))).isSuccess());
            edu.seu.vcampus.common.dto.store.OrderDto order = (edu.seu.vcampus.common.dto.store.OrderDto)
                    second.exchange(Message.request(StoreCommands.ORDER_CREATE, b, null)).getPayload();
            assertTrue(first.exchange(Message.request(StoreCommands.ORDER_PAY, a,
                    new edu.seu.vcampus.common.dto.store.PaymentRequest(order.getId(), "tcp-once"))).isSuccess());
            assertEquals(ResultCodes.CONFLICT, second.exchange(Message.request(StoreCommands.ORDER_PAY, b,
                    new edu.seu.vcampus.common.dto.store.PaymentRequest(order.getId(), "tcp-twice"))).getResultCode());
            edu.seu.vcampus.common.dto.store.AccountDto account = (edu.seu.vcampus.common.dto.store.AccountDto)
                    second.exchange(Message.request(StoreCommands.ACCOUNT_GET, b, null)).getPayload();
            assertEquals(0, new BigDecimal("95.00").compareTo(account.getBalance()));
            assertTrue(manager.exchange(Message.request(StoreCommands.ORDER_SHIPPING_UPDATE, m,
                    new edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest(order.getId(), "SHIPPED", "TCP-TRACK", "已交快递"))).isSuccess());
            order = (edu.seu.vcampus.common.dto.store.OrderDto) second.exchange(Message.request(StoreCommands.ORDER_DETAIL, b, Long.valueOf(order.getId()))).getPayload();
            assertEquals("SHIPPED", order.getShippingStatus()); assertEquals("TCP-TRACK", order.getTrackingNo());
            assertTrue(first.exchange(Message.request(Commands.AUTH_LOGOUT, a, null)).isSuccess());
            assertTrue(second.exchange(Message.request(StoreCommands.ACCOUNT_GET, b, null)).isSuccess());
        }
    }

    private static String login(Connection connection, String account) throws Exception {
        Message response = connection.exchange(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest(account, IntegrationFixture.MULTI_PASSWORD)));
        assertTrue(response.isSuccess());
        return ((LoginResult) response.getPayload()).getSessionToken();
    }

    private static final class Connection implements AutoCloseable {
        private final Socket socket;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;
        private Connection(Socket socket) throws Exception {
            this.socket = socket; socket.setSoTimeout(3000);
            output = new ObjectOutputStream(socket.getOutputStream()); output.flush();
            input = new ObjectInputStream(socket.getInputStream());
        }
        private Message exchange(Message request) throws Exception {
            return TcpProtocolSecurityIntegrationTest.exchange(output, input, request);
        }
        @Override public void close() throws Exception { socket.close(); }
    }

    private static Message exchange(ObjectOutputStream output, ObjectInputStream input,
                                     Message request) throws Exception {
        output.writeObject(request);
        output.flush();
        output.reset();
        return (Message) input.readObject();
    }

    private static ProductWriteRequest product(String name) {
        return new ProductWriteRequest(1L, "SKU-1", name, "测试", null,
                new BigDecimal("5.00"), 1, "ON_SALE");
    }
}
