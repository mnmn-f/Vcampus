package edu.seu.vcampus.server.dorm.ext.handler;

import edu.seu.vcampus.common.protocol.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.dorm.service.*;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;
import java.util.logging.*;

/** Uniform protocol adapter; validation and business rules stay in DormExtService. */
public final class DormExtCommandHandler implements CommandHandler {
    private static final Logger LOGGER = Logger.getLogger(DormExtCommandHandler.class.getName());
    private final String command;
    private final DormExtService service;
    public DormExtCommandHandler(String command, DormExtService service) {
        if (command == null || service == null) throw new IllegalArgumentException("dorm ext handler dependencies required");
        this.command = command; this.service = service;
    }
    @Override public Message handle(Message request, SessionContext session) {
        try { return dispatch(request, session, request == null ? null : request.getPayload()); }
        catch (DormException ex) { if (ex.getCause() != null) LOGGER.log(Level.WARNING, command + " failed", ex.getCause()); return Message.failure(request, ex.getResultCode(), ex.getUserMessage()); }
        catch (IllegalArgumentException ex) { return Message.failure(request, DormExtCommands.INVALID_INPUT, "请求参数格式不正确"); }
        catch (RuntimeException ex) { LOGGER.log(Level.WARNING, command + " unexpected failure", ex); return Message.failure(request, DormExtCommands.INTERNAL_ERROR, "宿舍扩展服务暂时不可用"); }
    }
    private Message dispatch(Message request, SessionContext s, Object p) {
        if (DormExtCommands.STATUS.equals(command)) return Message.success(request, service.status(s));
        if (DormExtCommands.METER_LIST.equals(command)) return Message.success(request, service.meterReadings(s, DormExtPayloads.query(p)));
        if (DormExtCommands.METER_SUBMIT.equals(command)) return Message.success(request, service.saveMeterReading(s, DormExtPayloads.meter(p)));
        if (DormExtCommands.BILL_GENERATE.equals(command)) return Message.success(request, service.generateBills(s, DormExtPayloads.bill(p)));
        if (DormExtCommands.WARNING_SCAN.equals(command)) return Message.success(request, service.scanAbsences(s, DormExtPayloads.scan(p)));
        if (DormExtCommands.WARNING_LIST.equals(command)) return Message.success(request, service.warnings(s, DormExtPayloads.query(p)));
        if (DormExtCommands.WARNING_NOTIFY.equals(command)) return Message.success(request, service.notifyWarning(s, DormExtPayloads.handle(p)));
        if (DormExtCommands.WARNING_VERIFY.equals(command)) return Message.success(request, service.verifyWarning(s, DormExtPayloads.handle(p)));
        if (DormExtCommands.WARNING_CONFIG_GET.equals(command)) return Message.success(request, service.warningConfig(s));
        if (DormExtCommands.WARNING_CONFIG_SET.equals(command)) return Message.success(request, service.saveWarningConfig(s, DormExtPayloads.config(p)));
        if (DormExtCommands.VISITOR_SUBMIT.equals(command)) return Message.success(request, service.submitVisitor(s, DormExtPayloads.visitor(p)));
        if (DormExtCommands.VISITOR_MINE.equals(command)) return Message.success(request, service.ownVisitors(s, DormExtPayloads.query(p)));
        if (DormExtCommands.VISITOR_CANCEL.equals(command)) return Message.success(request, service.cancelVisitor(s, DormExtPayloads.audit(p)));
        if (DormExtCommands.VISITOR_LIST.equals(command)) return Message.success(request, service.visitors(s, DormExtPayloads.query(p)));
        if (DormExtCommands.VISITOR_AUDIT.equals(command)) return Message.success(request, service.auditVisitor(s, DormExtPayloads.audit(p)));
        if (DormExtCommands.HYGIENE_SUBMIT.equals(command)) return Message.success(request, service.submitHygiene(s, DormExtPayloads.hygiene(p)));
        if (DormExtCommands.HYGIENE_DETAIL.equals(command)) return Message.success(request, service.hygieneDetail(s, DormExtPayloads.detail(p)));
        if (DormExtCommands.HYGIENE_TASK_GENERATE.equals(command)) return Message.success(request, service.generateHygieneTasks(s, DormExtPayloads.task(p)));
        if (DormExtCommands.HYGIENE_TASK_LIST.equals(command)) return Message.success(request, service.hygieneTasks(s, DormExtPayloads.query(p)));
        if (DormExtCommands.SCHEDULER_RUN.equals(command)) return Message.success(request, service.runScheduledTask(s, DormExtPayloads.taskName(p)));
        if (DormExtCommands.NOTICE_MINE.equals(command)) return Message.success(request, service.myNotices(s, DormExtPayloads.query(p)));
        if (DormExtCommands.NOTICE_LIST.equals(command)) return Message.success(request, service.notices(s, DormExtPayloads.query(p)));
        if (DormExtCommands.NOTICE_EXTRA_SET.equals(command)) return Message.success(request, service.saveNoticeExtra(s, DormExtPayloads.notice(p)));
        if (DormExtCommands.STAY_MINE.equals(command)) return Message.success(request, service.myStayStatus(s));
        if (DormExtCommands.STAY_LIST.equals(command)) return Message.success(request, service.stayStatuses(s));
        if (DormExtCommands.ACCESS_MINE.equals(command)) return Message.success(request, service.myAccessRecords(s, DormExtPayloads.query(p)));
        if (DormExtCommands.ACCESS_POLICY_GET.equals(command)) return Message.success(request, service.accessPolicy(s));
        if (DormExtCommands.ACCESS_POLICY_SET.equals(command)) return Message.success(request, service.saveAccessPolicy(s, DormExtPayloads.policy(p)));
        if (DormExtCommands.REPAIR_PERMIT_SET.equals(command)) return Message.success(request, service.setRepairPermit(s, DormExtPayloads.permit(p)));
        if (DormExtCommands.REPAIR_PERMIT_MINE.equals(command)) return Message.success(request, service.myRepairPermits(s, DormExtPayloads.query(p)));
        if (DormExtCommands.ROOM_DELETE.equals(command)) return Message.success(request, service.deleteRoom(s, DormExtPayloads.room(p)));
        return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的宿舍扩展操作");
    }
    @Override public Permission requiredPermission() {
        if (DormExtCommands.VISITOR_SUBMIT.equals(command) || DormExtCommands.VISITOR_CANCEL.equals(command) || DormExtCommands.REPAIR_PERMIT_SET.equals(command)) return Permission.DORM_REQUEST;
        if (DormExtCommands.VISITOR_MINE.equals(command) || DormExtCommands.STAY_MINE.equals(command) || DormExtCommands.ACCESS_MINE.equals(command) || DormExtCommands.REPAIR_PERMIT_MINE.equals(command) || DormExtCommands.NOTICE_MINE.equals(command)) return Permission.DORM_SELF_READ;
        if (DormExtCommands.ROOM_DELETE.equals(command)) return Permission.DORM_MANAGE;
        if (DormExtCommands.VISITOR_LIST.equals(command) || DormExtCommands.VISITOR_AUDIT.equals(command)) return Permission.DORM_APPROVE;
        return Permission.DORM_GOVERN;
    }
    @Override public boolean requiresAuthentication() { return true; }
}
