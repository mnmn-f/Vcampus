package edu.seu.vcampus.client.service.academic;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public final class StudentEnrollmentsClientServiceTest {
    @Test public void requestUsesSessionIdentityAndNoUserIdPayload() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        StudentEnrollmentListDto expected = new StudentEnrollmentListDto(
                Collections.<StudentEnrollmentDto>emptyList());
        gateway.payload = expected;
        AcademicClientService service = new AcademicClientService(
                new NetworkClientService(gateway), session());
        assertSame(expected, service.studentEnrollments());
        assertEquals(AcademicCommands.STUDENT_ENROLLMENTS, gateway.request.getCommand());
        assertEquals("token-7", gateway.request.getSessionToken());
        assertNull(gateway.request.getPayload());
    }

    private static ClientSession session() {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        return value;
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        private Object payload;
        @Override public Message send(Message value) {
            request = value;
            return Message.success(value, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
