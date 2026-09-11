package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 内存门禁、未归、卫生和报修仓储。 */
final class InMemoryDormGovernanceRepository implements DormGovernanceRepository {
    private final InMemoryDormState state;

    InMemoryDormGovernanceRepository(InMemoryDormState state) { this.state = state; }

    @Override
    public synchronized AccessRecordDto addAccess(Connection c, long studentId,
                                                   AccessRecordDto request) {
        AccessRecordDto value = new AccessRecordDto(state.nextAccess++, studentId,
                request.getRecordType(), request.getOccurredAt() == null
                        ? LocalDateTime.now() : request.getOccurredAt(), request.getDoorName(),
                request.getSource() == null ? "MANUAL" : request.getSource(), request.getNote());
        state.access.put(Long.valueOf(value.getId()), value);
        return value;
    }

    @Override
    public synchronized org.threeten.bp.LocalTime[] accessPolicy(Connection c) {
        return new org.threeten.bp.LocalTime[]{org.threeten.bp.LocalTime.of(23, 0), org.threeten.bp.LocalTime.of(5, 0)};
    }

    @Override
    public synchronized boolean openLateAlert(Connection c, long studentId, org.threeten.bp.LocalDate date,
                                              LocalDateTime detectedAt) {
        for (LateReturnAlertDto item : state.alerts.values()) {
            if (item.getStudentUserId() == studentId && date.equals(item.getAlertDate())) return false;
        }
        long id = state.nextAlert++;
        state.alerts.put(Long.valueOf(id), new LateReturnAlertDto(id, studentId, date,
                detectedAt == null ? LocalDateTime.now() : detectedAt, "OPEN", null, null, null));
        return true;
    }

