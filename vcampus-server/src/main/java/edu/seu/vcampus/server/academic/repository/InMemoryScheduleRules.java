package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;

/** 内存实现复用的排课冲突规则，保持测试仓储与 MySQL 规则一致。 */
final class InMemoryScheduleRules {
    private InMemoryScheduleRules() { }

    static boolean hasTeacherOrStudentConflict(InMemoryAcademicState state,
                                                ScheduleSaveRequest candidate) {
        for (InMemoryAcademicState.ScheduleState old : state.schedules.values()) {
            if (!same(old, candidate) && overlaps(old, candidate)
                    && related(state, old.courseId, candidate.getCourseId())) {
                return true;
            }
        }
        return false;
    }

    static boolean overlaps(InMemoryAcademicState.ScheduleState old,
                             ScheduleSaveRequest candidate) {
        return old.weekday == candidate.getWeekday()
                && old.startPeriod <= candidate.getEndPeriod()
                && old.endPeriod >= candidate.getStartPeriod()
                && datesOverlap(old.startDate, old.endDate,
                candidate.getStartDate(), candidate.getEndDate());
    }

    private static boolean related(InMemoryAcademicState state, long first, long second) {
        if (first == second) return true;
        InMemoryAcademicState.CourseState left = state.courses.get(first);
        InMemoryAcademicState.CourseState right = state.courses.get(second);
        if (left != null && right != null) {
            for (Long teacher : left.teacherIds) if (right.teacherIds.contains(teacher)) return true;
        }
        for (InMemoryAcademicState.EnrollmentState value : state.enrollments.values()) {
            if (value.courseId == first && EnrollmentStatus.ENROLLED.name().equals(value.status)
                    && hasActiveEnrollment(state, value.studentId, second)) return true;
        }
        return false;
    }

    private static boolean hasActiveEnrollment(InMemoryAcademicState state, long student,
                                               long course) {
        for (InMemoryAcademicState.EnrollmentState value : state.enrollments.values()) {
            if (value.studentId == student && value.courseId == course
                    && EnrollmentStatus.ENROLLED.name().equals(value.status)) return true;
        }
        return false;
    }

    private static boolean same(InMemoryAcademicState.ScheduleState old,
                                ScheduleSaveRequest candidate) {
        return candidate.getScheduleId() != null
                && old.id == candidate.getScheduleId().longValue();
    }

    private static boolean datesOverlap(org.threeten.bp.LocalDate firstStart,
                                        org.threeten.bp.LocalDate firstEnd,
                                        org.threeten.bp.LocalDate secondStart,
                                        org.threeten.bp.LocalDate secondEnd) {
        return (firstStart == null || secondEnd == null || !firstStart.isAfter(secondEnd))
                && (firstEnd == null || secondStart == null || !firstEnd.isBefore(secondStart));
    }
}
