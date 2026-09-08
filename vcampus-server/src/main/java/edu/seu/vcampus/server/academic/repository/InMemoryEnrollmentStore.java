package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;

import java.sql.SQLException;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 内存学生身份、选课关系和个人课表存储。 */
final class InMemoryEnrollmentStore {
    private final InMemoryAcademicState state;
    private final InMemoryAcademicMapper mapper;
    private final InMemoryCourseStore courses;

    InMemoryEnrollmentStore(InMemoryAcademicState state, InMemoryAcademicMapper mapper,
                            InMemoryCourseStore courses) {
        this.state = state;
        this.mapper = mapper;
        this.courses = courses;
    }

    boolean isActiveStudent(long userId) {
        return state.students.contains(userId);
    }

    EnrollmentDto find(long studentId, long courseId) {
        return mapper.enrollment(state.enrollments.get(key(studentId, courseId)));
    }

    void add(long id, long studentId, long courseId, String status) {
        InMemoryAcademicState.EnrollmentState value =
                new InMemoryAcademicState.EnrollmentState(id, studentId, courseId);
        value.status = status;
        state.enrollments.put(key(studentId, courseId), value);
    }

    long countEnrolled(long courseId) {
        return mapper.countEnrolled(courseId);
    }

    boolean hasScheduleConflict(long studentId, long courseId) {
        for (InMemoryAcademicState.EnrollmentState enrollment : state.enrollments.values()) {
            if (enrollment.studentId == studentId
                    && enrollment.courseId != courseId
                    && EnrollmentStatus.ENROLLED.name().equals(enrollment.status)
                    && coursesConflict(enrollment.courseId, courseId)) {
                return true;
            }
        }
        return false;
    }

    EnrollmentDto enroll(long studentId, long courseId) {
        String key = key(studentId, courseId);
        InMemoryAcademicState.EnrollmentState enrollment = state.enrollments.get(key);
        if (enrollment == null) {
            enrollment = new InMemoryAcademicState.EnrollmentState(
                    state.enrollmentIds.incrementAndGet(), studentId, courseId);
            state.enrollments.put(key, enrollment);
        }
        enrollment.status = EnrollmentStatus.ENROLLED.name();
        enrollment.enrolledAt = LocalDateTime.now();
        enrollment.droppedAt = null;
        return mapper.enrollment(enrollment);
    }

    void drop(long studentId, long courseId) throws SQLException {
        InMemoryAcademicState.EnrollmentState enrollment =
                state.enrollments.get(key(studentId, courseId));
        if (enrollment == null
                || !EnrollmentStatus.ENROLLED.name().equals(enrollment.status)) {
            throw new SQLException("enrollment is not active");
        }
        enrollment.status = EnrollmentStatus.DROPPED.name();
        enrollment.droppedAt = LocalDateTime.now();
    }

    StudentScheduleDto schedule(long studentId) {
        return schedule(studentId, StudentScheduleQuery.all());
    }

    StudentScheduleDto schedule(long studentId, StudentScheduleQuery query) {
        String semesterCode = query == null ? null : query.getSemesterCode();
        List<CourseDto> result = new ArrayList<CourseDto>();
        for (InMemoryAcademicState.EnrollmentState enrollment : state.enrollments.values()) {
            if (enrollment.studentId == studentId
                    && EnrollmentStatus.ENROLLED.name().equals(enrollment.status)) {
                CourseDto course = courses.findCourse(enrollment.courseId);
                if (course != null && (semesterCode == null
                        || semesterCode.equals(course.getSemesterCode()))) {
                    result.add(course);
                }
            }
        }
        Collections.sort(result, new Comparator<CourseDto>() {
            @Override
            public int compare(CourseDto first, CourseDto second) {
                return Long.compare(first.getId(), second.getId());
            }
        });
        return new StudentScheduleDto(studentId, semesterCode, result);
    }

    StudentEnrollmentListDto enrollments(long studentId) {
        List<StudentEnrollmentDto> result = new ArrayList<StudentEnrollmentDto>();
        for (InMemoryAcademicState.EnrollmentState value : state.enrollments.values()) {
            if (value.studentId != studentId) continue;
            CourseDto course = courses.findCourse(value.courseId);
            if (course != null) result.add(new StudentEnrollmentDto(mapper.enrollment(value), course));
        }
        Collections.sort(result, new Comparator<StudentEnrollmentDto>() {
            @Override public int compare(StudentEnrollmentDto left, StudentEnrollmentDto right) {
                return Long.compare(right.getEnrollment().getId(), left.getEnrollment().getId());
            }
        });
        return new StudentEnrollmentListDto(result);
    }

    CourseRosterDto roster(long courseId) {
        List<CourseRosterEntryDto> result = new ArrayList<CourseRosterEntryDto>();
        for (InMemoryAcademicState.EnrollmentState enrollment : state.enrollments.values()) {
            if (enrollment.courseId != courseId
                    || EnrollmentStatus.DROPPED.name().equals(enrollment.status)) continue;
            InMemoryAcademicState.StudentState student =
                    state.studentProfiles.get(enrollment.studentId);
            result.add(new CourseRosterEntryDto(enrollment.id, enrollment.studentId,
                    student == null ? null : student.studentNo,
                    student == null ? null : student.displayName,
                    student == null ? null : student.college,
                    student == null ? null : student.major,
                    student == null ? null : student.className,
                    enrollment.status, enrollment.enrolledAt));
        }
        Collections.sort(result, new Comparator<CourseRosterEntryDto>() {
            @Override public int compare(CourseRosterEntryDto left,
                                         CourseRosterEntryDto right) {
                String first = left.getStudentNo() == null ? "" : left.getStudentNo();
                String second = right.getStudentNo() == null ? "" : right.getStudentNo();
                int compared = first.compareTo(second);
                return compared != 0 ? compared
                        : Long.compare(left.getEnrollmentId(), right.getEnrollmentId());
            }
        });
        return new CourseRosterDto(courseId, result);
    }

    private boolean coursesConflict(long firstId, long secondId) {
        List<InMemoryAcademicState.ScheduleState> first = schedules(firstId);
        List<InMemoryAcademicState.ScheduleState> second = schedules(secondId);
        for (InMemoryAcademicState.ScheduleState left : first) {
            for (InMemoryAcademicState.ScheduleState right : second) {
                if (overlaps(left, right)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<InMemoryAcademicState.ScheduleState> schedules(long courseId) {
        List<InMemoryAcademicState.ScheduleState> result =
                new ArrayList<InMemoryAcademicState.ScheduleState>();
        for (InMemoryAcademicState.ScheduleState schedule : state.schedules.values()) {
            if (schedule.courseId == courseId) {
                result.add(schedule);
            }
        }
        return result;
    }

    private static boolean overlaps(InMemoryAcademicState.ScheduleState first,
                                    InMemoryAcademicState.ScheduleState second) {
        return first.weekday == second.weekday && first.startPeriod <= second.endPeriod
                && first.endPeriod >= second.startPeriod
                && (first.startDate == null || second.endDate == null
                || !first.startDate.isAfter(second.endDate))
                && (first.endDate == null || second.startDate == null
                || !first.endDate.isBefore(second.startDate));
    }

    private static String key(long studentId, long courseId) {
        return studentId + ":" + courseId;
    }
}
