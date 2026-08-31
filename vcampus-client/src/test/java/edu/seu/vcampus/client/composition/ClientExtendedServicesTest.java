package edu.seu.vcampus.client.composition;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Campus 与 Identity 服务共享组合根、会话令牌和请求边界。 */
public final class ClientExtendedServicesTest {
    @Test
    public void campusAndIdentityShareNetworkAndActiveToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        ClientBusinessServices services = new ClientBusinessServices(new NetworkClientService(gateway), session);

        assertEquals(0L, services.campus().announcements(new CampusAnnouncementQuery()).getTotalElements());
        assertEquals(7L, services.identity().getOwnProfile().getUserId());
        assertEquals(CampusCommands.ANNOUNCEMENT_LIST, gateway.commands.get(0));
        assertEquals(IdentityCommands.PROFILE_SELF, gateway.commands.get(1));
        assertEquals("token-7", gateway.tokens.get(0)); assertEquals("token-7", gateway.tokens.get(1));
    }

    private static final class RecordingGateway implements ClientGateway {
        private final List<String> commands = new ArrayList<String>(); private final List<String> tokens = new ArrayList<String>();
        @Override public Message send(Message request) {
            commands.add(request.getCommand()); tokens.add(request.getSessionToken());
            if (CampusCommands.ANNOUNCEMENT_LIST.equals(request.getCommand())) return Message.success(request,
                    new CampusPage<Object>(1, 20, 0, Collections.emptyList()));
            return Message.success(request, new ProfileDto(7L, "student", "学生", "ACTIVE",
                    Collections.singleton(Role.STUDENT)));
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
