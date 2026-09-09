package edu.seu.vcampus.server.academic.service;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 课程时段和教室冲突维护。 */
final class AcademicScheduleService {
    private final AcademicServiceSupport support;

    AcademicScheduleService(AcademicServiceSupport support) {
        this.support = support;
    }

    CourseScheduleDto create(SessionContext session, ScheduleSaveRequest request)
            throws AcademicException {
        return save(session, request, false);
    }

    CourseScheduleDto update(SessionContext session, ScheduleSaveRequest request)
            throws AcademicException {
        return save(session, request, true);
    }

    void delete(SessionContext session, final long scheduleId) throws AcademicException {
        support.requirePermission(session, Permission.COURSE_MANAGE, Role.ACADEMIC_ADMIN);
        if (scheduleId <= 0) {
            throw AcademicServiceSupport.failure(AcademicCommands.INVALID_SCHEDULE,
                    "时段编号不正确");
        }
        support.execute(new AcademicServiceSupport.Work<Void>() {
            @Override
            public Void run(java.sql.Connection connection) throws Exception {
                if (!repository().deleteSchedule(connection, scheduleId)) {
                    throw AcademicServiceSupport.failure(AcademicCommands.SCHEDULE_NOT_FOUND,
                            "课程时段不存在");
                }
                return null;
            }
        });
    }

    private CourseScheduleDto save(SessionContext session, ScheduleSaveRequest request,
                                   boolean update) throws AcademicException {
        support.requirePermission(session, Permission.COURSE_MANAGE, Role.ACADEMIC_ADMIN);
        support.validateSchedule(request, update);
        final ScheduleSaveRequest selected = request;
        return support.execute(new AcademicServiceSupport.Work<CourseScheduleDto>() {
            @Override
            public CourseScheduleDto run(java.sql.Connection connection) throws Exception {
                AcademicRepository repository = repository();
                CourseDto course = repository.findCourse(connection, selected.getCourseId());
                if (course == null) {
                    throw AcademicServiceSupport.failure(AcademicCommands.COURSE_NOT_FOUND,
                            "课程不存在");
                }
                if ("ARCHIVED".equals(course.getStatus())) {
                    throw AcademicServiceSupport.failure(AcademicCommands.COURSE_CLOSED,
                            "归档课程不能排课");
                }
                if (selected.isUpdate()) {
                    CourseScheduleDto old = repository.findSchedule(connection,
                            selected.getScheduleId());
                    if (old == null || old.getCourseId() != selected.getCourseId()) {
                        throw AcademicServiceSupport.failure(AcademicCommands.SCHEDULE_NOT_FOUND,
                                "课程时段不存在");
                    }
                }
                repository.lockSchedules(connection);
                if (!repository.classroomAvailable(connection, selected.getClassroomId())) {
                    throw AcademicServiceSupport.failure(AcademicCommands.CLASSROOM_NOT_FOUND,
                            "教室不存在或当前不可用");
                }
                if (!repository.classroomFitsCourse(connection, selected.getCourseId(),
                        selected.getClassroomId())) {
                    throw AcademicServiceSupport.failure(AcademicCommands.CLASSROOM_CONFLICT,
                            "教室容量或类型不满足课程要求");
                }
                if (repository.hasScheduleConflict(connection, selected)) {
                    throw AcademicServiceSupport.failure(AcademicCommands.SCHEDULE_CONFLICT,
                            "同一课程、授课教师或已选学生的时段存在冲突");
                }
                if (repository.hasClassroomConflict(connection, selected)) {
                    throw AcademicServiceSupport.failure(AcademicCommands.CLASSROOM_CONFLICT,
                            "教室在该时间段已被占用");
                }
                return repository.saveSchedule(connection, selected);
            }
        });
    }

    private AcademicRepository repository() {
        return support.repository();
    }
}
