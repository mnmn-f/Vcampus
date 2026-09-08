package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** In-memory absence-warning persistence. */
final class InMemoryDormWarningStore {
    private final InMemoryDormExtState state;
    InMemoryDormWarningStore(InMemoryDormExtState state) { this.state = state; }

    void addResident(long studentId, long roomId, LocalDateTime exit, LocalDateTime entry) {
        state.residentSnapshots.add(new ResidentAbsenceSnapshot(studentId, roomId, exit, entry));
    }

    void addApprovedLeave(long studentId, LocalDate date) { state.approvedLeaves.add(studentId + "|" + date); }

    List<ResidentAbsenceSnapshot> residentsForScan() { return new ArrayList<ResidentAbsenceSnapshot>(state.residentSnapshots); }

    boolean hasApprovedLeave(long studentId, LocalDate date) { return state.approvedLeaves.contains(studentId + "|" + date); }

    AbsenceWarningDto save(long studentId, long roomId, LocalDate date, LocalDateTime lastLeaveAt,
                           int days, String level) {
        AbsenceWarningDto old = findByStudent(studentId, date);
        long id = old == null ? state.nextWarningId++ : old.getId();
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        AbsenceWarningDto value = new AbsenceWarningDto(id, studentId, roomId, parts[0], parts[1], date,
                lastLeaveAt, days, level, old == null ? AbsenceWarningDto.STATUS_PENDING : old.getHandleStatus(),
                old == null ? null : old.getNotifiedTeacherId(), old == null ? null : old.getNotifiedAt(),
                old == null ? null : old.getNote());
        state.warnings.put(Long.valueOf(id), value);
        return value;
    }

    DormPage<AbsenceWarningDto> list(DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<AbsenceWarningDto> rows = new ArrayList<AbsenceWarningDto>();
        for (AbsenceWarningDto item : state.warnings.values()) {
            boolean room = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (room && InMemoryDormSupport.status(q.getStatus(), item.getHandleStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(), item.getRoomNo(), item.getBuildingCode(), item.getWarningLevel())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, q);
    }

    AbsenceWarningDto find(long id) { return state.warnings.get(Long.valueOf(id)); }

    AbsenceWarningDto update(long id, String status, Long teacherId, LocalDateTime notifiedAt, String note) {
        AbsenceWarningDto old = find(id);
        if (old == null) throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在");
        AbsenceWarningDto value = new AbsenceWarningDto(old.getId(), old.getStudentUserId(), old.getRoomId(),
                old.getBuildingCode(), old.getRoomNo(), old.getScanDate(), old.getLastLeaveAt(), old.getAbsenceDays(),
                old.getWarningLevel(), status, teacherId, notifiedAt, note);
        state.warnings.put(Long.valueOf(id), value);
        return value;
    }

    WarningConfigDto config() { return state.warningConfig; }

    WarningConfigDto saveConfig(WarningConfigRequest request) {
        state.warningConfig = new WarningConfigDto(request.getWarnDays(), request.getNotifyDays(),
                request.isExemptOnLeave(), LocalDateTime.now());
        return state.warningConfig;
    }

    List<AbsenceWarningDto> pendingSevere(LocalDate limit) {
        LocalDate date = limit == null ? LocalDate.now() : limit;
        List<AbsenceWarningDto> result = new ArrayList<AbsenceWarningDto>();
        for (AbsenceWarningDto item : state.warnings.values()) {
            if (AbsenceWarningDto.LEVEL_SEVERE.equals(item.getWarningLevel())
                    && AbsenceWarningDto.STATUS_PENDING.equals(item.getHandleStatus())
                    && (item.getScanDate() == null || !item.getScanDate().isAfter(date))) result.add(item);
        }
        return result;
    }

    private AbsenceWarningDto findByStudent(long studentId, LocalDate date) {
        for (AbsenceWarningDto item : state.warnings.values()) {
            if (item.getStudentUserId() == studentId && item.getScanDate().equals(date)) return item;
        }
        return null;
    }
}
