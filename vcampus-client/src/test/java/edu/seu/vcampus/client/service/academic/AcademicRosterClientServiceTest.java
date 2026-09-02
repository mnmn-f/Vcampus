package edu.seu.vcampus.client.service.academic;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 教务客户端按稳定命令请求并反序列化课程花名册。 */
public final class AcademicRosterClientServiceTest {
    @Test public void courseRosterUsesStableCommandAndRequest() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        AcademicClientService service = new AcademicClientService(new NetworkClientService(gateway));
        CourseRosterEntryDto entry = new CourseRosterEntryDto(31L, 7L, "S007", "学生",
                "学院", "专业", "班级", "ENROLLED", LocalDateTime.now());
        CourseRosterDto expected = new CourseRosterDto(19L, Collections.singletonList(entry));
        gateway.payload = expected;

        CourseRosterDto actual = service.courseRoster(19L);

        assertSame(expected, actual);
        assertEquals(AcademicCommands.COURSE_ROSTER, gateway.request.getCommand());
        assertEquals(19L, ((CourseRosterRequest) gateway.request.getPayload()).getCourseId());
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
