package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** 内存仓储共享状态，仅供测试实现使用。 */
final class InMemoryAcademicState {
    final AtomicLong courseIds = new AtomicLong(100L);
    final AtomicLong scheduleIds = new AtomicLong(1000L);
    final AtomicLong enrollmentIds = new AtomicLong(10000L);
    final Map<Long, CourseState> courses = new HashMap<Long, CourseState>();
    final Map<Long, ScheduleState> schedules = new HashMap<Long, ScheduleState>();
    final Map<String, EnrollmentState> enrollments = new HashMap<String, EnrollmentState>();
    final Map<Long, ClassroomDto> classrooms = new HashMap<Long, ClassroomDto>();
    final Set<Long> students = new HashSet<Long>();
    final Map<Long, String> teachers = new HashMap<Long, String>();

    static final class CourseState {
        final long id;
        String code;
        String name;
        String type;
        BigDecimal credits;
        Integer totalHours;
        int capacity;
        String description;
        String status;
        final List<Long> teacherIds = new ArrayList<Long>();

        CourseState(long id) {
            this.id = id;
        }
    }

    static final class ScheduleState {
        final long id;
        long courseId;
        int weekday;
        int startPeriod;
        int endPeriod;
        LocalDate startDate;
        LocalDate endDate;
        Long classroomId;

        ScheduleState(long id) {
            this.id = id;
        }
    }

    static final class EnrollmentState {
        final long id;
        final long studentId;
        final long courseId;
        String status = EnrollmentStatus.DROPPED.name();
        LocalDateTime enrolledAt;
        LocalDateTime droppedAt;

        EnrollmentState(long id, long studentId, long courseId) {
            this.id = id;
            this.studentId = studentId;
            this.courseId = courseId;
        }
    }
}