    @Override
    public synchronized DormPage<AccessRecordDto> listAccess(Connection c, Long studentId,
                                                              DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<AccessRecordDto> rows = new ArrayList<AccessRecordDto>();
        for (AccessRecordDto item : state.access.values()) {
            if ((studentId == null || item.getStudentUserId() == studentId.longValue())
                    && InMemoryDormSupport.status(query.getStatus(), item.getRecordType())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormPage<LateReturnAlertDto> listAlerts(Connection c, Long studentId,
                                                                 DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<LateReturnAlertDto> rows = new ArrayList<LateReturnAlertDto>();
        for (LateReturnAlertDto item : state.alerts.values()) {
            if ((studentId == null || item.getStudentUserId() == studentId.longValue())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized LateReturnAlertDto lockAlert(Connection c, long alertId) {
        return state.alerts.get(Long.valueOf(alertId));
    }

    @Override
    public synchronized LateReturnAlertDto handleAlert(Connection c, long alertId,
                                                        long handlerId,
                                                        LateReturnHandleRequest request) {
        LateReturnAlertDto old = state.alerts.get(Long.valueOf(alertId));
        if (old == null) throw new DormRepositoryException(DormCommands.ALERT_NOT_FOUND, "未归记录不存在");
        if (!"OPEN".equals(old.getStatus())) {
            throw new DormRepositoryException(DormCommands.ALERT_INVALID_STATE, "未归记录已处理");
        }
        String status = request.getStatus();
        if (!"CONFIRMED".equals(status) && !"CLEARED".equals(status)
                && !"IGNORED".equals(status)) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "未归处理状态不正确");
        }
        LateReturnAlertDto value = new LateReturnAlertDto(old.getId(), old.getStudentUserId(),
                old.getAlertDate(), old.getDetectedAt(), status, handlerId, LocalDateTime.now(),
                request.getNote());
        state.alerts.put(Long.valueOf(value.getId()), value);
        return value;
    }

    @Override
    public synchronized DormPage<HygieneInspectionDto> listHygiene(Connection c, DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<HygieneInspectionDto> rows = new ArrayList<HygieneInspectionDto>();
        for (HygieneInspectionDto item : state.hygiene.values()) {
            if ((query.getRoomId() == null || item.getRoomId() == query.getRoomId().longValue())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized HygieneInspectionDto saveHygiene(Connection c,
                                                          HygieneInspectionRequest request,
                                                          long inspectorId) {
        long id = request.getId() <= 0 ? state.nextHygiene++ : request.getId();
        HygieneInspectionDto old = state.hygiene.get(Long.valueOf(id));
        String status = request.getStatus() == null ? "NORMAL" : request.getStatus();
        LocalDateTime inspected = old == null ? LocalDateTime.now() : old.getInspectedAt();
        LocalDateTime rectified = "RECTIFIED".equals(status) ? LocalDateTime.now()
                : old == null ? null : old.getRectifiedAt();
        HygieneInspectionDto value = new HygieneInspectionDto(id, request.getRoomId(), inspectorId,
                inspected, request.getScore(), request.getResult(), request.getIssueDescription(),
                status, rectified, request.getRectificationNote());
        state.hygiene.put(Long.valueOf(id), value);
        return value;
    }

    @Override
    public synchronized DormPage<RepairOrderDto> listRepairs(Connection c, Long reporterId,
                                                              DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<RepairOrderDto> rows = new ArrayList<RepairOrderDto>();
        for (RepairOrderDto item : state.repairs.values()) {
            if ((reporterId == null || item.getReporterId() == reporterId.longValue())
                    && (query.getRoomId() == null || item.getRoomId() == query.getRoomId().longValue())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized RepairOrderDto createRepair(Connection c, RepairCreateRequest request,
                                                     long reporterId) {
        RepairOrderDto value = new RepairOrderDto(state.nextRepair++, request.getRoomId(), reporterId,
                request.getCategory(), request.getDescription(), request.getPriority(), "SUBMITTED",
                null, LocalDateTime.now(), null, null, null, null);
        state.repairs.put(Long.valueOf(value.getId()), value);
        return value;
    }

    @Override
    public synchronized RepairOrderDto lockRepair(Connection c, long orderId) {
        return state.repairs.get(Long.valueOf(orderId));
    }

    @Override
    public synchronized RepairOrderDto updateRepair(Connection c, RepairStatusRequest request,
                                                    long actorId) {
        RepairOrderDto old = state.repairs.get(Long.valueOf(request.getOrderId()));
        if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        String status = request.getStatus();
        LocalDateTime accepted = old.getAcceptedAt();
        LocalDateTime completed = old.getCompletedAt();
        Long handler = old.getHandlerId();
        if ("ACCEPTED".equals(status)) { accepted = LocalDateTime.now(); handler = actorId; }
        if ("IN_PROGRESS".equals(status) && handler == null) handler = actorId;
        if ("COMPLETED".equals(status)) completed = LocalDateTime.now();
        Integer score = request.getEvaluationScore() == null ? old.getEvaluationScore()
                : request.getEvaluationScore();
        String note = request.getEvaluationNote() == null ? old.getEvaluationNote()
                : request.getEvaluationNote();
        RepairOrderDto value = new RepairOrderDto(old.getId(), old.getRoomId(), old.getReporterId(),
                old.getCategory(), old.getDescription(), old.getPriority(), status, handler,
                old.getSubmittedAt(), accepted, completed, score, note);
        state.repairs.put(Long.valueOf(value.getId()), value);
        return value;
    }

    @Override
    public synchronized RepairOrderDto evaluateRepair(Connection c, RepairEvaluationRequest request,
                                                       long studentId) {
        RepairOrderDto old = state.repairs.get(Long.valueOf(request.getOrderId()));
        if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        if (!"COMPLETED".equals(old.getStatus()) || old.getEvaluationScore() != null) {
            throw new DormRepositoryException(DormCommands.REPAIR_INVALID_STATE, "报修工单当前不可评价");
        }
        RepairOrderDto value = new RepairOrderDto(old.getId(), old.getRoomId(), old.getReporterId(),
                old.getCategory(), old.getDescription(), old.getPriority(), old.getStatus(),
                old.getHandlerId(), old.getSubmittedAt(), old.getAcceptedAt(), old.getCompletedAt(),
                Integer.valueOf(request.getScore()), request.getNote());
        state.repairs.put(Long.valueOf(value.getId()), value);
        return value;
    }
}
