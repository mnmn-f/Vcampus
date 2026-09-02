package edu.seu.vcampus.server.dorm.ext.handler;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/** 宿舍扩展命令的统一适配器；业务规则留在服务层。 */
public final class DormExtCommandHandler implements CommandHandler {
    private static final Logger LOGGER = Logger.getLogger(DormExtCommandHandler.class.getName());

    private final String command;
    private final DormExtService service;

    public DormExtCommandHandler(String command, DormExtService service) {
        if (command == null || service == null) {
            throw new IllegalArgumentException("dorm ext handler dependencies required");
        }
        this.command = command;
        this.service = service;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            Object payload = request == null ? null : request.getPayload();
            if (DormExtCommands.STATUS.equals(command)) {
                return Message.success(request, service.status(session));
            }
            if (DormExtCommands.METER_LIST.equals(command)) {
                return Message.success(request, service.meterReadings(session, query(payload)));
            }
            if (DormExtCommands.METER_SUBMIT.equals(command)) {
                return Message.success(request,
                        service.saveMeterReading(session, meter(payload)));
            }
            if (DormExtCommands.BILL_GENERATE.equals(command)) {
                return Message.success(request, service.generateBills(session, bill(payload)));
            }
            if (DormExtCommands.WARNING_SCAN.equals(command)) {
                return Message.success(request, service.scanAbsences(session, scan(payload)));
            }
            if (DormExtCommands.WARNING_LIST.equals(command)) {
                return Message.success(request, service.warnings(session, query(payload)));
            }
            if (DormExtCommands.WARNING_NOTIFY.equals(command)) {
                return Message.success(request, service.notifyWarning(session, handle(payload)));
            }
            if (DormExtCommands.WARNING_VERIFY.equals(command)) {
                return Message.success(request, service.verifyWarning(session, handle(payload)));
            }
            if (DormExtCommands.WARNING_CONFIG_GET.equals(command)) {
                return Message.success(request, service.warningConfig(session));
            }
            if (DormExtCommands.WARNING_CONFIG_SET.equals(command)) {
                return Message.success(request, service.saveWarningConfig(session, config(payload)));
            }
            if (DormExtCommands.VISITOR_SUBMIT.equals(command)) {
                return Message.success(request, service.submitVisitor(session, visitor(payload)));
            }
            if (DormExtCommands.VISITOR_MINE.equals(command)) {
                return Message.success(request, service.ownVisitors(session, query(payload)));
            }
            if (DormExtCommands.VISITOR_CANCEL.equals(command)) {
                return Message.success(request, service.cancelVisitor(session, audit(payload)));
            }
            if (DormExtCommands.VISITOR_LIST.equals(command)) {
                return Message.success(request, service.visitors(session, query(payload)));
            }
            if (DormExtCommands.VISITOR_AUDIT.equals(command)) {
                return Message.success(request, service.auditVisitor(session, audit(payload)));
            }
            if (DormExtCommands.HYGIENE_SUBMIT.equals(command)) {
                return Message.success(request, service.submitHygiene(session, hygiene(payload)));
            }
            if (DormExtCommands.HYGIENE_DETAIL.equals(command)) {
                return Message.success(request, service.hygieneDetail(session, detail(payload)));
            }
            if (DormExtCommands.HYGIENE_TASK_GENERATE.equals(command)) {
                return Message.success(request,
                        service.generateHygieneTasks(session, taskRequest(payload)));
            }
            if (DormExtCommands.HYGIENE_TASK_LIST.equals(command)) {
                return Message.success(request, service.hygieneTasks(session, query(payload)));
            }
            if (DormExtCommands.SCHEDULER_RUN.equals(command)) {
                return Message.success(request, service.runScheduledTask(session, taskName(payload)));
            }
            if (DormExtCommands.NOTICE_MINE.equals(command)) {
                return Message.success(request, service.myNotices(session, query(payload)));
            }
            if (DormExtCommands.NOTICE_LIST.equals(command)) {
                return Message.success(request, service.notices(session, query(payload)));
            }
            if (DormExtCommands.NOTICE_EXTRA_SET.equals(command)) {
                return Message.success(request,
                        service.saveNoticeExtra(session, noticeExtra(payload)));
            }
            if (DormExtCommands.STAY_MINE.equals(command)) {
                return Message.success(request, service.myStayStatus(session));
            }
            if (DormExtCommands.STAY_LIST.equals(command)) {
                return Message.success(request, service.stayStatuses(session));
            }
            if (DormExtCommands.ACCESS_MINE.equals(command)) {
                return Message.success(request, service.myAccessRecords(session, query(payload)));
            }
            if (DormExtCommands.ACCESS_POLICY_GET.equals(command)) {
                return Message.success(request, service.accessPolicy(session));
            }
            if (DormExtCommands.ACCESS_POLICY_SET.equals(command)) {
                return Message.success(request, service.saveAccessPolicy(session, policy(payload)));
            }
            if (DormExtCommands.REPAIR_PERMIT_SET.equals(command)) {
                return Message.success(request, service.setRepairPermit(session, permit(payload)));
            }
            if (DormExtCommands.REPAIR_PERMIT_MINE.equals(command)) {
                return Message.success(request, service.myRepairPermits(session, query(payload)));
            }
            if (DormExtCommands.ROOM_DELETE.equals(command)) {
                return Message.success(request, service.deleteRoom(session, roomDelete(payload)));
            }
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的宿舍扩展操作");
        } catch (DormException ex) {
            // 业务异常本身不带 cause；带 cause 的是服务层兜底包装的未预期异常，
            // 只回给客户端一句笼统提示会让排查无从下手，因此在服务端留下堆栈。
            if (ex.getCause() != null) {
                LOGGER.log(Level.WARNING, "[dorm-ext] " + command + " 执行失败", ex.getCause());
            }
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, DormExtCommands.INVALID_INPUT, "请求参数格式不正确");
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "[dorm-ext] " + command + " 未预期异常", ex);
            return Message.failure(request, DormExtCommands.INTERNAL_ERROR, "宿舍扩展服务暂时不可用");
        }
    }

    @Override
    public Permission requiredPermission() {
        // 学生自助与宿管治理走不同权限；仍然全部复用宿舍模块已登记的权限，
        // 没有新增 Permission 枚举值，因此 Permission 与 RolePolicy 都不必改动。
        if (DormExtCommands.VISITOR_SUBMIT.equals(command)
                || DormExtCommands.VISITOR_CANCEL.equals(command)) {
            return Permission.DORM_REQUEST;
        }
        if (DormExtCommands.VISITOR_MINE.equals(command)
                || DormExtCommands.STAY_MINE.equals(command)
                || DormExtCommands.ACCESS_MINE.equals(command)
                || DormExtCommands.REPAIR_PERMIT_MINE.equals(command)
                || DormExtCommands.NOTICE_MINE.equals(command)) {
            return Permission.DORM_SELF_READ;
        }
        if (DormExtCommands.REPAIR_PERMIT_SET.equals(command)) {
            return Permission.DORM_REQUEST;
        }
        if (DormExtCommands.ROOM_DELETE.equals(command)) {
            return Permission.DORM_MANAGE;
        }
        if (DormExtCommands.VISITOR_LIST.equals(command)
                || DormExtCommands.VISITOR_AUDIT.equals(command)) {
            return Permission.DORM_APPROVE;
        }
        return Permission.DORM_GOVERN;
    }

    @Override
    public boolean requiresAuthentication() { return true; }

    private static DormPageQuery query(Object value) {
        if (value == null) return DormPageQuery.all();
        if (value instanceof DormPageQuery) return (DormPageQuery) value;
        throw new IllegalArgumentException("payload must be DormPageQuery");
    }

    private static BillGenerateRequest bill(Object value) {
        if (value instanceof BillGenerateRequest) return (BillGenerateRequest) value;
        throw new IllegalArgumentException("payload must be BillGenerateRequest");
    }

    private static AccessPolicyRequest policy(Object value) {
        if (value instanceof AccessPolicyRequest) return (AccessPolicyRequest) value;
        throw new IllegalArgumentException("payload must be AccessPolicyRequest");
    }

    private static RepairEntryPermitRequest permit(Object value) {
        if (value instanceof RepairEntryPermitRequest) return (RepairEntryPermitRequest) value;
        throw new IllegalArgumentException("payload must be RepairEntryPermitRequest");
    }

    private static String taskName(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        throw new IllegalArgumentException("payload must be String");
    }

    private static NoticeExtraRequest noticeExtra(Object value) {
        if (value instanceof NoticeExtraRequest) return (NoticeExtraRequest) value;
        throw new IllegalArgumentException("payload must be NoticeExtraRequest");
    }

    private static RoomDeleteRequest roomDelete(Object value) {
        if (value instanceof RoomDeleteRequest) return (RoomDeleteRequest) value;
        throw new IllegalArgumentException("payload must be RoomDeleteRequest");
    }

    private static HygieneScoreSubmitRequest hygiene(Object value) {
        if (value instanceof HygieneScoreSubmitRequest) return (HygieneScoreSubmitRequest) value;
        throw new IllegalArgumentException("payload must be HygieneScoreSubmitRequest");
    }

    private static HygieneDetailRequest detail(Object value) {
        if (value instanceof HygieneDetailRequest) return (HygieneDetailRequest) value;
        throw new IllegalArgumentException("payload must be HygieneDetailRequest");
    }

    private static HygieneTaskGenerateRequest taskRequest(Object value) {
        if (value == null) return new HygieneTaskGenerateRequest(null, null);
        if (value instanceof HygieneTaskGenerateRequest) return (HygieneTaskGenerateRequest) value;
        throw new IllegalArgumentException("payload must be HygieneTaskGenerateRequest");
    }

    private static VisitorRegistrationRequest visitor(Object value) {
        if (value instanceof VisitorRegistrationRequest) return (VisitorRegistrationRequest) value;
        throw new IllegalArgumentException("payload must be VisitorRegistrationRequest");
    }

    private static VisitorAuditRequest audit(Object value) {
        if (value instanceof VisitorAuditRequest) return (VisitorAuditRequest) value;
        throw new IllegalArgumentException("payload must be VisitorAuditRequest");
    }

    private static WarningScanRequest scan(Object value) {
        if (value == null) return new WarningScanRequest(null);
        if (value instanceof WarningScanRequest) return (WarningScanRequest) value;
        throw new IllegalArgumentException("payload must be WarningScanRequest");
    }

    private static WarningHandleRequest handle(Object value) {
        if (value instanceof WarningHandleRequest) return (WarningHandleRequest) value;
        throw new IllegalArgumentException("payload must be WarningHandleRequest");
    }

    private static WarningConfigRequest config(Object value) {
        if (value instanceof WarningConfigRequest) return (WarningConfigRequest) value;
        throw new IllegalArgumentException("payload must be WarningConfigRequest");
    }

    private static MeterReadingRequest meter(Object value) {
        if (value instanceof MeterReadingRequest) return (MeterReadingRequest) value;
        throw new IllegalArgumentException("payload must be MeterReadingRequest");
    }
}
