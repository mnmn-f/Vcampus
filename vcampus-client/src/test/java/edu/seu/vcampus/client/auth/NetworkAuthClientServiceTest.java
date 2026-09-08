package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class NetworkAuthClientServiceTest {
    @Test
    public void loginUsesServerMatchedAccountNameIdAndRoles() throws Exception {
        LoginResult serverIdentity = new LoginResult(20230001L, "20230001", "张三",
                EnumSet.of(Role.STUDENT, Role.TEACHER), Role.STUDENT, "server-token");
        RecordingGateway gateway = new RecordingGateway(serverIdentity);
        NetworkAuthClientService service = new NetworkAuthClientService(gateway);

        LoginResult result = service.login(" 20230001 ", "secret123");

        assertEquals("auth.login", gateway.request.getCommand());
        LoginRequest request = (LoginRequest) gateway.request.getPayload();
        assertEquals("20230001", request.getAccount());
        assertEquals("secret123", request.getPassword());
        assertEquals(20230001L, result.getUserId());
        assertEquals("20230001", result.getAccount());
        assertEquals("张三", result.getDisplayName());
        assertEquals(Role.STUDENT, result.getActiveRole());
        assertTrue(result.getRoles().contains(Role.TEACHER));
    }

    @Test
    public void roleSwitchUsesOnlyRolesReturnedAfterLogin() throws Exception {
        LoginResult initial = new LoginResult(7L, "teacher-7", "李老师",
                EnumSet.of(Role.TEACHER, Role.ACADEMIC_ADMIN), Role.TEACHER, "token-7");
        RecordingGateway gateway = new RecordingGateway(initial);
        NetworkAuthClientService service = new NetworkAuthClientService(gateway);
        service.login("teacher-7", "secret123");

        assertEquals(Role.ACADEMIC_ADMIN, service.switchRole(Role.ACADEMIC_ADMIN));
        assertEquals(Commands.AUTH_SWITCH_ROLE, gateway.request.getCommand());
        SwitchRoleRequest request = (SwitchRoleRequest) gateway.request.getPayload();
        assertEquals(Role.ACADEMIC_ADMIN, request.getRole());
    }

    private static final class RecordingGateway implements ClientGateway {
        private final LoginResult identity;
        private Message request;

        private RecordingGateway(LoginResult identity) {
            this.identity = identity;
        }

        @Override public Message send(Message request) {
            this.request = request;
            if (Commands.AUTH_LOGIN.equals(request.getCommand())) {
                return Message.success(request, identity);
            }
            return Message.success(request, Role.ACADEMIC_ADMIN);
        }

        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
