package edu.seu.vcampus.client.service.academic;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.ScheduleIdRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import org.junit.Test;

import org.threeten.bp.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 排课客户端只负责稳定命令、请求体和 Common DTO 映射。 */
public final class AcademicScheduleClientServiceTest {
    @Test public void scheduleCrudUsesStableCommandsAndIds() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        AcademicClientService service = new AcademicClientService(new NetworkClientService(gateway));
        CourseScheduleDto expected = new CourseScheduleDto(41L, 7L, 2, 3, 4,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                new ClassroomDto(12L, "教一", "101", "普通", 80, "AVAILABLE"));
        gateway.payload = expected;
        ScheduleSaveRequest create = ScheduleSaveRequest.create(7L, 2, 3, 4,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), 12L);
        assertSame(expected, service.createSchedule(create));
        assertEquals(AcademicCommands.SCHEDULE_CREATE, gateway.request.getCommand());
        assertSame(create, gateway.request.getPayload());

        ScheduleSaveRequest update = ScheduleSaveRequest.update(41L, 7L, 2, 5, 6,
                create.getStartDate(), create.getEndDate(), 12L);
        assertSame(expected, service.updateSchedule(update));
        assertEquals(AcademicCommands.SCHEDULE_UPDATE, gateway.request.getCommand());
        assertSame(update, gateway.request.getPayload());

        gateway.payload = null;
        service.deleteSchedule(41L);
        assertEquals(AcademicCommands.SCHEDULE_DELETE, gateway.request.getCommand());
        assertEquals(41L, ((ScheduleIdRequest) gateway.request.getPayload()).getScheduleId());
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        private Object payload;
        @Override public Message send(Message value) { request = value; return Message.success(value, (java.io.Serializable) payload); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
