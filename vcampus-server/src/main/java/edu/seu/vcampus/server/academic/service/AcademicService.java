package edu.seu.vcampus.server.academic.service;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

/** 教务服务门面；业务规则按课程、排课、选课职责拆分到专责服务。 */
public final class AcademicService {
    private final AcademicCourseService courses;
    private final AcademicScheduleService schedules;
    private final AcademicEnrollmentService enrollments;

    /** 生产环境使用 TransactionManager，保证 MySQL 业务写操作有统一事务。 */
    public AcademicService(AcademicRepository repository, TransactionManager transactions) {
        if (repository == null) {
            throw new IllegalArgumentException("repository is required");
        }
        AcademicServiceSupport support = new AcademicServiceSupport(repository, transactions);
        this.courses = new AcademicCourseService(support);
        this.schedules = new AcademicScheduleService(support);
        this.enrollments = new AcademicEnrollmentService(support);
    }

    /** 测试和离线演示使用内存仓储，不需要 JDBC 连接。 */
    public AcademicService(AcademicRepository repository) {
        this(repository, null);
    }

    public CoursePageDto queryCourses(SessionContext session, CourseQuery query)
            throws AcademicException {
        return courses.query(session, query);
    }

    public CoursePageDto listCourses(SessionContext session, CourseQuery query)
            throws AcademicException {
        return queryCourses(session, query);
    }

    public CourseDto createCourse(SessionContext session, CourseSaveRequest request)
            throws AcademicException {
        return courses.create(session, request);
    }

    public CourseDto updateCourse(SessionContext session, CourseSaveRequest request)
            throws AcademicException {
        return courses.update(session, request);
    }

    public CourseDto saveCourse(SessionContext session, CourseSaveRequest request)
            throws AcademicException {
        return request != null && request.isUpdate()
                ? updateCourse(session, request) : createCourse(session, request);
    }

    public CourseScheduleDto createSchedule(SessionContext session,
                                            ScheduleSaveRequest request)
            throws AcademicException {
        return schedules.create(session, request);
    }

    public CourseScheduleDto updateSchedule(SessionContext session,
                                            ScheduleSaveRequest request)
            throws AcademicException {
        return schedules.update(session, request);
    }

    public CourseScheduleDto saveSchedule(SessionContext session,
                                          ScheduleSaveRequest request)
            throws AcademicException {
        return request != null && request.isUpdate()
                ? updateSchedule(session, request) : createSchedule(session, request);
    }

    public void deleteSchedule(SessionContext session, long scheduleId)
            throws AcademicException {
        schedules.delete(session, scheduleId);
    }

    public EnrollmentDto enroll(SessionContext session, long courseId)
            throws AcademicException {
        return enrollments.enroll(session, courseId);
    }

    public void drop(SessionContext session, long courseId) throws AcademicException {
        enrollments.drop(session, courseId);
    }

    public StudentScheduleDto studentSchedule(SessionContext session)
            throws AcademicException {
        return enrollments.schedule(session);
    }

    public CoursePageDto teacherCourses(SessionContext session, CourseQuery query)
            throws AcademicException {
        return courses.teacherCourses(session, query);
    }
}
