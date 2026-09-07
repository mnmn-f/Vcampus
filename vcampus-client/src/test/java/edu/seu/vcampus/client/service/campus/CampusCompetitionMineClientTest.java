package edu.seu.vcampus.client.service.campus;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class CampusCompetitionMineClientTest {
    @Test public void currentStudentRegistrationQueryUsesSessionScopedCommand() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        CampusPageQuery query = new CampusPageQuery(1, 100, null, null);
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token"));
        NetworkCampusClientService service = new NetworkCampusClientService(new NetworkClientService(gateway), session);
        CampusPage<CompetitionRegistrationDto> result = service.myCompetitionRegistrations(query);
        assertEquals(0L, result.getTotalElements());
        assertEquals(CampusCommands.COMPETITION_MINE, gateway.request.getCommand());
        assertSame(query, gateway.request.getPayload());
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        @Override public Message send(Message value) {
            request = value;
            return Message.success(value, new CampusPage<CompetitionRegistrationDto>(1, 100, 0,
                    Collections.<CompetitionRegistrationDto>emptyList()));
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
