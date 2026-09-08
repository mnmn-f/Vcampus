package edu.seu.vcampus.client.service.dorm;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/** 新宿舍命令只使用会话令牌，并把响应映射为精确 DTO 类型。 */
public final class NetworkDormClientServiceTest {
    @Test
    public void newCommandsMapToExpectedCommandsAndTypes() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkDormClientService service = service(gateway);
        DormBuildingDto building = new DormBuildingDto(1L, "B1", "一号楼", "校内", "MIXED", "OPEN");
        DormRoomDto room = new DormRoomDto(2L, 1L, "B1", "一号楼", "101", 1, 4,
                "STANDARD", "OPEN", null, 0);
        DormBedDto bed = new DormBedDto(3L, 2L, "B1", "101", "01", "AVAILABLE", null);
        RepairOrderDto repair = new RepairOrderDto(4L, 2L, 7L, "WATER", "漏水", "NORMAL",
                "COMPLETED", null, null, null, null, null, null);
        LeaveRequestDto leave = leave(5L);

        gateway.payload = building;
        assertSame(building, service.createBuilding(new DormBuildingWriteRequest("B1", "一号楼", "校内", "MIXED", "OPEN")));
        assertCommand(gateway, DormCommands.BUILDING_CREATE);
        gateway.payload = building;
        assertSame(building, service.updateBuilding(new DormBuildingWriteRequest(1L, "B1", "一号楼", "校内", "MIXED", "OPEN")));
        assertCommand(gateway, DormCommands.BUILDING_UPDATE);
        gateway.payload = room;
        assertSame(room, service.createRoom(new DormRoomWriteRequest(1L, "101", 1, 4, "STANDARD", "OPEN", null)));
        assertCommand(gateway, DormCommands.ROOM_CREATE);
        gateway.payload = room;
        assertSame(room, service.updateRoom(new DormRoomWriteRequest(2L, 1L, "101", 1, 4, "STANDARD", "OPEN", null)));
        assertCommand(gateway, DormCommands.ROOM_UPDATE);
        gateway.payload = bed;
        assertSame(bed, service.createBed(new DormBedWriteRequest(2L, "01", "AVAILABLE")));
        assertCommand(gateway, DormCommands.BED_CREATE);
        gateway.payload = bed;
        assertSame(bed, service.updateBed(new DormBedWriteRequest(3L, 2L, "01", "AVAILABLE")));
        assertCommand(gateway, DormCommands.BED_UPDATE);
        gateway.payload = repair;
        assertSame(repair, service.evaluateRepair(new RepairEvaluationRequest(4L, 5, "满意")));
        assertCommand(gateway, DormCommands.REPAIR_EVALUATE);
        gateway.payload = leave;
        assertSame(leave, service.submitLeave(new LeaveSubmitRequest("PERSONAL", at(1), at(2), "事假")));
        assertCommand(gateway, DormCommands.LEAVE_SUBMIT);
        gateway.payload = leave;
        assertSame(leave, service.cancelLeave(new LeaveCancelRequest(5L)));
        assertCommand(gateway, DormCommands.LEAVE_CANCEL);
        gateway.payload = leave;
        assertSame(leave, service.reviewLeave(new LeaveReviewRequest(5L, true, "通过")));
        assertCommand(gateway, DormCommands.LEAVE_REVIEW);
    }

    @Test
    public void leaveQueriesStripCallerSuppliedStudentId() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkDormClientService service = service(gateway);
        DormPage<LeaveRequestDto> expected = new DormPage<LeaveRequestDto>(
                1, 20, 1L, Collections.singletonList(leave(5L)));
        LeaveQuery query = new LeaveQuery(1, 20, "PENDING", 99L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));
        gateway.payload = expected;

        assertSame(expected, service.myLeaves(query));
        assertEquals(DormCommands.LEAVE_MINE, gateway.lastRequest.getCommand());
        assertEquals(null, ((LeaveQuery) gateway.lastRequest.getPayload()).getStudentUserId());
        assertSame(expected, service.managerLeaves(query));
        assertEquals(DormCommands.LEAVE_LIST, gateway.lastRequest.getCommand());
        assertEquals(Long.valueOf(99L), ((LeaveQuery) gateway.lastRequest.getPayload()).getStudentUserId());
    }

    @Test
    public void wrongResponseTypeIsRejected() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkDormClientService service = service(gateway);
        gateway.payload = new DormBuildingDto(1L, "B1", "一号楼", null, "MIXED", "OPEN");
        try {
            service.evaluateRepair(new RepairEvaluationRequest(4L, 5, null));
            fail("wrong DTO must be rejected");
        } catch (NetworkClientException ex) {
            assertEquals(ResultCodes.INTERNAL_ERROR, ex.getCode());
        }
    }

    private static NetworkDormClientService service(RecordingGateway gateway) {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        return new NetworkDormClientService(new NetworkClientService(gateway), session);
    }

    private static LeaveRequestDto leave(long id) {
        return new LeaveRequestDto(id, 7L, "PERSONAL", at(1), at(2), "事假",
                "PENDING", null, null, null, at(0));
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 1, 1, hour, 0);
    }

    private static void assertCommand(RecordingGateway gateway, String command) {
        assertEquals(command, gateway.lastRequest.getCommand());
        assertEquals("token-7", gateway.lastRequest.getSessionToken());
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
