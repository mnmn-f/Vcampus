package edu.seu.vcampus.server.dorm.handler;

import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormAssignmentRequest;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;


/** 宿舍命令统一适配器，业务规则留在服务层。 */
public final class DormCommandHandler implements CommandHandler {
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(DormCommandHandler.class.getName());
    private final String command;
    private final DormService service;

    public DormCommandHandler(String command, DormService service) {
        if (command == null || service == null) throw new IllegalArgumentException("dorm handler dependencies required");
        this.command = command;
        this.service = service;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            Object p = request == null ? null : request.getPayload();
            if (DormCommands.BUILDING_LIST.equals(command)) return Message.success(request, service.buildings(session, query(p)));
            if (DormCommands.ROOM_LIST.equals(command)) return Message.success(request, service.rooms(session, query(p)));
            if (DormCommands.BED_LIST.equals(command)) return Message.success(request, service.beds(session, query(p)));
            if (DormCommands.BUILDING_CREATE.equals(command)) return Message.success(request, service.createBuilding(session, payload(p, DormBuildingWriteRequest.class)));
            if (DormCommands.BUILDING_UPDATE.equals(command)) return Message.success(request, service.updateBuilding(session, payload(p, DormBuildingWriteRequest.class)));
            if (DormCommands.ROOM_CREATE.equals(command)) return Message.success(request, service.createRoom(session, payload(p, DormRoomWriteRequest.class)));
            if (DormCommands.ROOM_UPDATE.equals(command)) return Message.success(request, service.updateRoom(session, payload(p, DormRoomWriteRequest.class)));
            if (DormCommands.BED_CREATE.equals(command)) return Message.success(request, service.createBed(session, payload(p, DormBedWriteRequest.class)));
            if (DormCommands.BED_UPDATE.equals(command)) return Message.success(request, service.updateBed(session, payload(p, DormBedWriteRequest.class)));
            if (DormCommands.ACCOMMODATION_MINE.equals(command)) return Message.success(request, service.mine(session));
            if (DormCommands.ACCOMMODATION_ASSIGN.equals(command)) return Message.success(request, service.assign(session, payload(p, DormAssignmentRequest.class)));
            if (DormCommands.ACCOMMODATION_TRANSFER.equals(command)) return Message.success(request, service.transfer(session, payload(p, DormAssignmentRequest.class)));
            if (DormCommands.ACCOMMODATION_CHECKOUT.equals(command)) return Message.success(request, service.checkout(session, payload(p, DormAssignmentRequest.class)));
            if (DormCommands.REQUEST_SUBMIT.equals(command)) return Message.success(request, service.submitRequest(session, payload(p, AccommodationRequest.class)));
            if (DormCommands.REQUEST_LIST.equals(command)) return Message.success(request, service.requests(session, query(p), student(p)));
            if (DormCommands.REQUEST_APPROVE.equals(command)) return Message.success(request, service.approveRequest(session, payload(p, DormApprovalRequest.class)));
            if (DormCommands.ACCESS_RECORD.equals(command)) return Message.success(request, service.recordAccess(session, payload(p, AccessRecordRequest.class)));
            if (DormCommands.ACCESS_LIST.equals(command)) return Message.success(request, service.access(session, query(p), student(p)));
            if (DormCommands.ALERT_LIST.equals(command)) return Message.success(request, service.alerts(session, query(p), student(p)));
            if (DormCommands.ALERT_HANDLE.equals(command)) return Message.success(request, service.handleAlert(session, payload(p, LateReturnHandleRequest.class)));
            if (DormCommands.HYGIENE_LIST.equals(command)) return Message.success(request, service.hygiene(session, query(p)));
            if (DormCommands.HYGIENE_SAVE.equals(command)) return Message.success(request, service.saveHygiene(session, payload(p, HygieneInspectionRequest.class)));
            if (DormCommands.REPAIR_LIST.equals(command)) return Message.success(request, service.repairs(session, query(p)));
            if (DormCommands.REPAIR_CREATE.equals(command)) return Message.success(request, service.createRepair(session, payload(p, RepairCreateRequest.class)));
            if (DormCommands.REPAIR_UPDATE.equals(command)) return Message.success(request, service.updateRepair(session, payload(p, RepairStatusRequest.class)));
            if (DormCommands.REPAIR_EVALUATE.equals(command)) return Message.success(request, service.evaluateRepair(session, payload(p, RepairEvaluationRequest.class)));
            if (DormCommands.LEAVE_SUBMIT.equals(command)) return Message.success(request, service.submitLeave(session, payload(p, LeaveSubmitRequest.class)));
            if (DormCommands.LEAVE_MINE.equals(command)) return Message.success(request, service.ownLeaves(session, leaveQuery(p)));
            if (DormCommands.LEAVE_CANCEL.equals(command)) return Message.success(request, service.cancelLeave(session, payload(p, LeaveCancelRequest.class)));
            if (DormCommands.LEAVE_LIST.equals(command)) return Message.success(request, service.manageLeaves(session, leaveQuery(p)));
            if (DormCommands.LEAVE_REVIEW.equals(command)) return Message.success(request, service.reviewLeave(session, payload(p, LeaveReviewRequest.class)));
            if (DormCommands.LEAVE_APPROVE.equals(command)) return Message.success(request, service.reviewLeave(session, review(p, true)));
            if (DormCommands.LEAVE_REJECT.equals(command)) return Message.success(request, service.reviewLeave(session, review(p, false)));
            if (DormCommands.UTILITY_MINE.equals(command)) return Message.success(request, service.bills(session, query(p)));
            if (DormCommands.UTILITY_MANAGER_LIST.equals(command)) return Message.success(request, service.managerBills(session, utilityQuery(p)));
            if (DormCommands.UTILITY_PAY.equals(command)) return Message.success(request, service.payBill(session, payload(p, UtilityPaymentRequest.class)));
            if (DormCommands.ANNOUNCEMENT_LIST.equals(command)) return Message.success(request, service.announcements(session, query(p)));
            if (DormCommands.ANNOUNCEMENT_SAVE.equals(command)) return Message.success(request, service.saveAnnouncement(session, payload(p, AnnouncementSaveRequest.class)));
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的宿舍操作");
        } catch (DormException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, DormCommands.INVALID_INPUT, "请求参数格式不正确");
        } catch (RuntimeException ex) {
            // 这一档兜的是服务层之外的意外（路由、序列化…）。不记日志的话，界面上那句
            // 「暂时不可用」就是唯一的线索，谁也查不出真正坏在哪儿。
            LOGGER.log(java.util.logging.Level.SEVERE, command + " unexpected failure", ex);
            return Message.failure(request, DormCommands.INTERNAL_ERROR, "宿舍服务暂时不可用");
        }
    }

    @Override public Permission requiredPermission() {
        if (DormCommands.ACCOMMODATION_MINE.equals(command) || DormCommands.UTILITY_MINE.equals(command)) return Permission.DORM_SELF_READ;
        if (DormCommands.UTILITY_MANAGER_LIST.equals(command)) return Permission.DORM_GOVERN;
        if (DormCommands.ACCOMMODATION_ASSIGN.equals(command) || DormCommands.ACCOMMODATION_TRANSFER.equals(command) || DormCommands.ACCOMMODATION_CHECKOUT.equals(command)) return Permission.DORM_MANAGE;
        if (DormCommands.BUILDING_CREATE.equals(command) || DormCommands.BUILDING_UPDATE.equals(command)
                || DormCommands.ROOM_CREATE.equals(command) || DormCommands.ROOM_UPDATE.equals(command)
                || DormCommands.BED_CREATE.equals(command) || DormCommands.BED_UPDATE.equals(command)) return Permission.DORM_MANAGE;
        if (DormCommands.REQUEST_SUBMIT.equals(command) || DormCommands.REPAIR_CREATE.equals(command)) return Permission.DORM_REQUEST;
        if (DormCommands.REPAIR_EVALUATE.equals(command) || DormCommands.LEAVE_SUBMIT.equals(command)
                || DormCommands.LEAVE_CANCEL.equals(command)) return Permission.DORM_REQUEST;
        if (DormCommands.REQUEST_APPROVE.equals(command)) return Permission.DORM_APPROVE;
        if (DormCommands.LEAVE_LIST.equals(command) || DormCommands.LEAVE_REVIEW.equals(command)
                || DormCommands.LEAVE_APPROVE.equals(command) || DormCommands.LEAVE_REJECT.equals(command)) return Permission.DORM_APPROVE;
        if (DormCommands.ALERT_HANDLE.equals(command) || DormCommands.HYGIENE_LIST.equals(command) || DormCommands.HYGIENE_SAVE.equals(command)) return Permission.DORM_GOVERN;
        if (DormCommands.UTILITY_PAY.equals(command)) return Permission.DORM_BILL_PAY;
        if (DormCommands.ANNOUNCEMENT_SAVE.equals(command)) return Permission.ANNOUNCEMENT_MANAGE;
        return null;
    }

    @Override public boolean requiresAuthentication() { return true; }

    private static DormPageQuery query(Object value) {
        if (value == null) return DormPageQuery.all();
        if (value instanceof DormPageQuery) return (DormPageQuery) value;
        if (value instanceof DormQuery) return ((DormQuery) value).getPage();
        throw new IllegalArgumentException("payload must be DormPageQuery");
    }
    private static Long student(Object value) {
        return value instanceof DormQuery ? ((DormQuery) value).getStudentUserId() : null;
    }
    private static UtilityBillQuery utilityQuery(Object value) {
        if (value == null) return UtilityBillQuery.all();
        if (value instanceof UtilityBillQuery) return (UtilityBillQuery) value;
        throw new IllegalArgumentException("payload must be UtilityBillQuery");
    }
    private static LeaveQuery leaveQuery(Object value) {
        if (value == null) return new LeaveQuery();
        if (value instanceof LeaveQuery) {
            return (LeaveQuery) value;
        }
        throw new IllegalArgumentException("payload must be LeaveQuery");
    }
    private static LeaveReviewRequest review(Object value, boolean approved) {
        if (value instanceof LeaveReviewRequest) {
            LeaveReviewRequest request = (LeaveReviewRequest) value;
            if (request.isApproved() != approved) throw new IllegalArgumentException("review action mismatch");
            return request;
        }
        if (value instanceof DormApprovalRequest) {
            DormApprovalRequest request = (DormApprovalRequest) value;
            if (request.isApproved() != approved) throw new IllegalArgumentException("review action mismatch");
            return new LeaveReviewRequest(request.getRequestId(), approved, request.getRemark());
        }
        throw new IllegalArgumentException("payload must be leave review request");
    }
    private static <T> T payload(Object value, Class<T> type) {
        if (!type.isInstance(value)) throw new IllegalArgumentException("payload must be " + type.getSimpleName());
        return type.cast(value);
    }
}
