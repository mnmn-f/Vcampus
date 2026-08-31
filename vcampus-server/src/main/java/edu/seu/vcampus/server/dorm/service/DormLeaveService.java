package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormLeaveType;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.Locale;

/** 请假提交、本人查询取消及宿管审核；时间段采用 [start,end) 重叠语义。 */
final class DormLeaveService extends DormServiceSupport {
    private final DormRepository repository;

    DormLeaveService(DormRepository repository, TransactionManager transactions) {
        super(transactions); this.repository = repository;
    }

    LeaveRequestDto submit(final SessionContext s, LeaveSubmitRequest r) {
        require(s, Permission.DORM_REQUEST); final LeaveSubmitRequest value = submitRequest(r);
        return serialized(new Work<LeaveRequestDto>() {
            @Override public LeaveRequestDto run(java.sql.Connection c) throws Exception {
                repository.lockStudent(c, s.getUserId());
                if (repository.hasOverlap(c, s.getUserId(), value.getStartAt(), value.getEndAt(), null)) throw new DormRepositoryException(DormCommands.LEAVE_OVERLAP, "请假时间与已有申请重叠");
                return repository.submit(c, s.getUserId(), value);
            }
        });
    }

    DormPage<LeaveRequestDto> mine(final SessionContext s, final LeaveQuery q) {
        require(s, Permission.DORM_SELF_READ); final LeaveQuery value = query(q);
        return execute(new Work<DormPage<LeaveRequestDto>>() { public DormPage<LeaveRequestDto> run(java.sql.Connection c) throws Exception { return repository.list(c, Long.valueOf(s.getUserId()), value); } });
    }

    DormPage<LeaveRequestDto> manage(final SessionContext s, final LeaveQuery q) {
        require(s, Permission.DORM_APPROVE); final LeaveQuery value = query(q);
        return execute(new Work<DormPage<LeaveRequestDto>>() { public DormPage<LeaveRequestDto> run(java.sql.Connection c) throws Exception { return repository.list(c, null, value); } });
    }

    LeaveRequestDto cancel(final SessionContext s, final LeaveCancelRequest r) {
        require(s, Permission.DORM_REQUEST);
        if (r == null || r.getLeaveId() <= 0) throw invalid("请假申请编号不正确");
        return serialized(new Work<LeaveRequestDto>() {
            @Override public LeaveRequestDto run(java.sql.Connection c) throws Exception {
                LeaveRequestDto old = repository.lock(c, r.getLeaveId());
                if (old == null) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "请假申请不存在");
                if (old.getStudentUserId() != s.getUserId()) throw new DormRepositoryException(ResultCodes.FORBIDDEN, "只能取消本人请假申请");
                return repository.cancel(c, r.getLeaveId());
            }
        });
    }

    LeaveRequestDto review(final SessionContext s, final LeaveReviewRequest r) {
        require(s, Permission.DORM_APPROVE);
        if (r == null || r.getLeaveId() <= 0) throw invalid("请假申请编号不正确");
        if (r.getRemark() != null && r.getRemark().length() > 500) throw invalid("审核备注长度超限");
        return serialized(new Work<LeaveRequestDto>() {
            @Override public LeaveRequestDto run(java.sql.Connection c) throws Exception {
                LeaveRequestDto old = repository.lock(c, r.getLeaveId());
                if (old == null) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "请假申请不存在");
                if (!"PENDING".equals(old.getStatus())) throw new DormRepositoryException(DormCommands.LEAVE_INVALID_STATE, "仅待审核申请可以审核");
                if (r.isApproved()) {
                    repository.lockStudent(c, old.getStudentUserId());
                    if (repository.hasOverlap(c, old.getStudentUserId(), old.getStartAt(), old.getEndAt(), Long.valueOf(old.getId()))) throw new DormRepositoryException(DormCommands.LEAVE_OVERLAP, "请假时间与已有申请重叠");
                }
                return repository.review(c, r.getLeaveId(), s.getUserId(), r.isApproved(), r.getRemark());
            }
        });
    }

    private <T> T serialized(Work<T> work) {
        if (transactions == null) synchronized (repository) { return execute(work); }
        return execute(work);
    }

    private static LeaveSubmitRequest submitRequest(LeaveSubmitRequest r) {
        if (r == null || r.getStartAt() == null || r.getEndAt() == null) throw invalid("请假时间不能为空");
        if (!r.getEndAt().isAfter(r.getStartAt())) throw invalid("结束时间必须晚于开始时间");
        String type = normalizeType(r.getLeaveType());
        String reason = requiredText(r.getReason(), "请假事由");
        if (reason.length() > 500) throw invalid("请假事由长度超限");
        return new LeaveSubmitRequest(type, r.getStartAt(), r.getEndAt(), reason);
    }

    private static LeaveQuery query(LeaveQuery q) {
        LeaveQuery value = q == null ? new LeaveQuery() : q;
        if (value.getStudentUserId() != null && value.getStudentUserId().longValue() <= 0L) throw invalid("学生编号不正确");
        if (value.getStartDate() != null && value.getEndDate() != null && value.getStartDate().isAfter(value.getEndDate())) throw invalid("日期范围不正确");
        if (value.getStatus() != null) {
            try { edu.seu.vcampus.common.dto.dorm.DormRequestStatus.valueOf(value.getStatus().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw invalid("请假状态不正确"); }
        }
        return value;
    }

    private static String normalizeType(String value) {
        String type = requiredText(value, "请假类型").toUpperCase(Locale.ROOT);
        try { DormLeaveType.valueOf(type); } catch (IllegalArgumentException ex) { throw invalid("请假类型不正确"); }
        return type;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw invalid(field + "不能为空");
        return value.trim();
    }
    private static DormException invalid(String message) { return new DormException(DormCommands.INVALID_INPUT, message); }
}
