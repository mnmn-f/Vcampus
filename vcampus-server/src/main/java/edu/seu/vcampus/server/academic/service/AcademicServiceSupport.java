package edu.seu.vcampus.server.academic.service;

import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 教务服务共用的鉴权、校验和事务边界。 */
final class AcademicServiceSupport {
    private final AcademicRepository repository;
    private final TransactionManager transactionManager;

    AcademicServiceSupport(AcademicRepository repository, TransactionManager transactionManager) {
        this.repository = repository;
        this.transactionManager = transactionManager;
    }

    AcademicRepository repository() {
        return repository;
    }

    <T> T execute(final Work<T> work) throws AcademicException {
        if (transactionManager == null) {
            synchronized (repository) {
                try {
                    return work.run(null);
                } catch (AcademicException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw internal(ex);
                }
            }
        }
        try {
            return transactionManager.execute(new TransactionWork<T>() {
                @Override
                public T execute(Connection connection) throws Exception {
                    return work.run(connection);
                }
            });
        } catch (AcademicException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal(ex);
        }
    }

    void requirePermission(SessionContext session, Permission permission,
                           Role expectedRole) throws AcademicException {
        if (session == null) {
            throw failure(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        if (expectedRole != null && session.getActiveRole() != expectedRole) {
            throw failure(ResultCodes.FORBIDDEN, "当前职责不能执行此操作");
        }
        if (!session.allows(permission)) {
            throw failure(ResultCodes.FORBIDDEN, "当前职责无权执行此操作");
        }
    }

    CourseQuery validQuery(CourseQuery query) throws AcademicException {
        CourseQuery result = query == null ? new CourseQuery() : query;
        if (result.getStatus() != null && !enumValue(CourseStatus.class, result.getStatus())) {
            throw failure(AcademicCommands.INVALID_COURSE, "课程状态不正确");
        }
        if (result.getCourseType() != null
                && !enumValue(CourseType.class, result.getCourseType())) {
            throw failure(AcademicCommands.INVALID_COURSE, "课程类别不正确");
        }
        return result;
    }

    void validateCourse(CourseSaveRequest request, boolean update)
            throws AcademicException {
        if (request == null || request.isUpdate() != update || blank(request.getCourseCode())
                || blank(request.getCourseName()) || request.getCredits() == null
                || update && (request.getCourseId() == null || request.getCourseId() <= 0)
                || request.getCredits().signum() <= 0 || request.getCapacity() == null
                || request.getCapacity().intValue() <= 0
                || request.getCredits().compareTo(new BigDecimal("99.99")) > 0
                || request.getCredits().scale() > 2
                || request.getTotalHours() != null && (request.getTotalHours() <= 0
                || request.getTotalHours() > 65535)
                || !enumValue(CourseType.class, request.getCourseType())
                || !enumValue(CourseStatus.class, request.getStatus())) {
            throw failure(AcademicCommands.INVALID_COURSE, "课程信息不完整或格式不正确");
        }
        List<Long> ids = request.getInstructorUserIds();
        Set<Long> unique = new HashSet<Long>();
        for (Long id : ids) {
            if (id == null || id <= 0 || !unique.add(id)) {
                throw failure(AcademicCommands.INVALID_COURSE, "授课教师信息不正确");
            }
        }
        if (CourseStatus.PUBLISHED.name().equals(request.getStatus()) && ids.isEmpty()) {
            throw failure(AcademicCommands.INVALID_COURSE, "发布课程必须设置授课教师");
        }
    }

    void validateSchedule(ScheduleSaveRequest request, boolean update)
            throws AcademicException {
        if (request == null || request.isUpdate() != update
                || update && (request.getScheduleId() == null || request.getScheduleId() <= 0)
                || request.getCourseId() <= 0
                || request.getWeekday() < 1 || request.getWeekday() > 7
                || request.getStartPeriod() < 1
                || request.getEndPeriod() < request.getStartPeriod()
                || request.getEndPeriod() > 255
                || request.getClassroomId() != null && request.getClassroomId() <= 0
                || request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw failure(AcademicCommands.INVALID_SCHEDULE, "课程时段信息不正确");
        }
    }

    void verifyStudent(Connection c, long studentId) throws Exception {
        if (!repository.isActiveStudent(c, studentId)) {
            throw failure(AcademicCommands.STUDENT_NOT_FOUND, "当前账号不是在籍学生");
        }
    }

    void verifyTeachers(Connection c, List<Long> ids) throws Exception {
        if (!repository.allActiveTeachers(c, ids)) {
            throw failure(AcademicCommands.TEACHER_NOT_FOUND, "存在无效或停用的授课教师");
        }
    }

    static AcademicException failure(String code, String message) {
        return new AcademicException(code, message);
    }

    private static AcademicException internal(Exception cause) {
        return new AcademicException(ResultCodes.INTERNAL_ERROR,
                "教务服务暂时无法处理请求", cause);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <E extends Enum<E>> boolean enumValue(Class<E> type, String value) {
        if (value == null) {
            return false;
        }
        try {
            Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    interface Work<T> {
        T run(Connection connection) throws Exception;
    }
}
