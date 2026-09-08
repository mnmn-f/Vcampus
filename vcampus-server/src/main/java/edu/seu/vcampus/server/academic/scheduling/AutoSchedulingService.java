package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleConfirmRequest;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.AutoScheduleRequest;
import edu.seu.vcampus.common.dto.academic.AutoScheduleSaveResult;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;
import java.util.List;

/** Admin-only orchestration for preference editing, preview and atomic confirmation. */
public final class AutoSchedulingService {
    private final SchedulingRepository repository;
    private final TransactionManager transactions;
    private final AutoSchedulingSolver solver = new AutoSchedulingSolver();

    public AutoSchedulingService(SchedulingRepository repository, TransactionManager transactions) {
        if (repository == null) throw new IllegalArgumentException("scheduling repository is required");
        this.repository = repository; this.transactions = transactions;
    }

    /** In-memory test adapter; production always supplies a TransactionManager. */
    public AutoSchedulingService(SchedulingRepository repository) { this(repository, null); }

    public SchedulingOverviewDto overview(SessionContext session) throws AcademicException {
        requireAdmin(session); return execute(new Work<SchedulingOverviewDto>() {
            @Override public SchedulingOverviewDto run(Connection c) throws Exception { return repository.overview(c); }
        });
    }

    public TeacherTimePreferenceDto savePreference(SessionContext session,
            final TeacherTimePreferenceDto value) throws AcademicException {
        requireAdmin(session); validate(value); return execute(new Work<TeacherTimePreferenceDto>() {
            @Override public TeacherTimePreferenceDto run(Connection c) throws Exception { return repository.savePreference(c, value); }
        });
    }

    public void deletePreference(SessionContext session, final long id) throws AcademicException {
        requireAdmin(session); if (id <= 0) throw failure("偏好记录编号不正确");
        execute(new Work<Void>() { @Override public Void run(Connection c) throws Exception {
            if (!repository.deletePreference(c, id)) throw failure("偏好记录不存在"); return null;
        }});
    }

    public AutoSchedulePreviewDto preview(SessionContext session, AutoScheduleRequest request)
            throws AcademicException {
        requireAdmin(session); final int limit = request == null ? 8000 : request.getTimeLimitMillis();
        if (limit < 100 || limit > 15000) throw failure("搜索时间应在 100 到 15000 毫秒之间");
        return execute(new Work<AutoSchedulePreviewDto>() {
            @Override public AutoSchedulePreviewDto run(Connection c) throws Exception {
                return solver.solve(repository.loadProblem(c), limit);
            }
        });
    }

    public AutoScheduleSaveResult confirm(SessionContext session,
            final AutoScheduleConfirmRequest request) throws AcademicException {
        requireAdmin(session);
        if (request == null || request.getEntries().isEmpty()) throw failure("没有可保存的排课预览");
        return execute(new Work<AutoScheduleSaveResult>() {
            @Override public AutoScheduleSaveResult run(Connection c) throws Exception {
                repository.lockSchedules(c);
                List<String> errors = solver.validatePlan(repository.loadProblem(c), request.getEntries());
                if (!errors.isEmpty()) throw new AcademicException(AcademicCommands.SCHEDULING_STALE_PREVIEW, join(errors));
                int saved = repository.savePlan(c, request.getEntries());
                if (saved != request.getEntries().size()) throw new AcademicException(ResultCodes.INTERNAL_ERROR, "自动课表未能完整保存");
                return new AutoScheduleSaveResult(saved);
            }
        });
    }

    private <T> T execute(final Work<T> work) throws AcademicException {
        try {
            if (transactions == null) synchronized (repository) { return work.run(null); }
            return transactions.execute(new TransactionWork<T>() {
            @Override public T execute(Connection c) throws Exception { return work.run(c); }
        }); } catch (AcademicException ex) { throw ex; }
        catch (Exception ex) { throw new AcademicException(ResultCodes.INTERNAL_ERROR, "自动排课服务暂时不可用", ex); }
    }
    private static void requireAdmin(SessionContext session) throws AcademicException {
        if (session == null) throw new AcademicException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (session.getActiveRole() != Role.ACADEMIC_ADMIN || !session.allows(Permission.COURSE_MANAGE))
            throw new AcademicException(ResultCodes.FORBIDDEN, "只有教务管理员可以执行自动排课");
    }
    private static void validate(TeacherTimePreferenceDto value) throws AcademicException {
        if (value == null || value.getTeacherUserId() <= 0 || value.getWeekday() < 1 || value.getWeekday() > 7
                || value.getStartPeriod() < 1 || value.getEndPeriod() < value.getStartPeriod() || value.getEndPeriod() > 255)
            throw failure("教师时间偏好格式不正确");
        try { TimePreferenceType.valueOf(value.getPreferenceType()); }
        catch (Exception ex) { throw failure("偏好类型不正确"); }
    }
    private static AcademicException failure(String message) { return new AcademicException(AcademicCommands.INVALID_SCHEDULE, message); }
    private static String join(List<String> values) { StringBuilder r=new StringBuilder(); for(String v:values){if(r.length()>0)r.append("；");r.append(v);}return r.toString(); }
    private interface Work<T> { T run(Connection connection) throws Exception; }
}
