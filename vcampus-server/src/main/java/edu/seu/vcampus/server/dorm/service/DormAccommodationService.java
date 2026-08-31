package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDate;

/** 住宿查询、直接办理和申请审批；身份主体始终来自 SessionContext。 */
final class DormAccommodationService extends DormServiceSupport {
    private final DormRepository repository;

    DormAccommodationService(DormRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    AccommodationDto mine(final SessionContext session) {
        require(session, Permission.DORM_SELF_READ);
        return execute(new Work<AccommodationDto>() { public AccommodationDto run(java.sql.Connection c) throws Exception { return repository.findCurrent(c, session.getUserId(), false); } });
    }

    AccommodationDto assign(final SessionContext session, final long studentId, final long bedId, final LocalDate date) {
        require(session, Permission.DORM_MANAGE);
        id(studentId, "学生账号"); id(bedId, "床位");
        return execute(new Work<AccommodationDto>() { public AccommodationDto run(java.sql.Connection c) throws Exception { return repository.assign(c, studentId, bedId, date, session.getUserId()); } });
    }

    AccommodationDto transfer(final SessionContext session, final long studentId, final long recordId,
                               final long targetBedId, final LocalDate date) {
        require(session, Permission.DORM_MANAGE);
        id(studentId, "学生账号"); id(recordId, "住宿记录"); id(targetBedId, "目标床位");
        return execute(new Work<AccommodationDto>() { public AccommodationDto run(java.sql.Connection c) throws Exception { return repository.transfer(c, studentId, recordId, targetBedId, date, session.getUserId()); } });
    }

    AccommodationDto checkout(final SessionContext session, final long studentId, final long recordId,
                              final LocalDate date) {
        require(session, Permission.DORM_MANAGE);
        id(studentId, "学生账号"); id(recordId, "住宿记录");
        return execute(new Work<AccommodationDto>() { public AccommodationDto run(java.sql.Connection c) throws Exception { return repository.checkout(c, studentId, recordId, date, session.getUserId()); } });
    }

    AccommodationRequestDto submit(final SessionContext session, final AccommodationRequest request) {
        require(session, Permission.DORM_REQUEST);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "申请参数不能为空");
        final String type = text(request.getRequestType(), "申请类型").toUpperCase();
        final Long current = request.getCurrentRecordId();
        Long target = request.getRequestedBedId();
        if ("CHECK_OUT".equals(type)) target = null;
        final Long targetBed = target;
        return execute(new Work<AccommodationRequestDto>() { public AccommodationRequestDto run(java.sql.Connection c) throws Exception { return repository.submitRequest(c, session.getUserId(), type, current, targetBed, request.getReason()); } });
    }

    DormPage<AccommodationRequestDto> requests(final SessionContext session, final DormPageQuery query,
                                               final Long requestedStudentId) {
        requireAny(session, Permission.DORM_REQUEST, Permission.DORM_APPROVE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final boolean manager = session.allows(Permission.DORM_APPROVE);
        final Long student = manager ? requestedStudentId
                : Long.valueOf(session.getUserId());
        return execute(new Work<DormPage<AccommodationRequestDto>>() { public DormPage<AccommodationRequestDto> run(java.sql.Connection c) throws Exception { return repository.listRequests(c, student, q); } });
    }

    AccommodationRequestDto approve(final SessionContext session, final DormApprovalRequest request) {
        require(session, Permission.DORM_APPROVE);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "审批参数不能为空");
        id(request.getRequestId(), "申请");
        return execute(new Work<AccommodationRequestDto>() {
            @Override public AccommodationRequestDto run(java.sql.Connection c) throws Exception {
                AccommodationRequestDto pending = repository.lockRequest(c, request.getRequestId());
                if (pending == null) throw new DormRepositoryException(DormCommands.REQUEST_NOT_FOUND, "住宿申请不存在");
                if (!"PENDING".equals(pending.getStatus())) throw new DormRepositoryException(DormCommands.REQUEST_INVALID_STATE, "申请已处理");
                if (request.isApproved()) apply(c, pending, session.getUserId());
                return repository.finishRequest(c, pending.getId(), session.getUserId(), request.isApproved(), request.getRemark());
            }
        });
    }

    private void apply(java.sql.Connection c, AccommodationRequestDto request, long actor)
            throws java.sql.SQLException {
        String type = request.getRequestType();
        if ("CHECK_IN".equals(type)) {
            repository.assign(c, request.getStudentUserId(), request.getRequestedBedId().longValue(),
                    LocalDate.now(), actor);
        } else if ("TRANSFER".equals(type)) {
            repository.transfer(c, request.getStudentUserId(), request.getCurrentRecordId().longValue(),
                    request.getRequestedBedId().longValue(), LocalDate.now(), actor);
        } else if ("CHECK_OUT".equals(type)) {
            repository.checkout(c, request.getStudentUserId(), request.getCurrentRecordId().longValue(),
                    LocalDate.now(), actor);
        } else {
            throw new DormException(DormCommands.INVALID_INPUT, "申请类型不正确");
        }
    }

}
