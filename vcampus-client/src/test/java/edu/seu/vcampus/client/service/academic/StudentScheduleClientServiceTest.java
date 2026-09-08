package edu.seu.vcampus.client.service.academic;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 学生课表客户端只传学期条件，学生身份由会话提供。 */
public final class StudentScheduleClientServiceTest {
    @Test
    public void scheduleQueryUsesStableCommandAndSessionToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        AcademicClientService service = new AcademicClientService(
                new NetworkClientService(gateway), session());
        StudentScheduleDto expected = new StudentScheduleDto(7L, "2026-FALL",
                Collections.<CourseDto>emptyList());
        gateway.payload = expected;
        StudentScheduleQuery query = new StudentScheduleQuery("2026-FALL");
        assertSame(expected, service.studentSchedule(query));
        assertEquals(AcademicCommands.STUDENT_SCHEDULE, gateway.request.getCommand());
        assertSame(query, gateway.request.getPayload());
        assertEquals("token-7", gateway.request.getSessionToken());
    }

    private static ClientSession session() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        return session;
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        private Object payload;
        @Override public Message send(Message value) {
            request = value; return Message.success(value, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
