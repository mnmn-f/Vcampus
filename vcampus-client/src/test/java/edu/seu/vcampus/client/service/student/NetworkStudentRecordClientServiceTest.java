package edu.seu.vcampus.client.service.student;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** 客户端只传消息并映射 DTO，同时每次请求使用当前会话令牌。 */
public class NetworkStudentRecordClientServiceTest {
    @Test
    public void selfProfileSendsCurrentSessionTokenAndMapsPayload() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkClientService network = new NetworkClientService(gateway);
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        NetworkStudentRecordClientService service =
                new NetworkStudentRecordClientService(network, session);
        StudentProfileDto expected = new StudentProfileDto(7L, "学生", "student", "S007",
                "软件工程", "软件工程", "软工2601", 2026, 2030, "UNDERGRADUATE",
                "UNKNOWN", null, null, null, null, StudentStatus.ENROLLED);
        gateway.payload = expected;

        StudentProfileDto actual = service.getOwnProfile();

        assertEquals(expected.getStudentNo(), actual.getStudentNo());
        assertEquals(StudentCommands.SELF_PROFILE, gateway.lastRequest.getCommand());
        assertEquals("token-7", gateway.lastRequest.getSessionToken());
        assertNull(gateway.lastRequest.getPayload());
    }

    @Test
    public void profileSearchReusesCommandAndPassesEveryCondition() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(9L, "registrar", "教务", Role.REGISTRAR, "token-9"));
        NetworkStudentRecordClientService service = new NetworkStudentRecordClientService(
                new NetworkClientService(gateway), session);
        StudentProfileQuery query = new StudentProfileQuery("S001", "学生一", "电气工程学院",
                "电气工程及其自动化", "电气2601", StudentStatus.ENROLLED, 2, 10);
        gateway.payload = new StudentProfilePage(java.util.Collections.<StudentProfileDto>emptyList(),
                0L, 2, 10);

        service.searchProfiles(query);

        assertEquals(StudentCommands.PROFILE_SEARCH, gateway.lastRequest.getCommand());
        assertEquals("token-9", gateway.lastRequest.getSessionToken());
        assertSame(query, gateway.lastRequest.getPayload());
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message lastRequest;
        private Object payload;

        @Override
        public Message send(Message request) {
            lastRequest = request;
            return Message.success(request, (java.io.Serializable) payload);
        }

        @Override
        public boolean isConnected() { return true; }

        @Override
        public void close() { }
    }
}
