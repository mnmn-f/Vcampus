package edu.seu.vcampus.server.academic.service;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 学生选课、退课和个人课表；选课检查在同一个事务中完成。 */
final class AcademicEnrollmentService {
    private final AcademicServiceSupport support;

    AcademicEnrollmentService(AcademicServiceSupport support) {
        this.support = support;
    }

    EnrollmentDto enroll(SessionContext session, final long courseId)
            throws AcademicException {
        support.requirePermission(session, Permission.COURSE_ENROLL, Role.STUDENT);
        if (courseId <= 0) {
            throw AcademicServiceSupport.failure(AcademicCommands.INVALID_COURSE,
                    "课程编号不正确");
        }
        final long studentId = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<EnrollmentDto>() {
            @Override
            public EnrollmentDto run(java.sql.Connection connection) throws Exception {
                AcademicRepository repository = repository();
                CourseDto course = repository.lockCourse(connection, courseId);
                if (course == null) {
                    throw failure(AcademicCommands.COURSE_NOT_FOUND, "课程不存在");
                }
                if (!CourseStatus.PUBLISHED.name().equals(course.getStatus())) {
                    throw failure(AcademicCommands.COURSE_NOT_PUBLISHED, "当前课程未开放选课");
                }
                support.verifyStudent(connection, studentId);
                EnrollmentDto old = repository.findEnrollment(connection, studentId,
                        courseId, true);
                if (old != null && EnrollmentStatus.ENROLLED.name().equals(old.getStatus())) {
                    throw failure(AcademicCommands.DUPLICATE_ENROLLMENT, "你已经选过该课程");
                }
                if (old != null && EnrollmentStatus.COMPLETED.name().equals(old.getStatus())) {
                    throw failure(AcademicCommands.ENROLLMENT_COMPLETED,
                            "已完成课程不能重复选课");
                }
                if (repository.countEnrolled(connection, courseId) >= course.getCapacity()) {
                    throw failure(AcademicCommands.COURSE_CAPACITY_FULL, "课程已满");
                }
                if (repository.hasStudentScheduleConflict(connection, studentId, courseId)) {
                    throw failure(AcademicCommands.SCHEDULE_CONFLICT, "与已有课程时间冲突");
                }
                return repository.enroll(connection, studentId, courseId);
            }
        });
    }

    void drop(SessionContext session, final long courseId) throws AcademicException {
        support.requirePermission(session, Permission.COURSE_ENROLL, Role.STUDENT);
        if (courseId <= 0) {
            throw AcademicServiceSupport.failure(AcademicCommands.INVALID_COURSE,
                    "课程编号不正确");
        }
        final long studentId = session.getUserId();
        support.execute(new AcademicServiceSupport.Work<Void>() {
            @Override
            public Void run(java.sql.Connection connection) throws Exception {
                AcademicRepository repository = repository();
                CourseDto course = repository.lockCourse(connection, courseId);
                if (course == null) {
                    throw failure(AcademicCommands.COURSE_NOT_FOUND, "课程不存在");
                }
                if (!CourseStatus.PUBLISHED.name().equals(course.getStatus())) {
                    throw failure(AcademicCommands.COURSE_CLOSED, "当前课程不允许退课");
                }
                EnrollmentDto enrollment = repository.findEnrollment(connection, studentId,
                        courseId, true);
                if (enrollment == null) {
                    throw failure(AcademicCommands.ENROLLMENT_NOT_FOUND, "没有找到有效选课记录");
                }
                if (EnrollmentStatus.COMPLETED.name().equals(enrollment.getStatus())) {
                    throw failure(AcademicCommands.ENROLLMENT_COMPLETED,
                            "已完成课程不能退选");
                }
                if (!EnrollmentStatus.ENROLLED.name().equals(enrollment.getStatus())) {
                    throw failure(AcademicCommands.ENROLLMENT_NOT_FOUND, "没有找到有效选课记录");
                }
                repository.drop(connection, studentId, courseId);
                return null;
            }
        });
    }

    StudentScheduleDto schedule(SessionContext session) throws AcademicException {
        return schedule(session, StudentScheduleQuery.all());
    }

    StudentScheduleDto schedule(SessionContext session, final StudentScheduleQuery query)
            throws AcademicException {
        support.requirePermission(session, Permission.COURSE_ENROLL, Role.STUDENT);
        final StudentScheduleQuery safe = query == null ? StudentScheduleQuery.all() : query;
        if (safe.getSemesterCode() != null && safe.getSemesterCode().length() > 32) {
            throw AcademicServiceSupport.failure(AcademicCommands.INVALID_SCHEDULE,
                    "学期编号长度不能超过32个字符");
        }
        final long studentId = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<StudentScheduleDto>() {
            @Override
            public StudentScheduleDto run(java.sql.Connection connection) throws Exception {
                support.verifyStudent(connection, studentId);
                return repository().findStudentSchedule(connection, studentId, safe);
            }
        });
    }

    StudentEnrollmentListDto enrollments(SessionContext session) throws AcademicException {
        support.requirePermission(session, Permission.COURSE_ENROLL, Role.STUDENT);
        final long studentId = session.getUserId();
        return support.execute(new AcademicServiceSupport.Work<StudentEnrollmentListDto>() {
            @Override public StudentEnrollmentListDto run(java.sql.Connection connection)
                    throws Exception {
                support.verifyStudent(connection, studentId);
                return repository().findStudentEnrollments(connection, studentId);
            }
        });
    }

    private AcademicRepository repository() {
        return support.repository();
    }

    private static AcademicException failure(String code, String message) {
        return AcademicServiceSupport.failure(code, message);
    }
}
