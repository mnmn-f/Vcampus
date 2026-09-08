package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.*;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Absence-warning workflow and threshold configuration. */
final class DormExtWarningService extends DormServiceSupport {
    private final DormExtRepository repository;
    DormExtWarningService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    WarningScanResultDto scan(SessionContext session, WarningScanRequest request) {
        require(session, Permission.DORM_GOVERN); return execute(new Work<WarningScanResultDto>() { @Override public WarningScanResultDto run(Connection c) throws Exception { return scan(c, request == null ? null : request.getScanDate()); } });
    }
    DormPage<AbsenceWarningDto> list(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_GOVERN); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<AbsenceWarningDto>>() { @Override public DormPage<AbsenceWarningDto> run(Connection c) throws Exception { return repository.listWarnings(c, q); } });
    }
    AbsenceWarningDto notify(SessionContext session, WarningHandleRequest request) {
        require(session, Permission.DORM_GOVERN); validateHandle(request, true);
        return execute(new Work<AbsenceWarningDto>() { @Override public AbsenceWarningDto run(Connection c) throws Exception {
            AbsenceWarningDto current = requireWarning(c, request.getWarningId());
            if (AbsenceWarningDto.STATUS_VERIFIED.equals(current.getHandleStatus())) throw new DormException(DormExtCommands.WARNING_INVALID_STATE, "该预警已核实，无需再通知");
            return repository.updateWarningStatus(c, request.getWarningId(), AbsenceWarningDto.STATUS_NOTIFIED, request.getTeacherUserId(), LocalDateTime.now(), request.getNote());
        } });
    }
    AbsenceWarningDto verify(SessionContext session, WarningHandleRequest request) {
        require(session, Permission.DORM_GOVERN); validateHandle(request, false);
        return execute(new Work<AbsenceWarningDto>() { @Override public AbsenceWarningDto run(Connection c) throws Exception {
            AbsenceWarningDto current = requireWarning(c, request.getWarningId());
            return repository.updateWarningStatus(c, request.getWarningId(), AbsenceWarningDto.STATUS_VERIFIED, current.getNotifiedTeacherId(), current.getNotifiedAt(), request.getNote());
        } });
    }
    WarningConfigDto config(SessionContext session) { require(session, Permission.DORM_GOVERN); return execute(new Work<WarningConfigDto>() { @Override public WarningConfigDto run(Connection c) throws Exception { return repository.loadWarningConfig(c); } }); }
    WarningConfigDto saveConfig(SessionContext session, WarningConfigRequest request) {
        require(session, Permission.DORM_GOVERN); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "阈值参数不能为空");
        if (request.getWarnDays() <= 0 || request.getNotifyDays() < request.getWarnDays()) throw new DormException(DormExtCommands.CONFIG_INVALID, request.getWarnDays() <= 0 ? "预警天数必须大于零" : "通知天数不能小于预警天数");
        return execute(new Work<WarningConfigDto>() { @Override public WarningConfigDto run(Connection c) throws Exception { return repository.saveWarningConfig(c, request, session.getUserId()); } });
    }
    WarningScanResultDto scanScheduled(final LocalDate date) { return execute(new Work<WarningScanResultDto>() { @Override public WarningScanResultDto run(Connection c) throws Exception { return scan(c, date); } }); }
    List<AbsenceWarningDto> pendingSevere(final LocalDate date) { return execute(new Work<List<AbsenceWarningDto>>() { @Override public List<AbsenceWarningDto> run(Connection c) throws Exception { return repository.pendingSevereWarnings(c, date); } }); }

    private WarningScanResultDto scan(Connection c, LocalDate requested) throws Exception {
        LocalDate date = requested == null ? LocalDate.now() : requested; WarningConfigDto config = repository.loadWarningConfig(c); List<ResidentAbsenceSnapshot> residents = repository.residentsForScan(c); int normal = 0; int severe = 0; int exempt = 0;
        for (ResidentAbsenceSnapshot resident : residents) {
            int days = DormAbsenceRules.absenceDays(resident.getLastExitAt(), resident.getLastEntryAt(), date); if (!DormAbsenceRules.shouldWarn(days, config.getWarnDays())) continue;
            boolean onLeave = config.isExemptOnLeave() && repository.hasApprovedLeave(c, resident.getStudentUserId(), date); String level = DormAbsenceRules.level(days, config.getNotifyDays(), onLeave);
            repository.saveWarning(c, resident.getStudentUserId(), resident.getRoomId(), date, resident.getLastExitAt(), days, level);
            if (AbsenceWarningDto.LEVEL_EXEMPT.equals(level)) exempt++; else if (AbsenceWarningDto.LEVEL_SEVERE.equals(level)) severe++; else normal++;
        }
        return new WarningScanResultDto(date, residents.size(), normal, severe, exempt);
    }
    private AbsenceWarningDto requireWarning(Connection c, long id) throws Exception { AbsenceWarningDto value = repository.findWarning(c, id); if (value == null) throw new DormException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在"); return value; }
    private static void validateHandle(WarningHandleRequest r, boolean notify) {
        if (r == null) throw new DormException(DormExtCommands.INVALID_INPUT, "处理参数不能为空"); DormExtValidation.id(r.getWarningId(), "预警编号");
        if (notify && (r.getTeacherUserId() == null || r.getTeacherUserId().longValue() <= 0L)) throw new DormException(DormExtCommands.INVALID_INPUT, "请指定要通知的辅导员");
    }
}
