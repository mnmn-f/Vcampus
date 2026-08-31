package edu.seu.vcampus.server.academic.service;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 课程查询、课程基本信息和授课教师维护。 */
final class AcademicCourseService {
    private final AcademicServiceSupport support;

    AcademicCourseService(AcademicServiceSupport support) {
        this.support = support;
    }

    CoursePageDto query(SessionContext session, CourseQuery query) throws AcademicException {
        support.requirePermission(session, Permission.COURSE_READ, null);
        CourseQuery selected = support.validQuery(query);
        if (session.getActiveRole() == Role.STUDENT) {
            selected = selected.withStatus(CourseStatus.PUBLISHED.name());
        }
        final CourseQuery effective = selected;
        return support.execute(new AcademicServiceSupport.Work<CoursePageDto>() {
            @Override
            public CoursePageDto run(java.sql.Connection connection) throws Exception {
                return repository().findCourses(connection, effective);
            }
        });
    }

    CourseDto create(SessionContext session, CourseSaveRequest request)
            throws AcademicException {
        support.requirePermission(session, Permission.COURSE_MANAGE, Role.ACADEMIC_ADMIN);
        support.validateCourse(request, false);
        final CourseSaveRequest selected = request;
        final long actor = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<CourseDto>() {
            @Override
            public CourseDto run(java.sql.Connection connection) throws Exception {
                support.verifyTeachers(connection, selected.getInstructorUserIds());
                return repository().saveCourse(connection, selected, actor);
            }
        });
    }

    CourseDto update(SessionContext session, CourseSaveRequest request)
            throws AcademicException {
        support.requirePermission(session, Permission.COURSE_MANAGE, Role.ACADEMIC_ADMIN);
        support.validateCourse(request, true);
        final CourseSaveRequest selected = request;
        final long actor = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<CourseDto>() {
            @Override
            public CourseDto run(java.sql.Connection connection) throws Exception {
                AcademicRepository repository = repository();
                CourseDto old = repository.findCourse(connection,
                        selected.getCourseId().longValue());
                if (old == null) {
                    throw AcademicServiceSupport.failure(AcademicCommands.COURSE_NOT_FOUND,
                            "课程不存在");
                }
                if (CourseStatus.ARCHIVED.name().equals(old.getStatus())) {
                    throw AcademicServiceSupport.failure(AcademicCommands.COURSE_CLOSED,
                            "归档课程不能修改");
                }
                if (selected.getCapacity().intValue() < repository.countEnrolled(
                        connection, old.getId())) {
                    throw AcademicServiceSupport.failure(AcademicCommands.CAPACITY_TOO_SMALL,
                            "课程容量不能小于已选人数");
                }
                support.verifyTeachers(connection, selected.getInstructorUserIds());
                return repository.saveCourse(connection, selected, actor);
            }
        });
    }

    CoursePageDto teacherCourses(SessionContext session, CourseQuery query)
            throws AcademicException {
        support.requirePermission(session, Permission.COURSE_TEACH, Role.TEACHER);
        final CourseQuery selected = support.validQuery(query);
        final long teacherId = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<CoursePageDto>() {
            @Override
            public CoursePageDto run(java.sql.Connection connection) throws Exception {
                return repository().findCoursesByTeacher(connection, teacherId, selected);
            }
        });
    }

    private AcademicRepository repository() {
        return support.repository();
    }
}
