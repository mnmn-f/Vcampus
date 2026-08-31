package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.campus.repository.CampusRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;

/** 教室申请与审批服务；审批在锁定申请并检查重叠占用的事务中完成。 */
final class CampusClassroomService extends CampusServiceSupport {
    private final CampusRepository repository;

    CampusClassroomService(CampusRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    CampusPage<CampusClassroomDto> classrooms(final SessionContext session, final CampusPageQuery query) {
        requireAny(session, Permission.CLASSROOM_RESERVE, Permission.CLASSROOM_APPROVE);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<CampusClassroomDto>>() { public CampusPage<CampusClassroomDto> run(java.sql.Connection c) throws Exception { return repository.listClassrooms(c, q); } });
    }

    CampusPage<ClassroomReservationDto> mine(final SessionContext session, final CampusPageQuery query) {
        require(session, Permission.CLASSROOM_RESERVE);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<ClassroomReservationDto>>() { public CampusPage<ClassroomReservationDto> run(java.sql.Connection c) throws Exception { return repository.listReservations(c, Long.valueOf(session.getUserId()), q); } });
    }

    CampusPage<ClassroomReservationDto> all(final SessionContext session, final CampusPageQuery query) {
        require(session, Permission.CLASSROOM_APPROVE);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<ClassroomReservationDto>>() { public CampusPage<ClassroomReservationDto> run(java.sql.Connection c) throws Exception { return repository.listReservations(c, null, q); } });
    }

    ClassroomReservationDto apply(final SessionContext session, final ClassroomReservationRequest request) {
        require(session, Permission.CLASSROOM_RESERVE);
        validate(request);
        return execute(new Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run(java.sql.Connection c) throws Exception {
                CampusClassroomDto room = repository.lockClassroom(c, request.getClassroomId());
                if (room == null) throw new CampusException(CampusCommands.CLASSROOM_NOT_FOUND, "教室不存在");
                if (!"AVAILABLE".equals(room.getStatus())) throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "教室当前不可申请");
                if (repository.hasConflict(c, request.getClassroomId(), request.getStartAt(), request.getEndAt(), 0L)) throw new CampusException(CampusCommands.CLASSROOM_CONFLICT, "该时段已有教室申请占用");
                if (repository.hasApplicantConflict(c, session.getUserId(), request.getStartAt(), request.getEndAt())) throw new CampusException(CampusCommands.CLASSROOM_CONFLICT, "本人已有重叠教室申请");
                return repository.createReservation(c, request, session.getUserId());
            }
        });
    }

    ClassroomReservationDto review(final SessionContext session, final ClassroomReviewRequest request) {
        require(session, Permission.CLASSROOM_APPROVE);
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "审批参数不能为空");
        id(request.getReservationId(), "预约");
        if (!"APPROVED".equals(request.getStatus()) && !"REJECTED".equals(request.getStatus())
                && !"CANCELLED".equals(request.getStatus())) {
            throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "审批状态不正确");
        }
        return execute(new Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run(java.sql.Connection c) throws Exception {
            /*
             * Apply already locks classrooms before reservation overlap rows.  Review
             * must use the same order; otherwise two transactions (one approving and
             * one applying) can form a classroom/reservation deadlock cycle.
             */
            ClassroomReservationDto candidate = repository.findReservation(c,
                    request.getReservationId());
            if (candidate == null) throw new CampusException(CampusCommands.CLASSROOM_NOT_FOUND, "预约不存在");
            CampusClassroomDto room = repository.lockClassroom(c, candidate.getClassroomId());
            if (room == null) throw new CampusException(CampusCommands.CLASSROOM_NOT_FOUND, "教室不存在");
            ClassroomReservationDto old = repository.lockReservation(c, request.getReservationId());
            if (old == null) throw new CampusException(CampusCommands.CLASSROOM_NOT_FOUND, "预约不存在");
            if ("CANCELLED".equals(request.getStatus())) {
                if (!"PENDING".equals(old.getStatus()) && !"APPROVED".equals(old.getStatus())) {
                    throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "预约当前不能取消");
                }
            } else if (!"PENDING".equals(old.getStatus())) {
                throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "预约已处理，不能重复审批");
            }
            if ("APPROVED".equals(request.getStatus())) {
                if (!"AVAILABLE".equals(room.getStatus())) throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "教室当前不可审批");
                if (repository.hasConflict(c, old.getClassroomId(), old.getStartAt(),
                        old.getEndAt(), old.getId())) {
                throw new CampusException(CampusCommands.CLASSROOM_CONFLICT, "审批后与已有预约冲突");
                }
            }
                return repository.review(c, request, session.getUserId());
            }
        });
    }

    ClassroomReservationDto cancel(final SessionContext session, final long id) {
        require(session, Permission.CLASSROOM_RESERVE);
        id(id, "预约");
        return execute(new Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run(java.sql.Connection c) throws Exception {
                ClassroomReservationDto old = repository.lockReservation(c, id);
                if (old == null) throw new CampusException(CampusCommands.CLASSROOM_NOT_FOUND, "预约不存在");
                if (old.getApplicantId() != session.getUserId()) throw new CampusException(CampusCommands.CLASSROOM_FORBIDDEN, "只能取消本人的预约");
                if (!"PENDING".equals(old.getStatus()) && !"APPROVED".equals(old.getStatus())) throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "当前状态不能取消");
                if (!LocalDateTime.now().isBefore(old.getStartAt())) throw new CampusException(CampusCommands.CLASSROOM_INVALID_STATE, "预约已开始，不能取消");
                return repository.cancelReservation(c, id, session.getUserId());
            }
        });
    }

    private static void validate(ClassroomReservationRequest request) {
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "教室申请参数不能为空");
        id(request.getClassroomId(), "教室");
        text(request.getPurpose(), "申请用途");
        if (request.getStartAt() == null || request.getEndAt() == null
                || !request.getEndAt().isAfter(request.getStartAt())) throw new CampusException(CampusCommands.INVALID_INPUT, "预约时间范围不正确");
    }
}
