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
