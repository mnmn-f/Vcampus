package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseInstructorDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 将内存状态映射为公共传输对象。 */
final class InMemoryAcademicMapper {
    private final InMemoryAcademicState state;

    InMemoryAcademicMapper(InMemoryAcademicState state) {
        this.state = state;
    }

    CourseDto course(InMemoryAcademicState.CourseState source) {
        if (source == null) {
            return null;
        }
        List<CourseScheduleDto> scheduleList = new ArrayList<CourseScheduleDto>();
        for (InMemoryAcademicState.ScheduleState schedule : state.schedules.values()) {
            if (schedule.courseId == source.id) {
                scheduleList.add(schedule(schedule));
            }
        }
        Collections.sort(scheduleList, new Comparator<CourseScheduleDto>() {
            @Override
            public int compare(CourseScheduleDto first, CourseScheduleDto second) {
                int weekday = Integer.compare(first.getWeekday(), second.getWeekday());
                if (weekday != 0) {
                    return weekday;
                }
                int period = Integer.compare(first.getStartPeriod(), second.getStartPeriod());
                return period == 0 ? Long.compare(first.getId(), second.getId()) : period;
            }
        });
        List<CourseInstructorDto> teachers = new ArrayList<CourseInstructorDto>();
        for (int i = 0; i < source.teacherIds.size(); i++) {
            Long id = source.teacherIds.get(i);
            teachers.add(new CourseInstructorDto(id, state.teachers.get(id),
                    "T" + id,
                    i == 0 ? "PRIMARY" : "ASSISTANT"));
        }
        return new CourseDto(source.id, source.code, source.name, source.type,
                source.credits, source.totalHours, source.capacity,
                countEnrolled(source.id), source.description, source.status,
                scheduleList, teachers, source.semesterCode);
    }

    CourseScheduleDto schedule(InMemoryAcademicState.ScheduleState source) {
        if (source == null) {
            return null;
        }
        return new CourseScheduleDto(source.id, source.courseId, source.weekday,
                source.startPeriod, source.endPeriod, source.startDate, source.endDate,
                source.classroomId == null ? null : state.classrooms.get(source.classroomId));
    }

    EnrollmentDto enrollment(InMemoryAcademicState.EnrollmentState source) {
        return source == null ? null : new EnrollmentDto(source.id, source.studentId,
                source.courseId, source.status, source.enrolledAt, source.droppedAt);
    }

    long countEnrolled(long courseId) {
        long count = 0L;
        for (InMemoryAcademicState.EnrollmentState enrollment : state.enrollments.values()) {
            if (enrollment.courseId == courseId
                    && EnrollmentStatus.ENROLLED.name().equals(enrollment.status)) {
                count++;
            }
        }
        return count;
    }
}
