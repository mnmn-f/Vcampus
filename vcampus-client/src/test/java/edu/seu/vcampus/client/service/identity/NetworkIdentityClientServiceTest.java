package edu.seu.vcampus.client.service.identity;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 身份客户端仅负责命令、令牌和 Common DTO 映射。 */
public final class NetworkIdentityClientServiceTest {
    @Test public void registrationDoesNotRequireLoginAndOtherCommandsUseToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        NetworkIdentityClientService service = new NetworkIdentityClientService(
                new NetworkClientService(gateway), session);
        gateway.payload = new ProfileDto(8L, "new_user", "新用户", "ACTIVE",
                java.util.Collections.singleton(Role.STUDENT));
        assertEquals(8L, service.register(new RegistrationRequest(
                "new_user", "NewUser123", "新用户")).getUserId());
        assertEquals(IdentityCommands.REGISTER, gateway.lastRequest.getCommand());
        assertNull(gateway.lastRequest.getSessionToken());

        session.open(new LoginResult(8L, "new_user", "新用户", Role.STUDENT, "token-8"));
        gateway.payload = new ProfileDto(8L, "new_user", "新用户", "ACTIVE",
                java.util.Collections.singleton(Role.STUDENT));
        service.getOwnProfile();
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
