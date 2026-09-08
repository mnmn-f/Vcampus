package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 内存课程、时段、教室和教师关系存储。 */
final class InMemoryCourseStore {
    private final InMemoryAcademicState state;
    private final InMemoryAcademicMapper mapper;

    InMemoryCourseStore(InMemoryAcademicState state, InMemoryAcademicMapper mapper) {
        this.state = state;
        this.mapper = mapper;
    }

    CoursePageDto findCourses(CourseQuery q, Long teacherId) {
        CourseQuery query = q == null ? new CourseQuery() : q;
        List<CourseDto> result = new ArrayList<CourseDto>();
        for (InMemoryAcademicState.CourseState source : state.courses.values()) {
            if (teacherId != null && !source.teacherIds.contains(teacherId)
                    || !matches(source, query)) {
                continue;
            }
            result.add(mapper.course(source));
        }
        Collections.sort(result, new Comparator<CourseDto>() {
            @Override
            public int compare(CourseDto first, CourseDto second) {
                return Long.compare(second.getId(), first.getId());
            }
        });
        int from = Math.min((query.getPageNumber() - 1) * query.getPageSize(), result.size());
        int to = Math.min(from + query.getPageSize(), result.size());
        return new CoursePageDto(query.getPageNumber(), query.getPageSize(), result.size(),
                result.subList(from, to));
    }

    CourseDto findCourse(long id) {
        return mapper.course(state.courses.get(id));
    }

    CourseDto saveCourse(CourseSaveRequest request) throws SQLException {
        InMemoryAcademicState.CourseState course;
        if (request.isUpdate()) {
            course = state.courses.get(request.getCourseId());
            if (course == null) {
                throw new SQLException("course not found");
            }
        } else {
            course = new InMemoryAcademicState.CourseState(state.courseIds.incrementAndGet());
            state.courses.put(course.id, course);
        }
        course.code = request.getCourseCode();
        course.name = request.getCourseName();
        course.type = request.getCourseType();
        course.semesterCode = request.getSemesterCode();
        course.credits = request.getCredits();
        course.totalHours = request.getTotalHours();
        course.capacity = request.getCapacity();
        course.description = request.getDescription();
        course.status = request.getStatus();
        course.teacherIds.clear();
        course.teacherIds.addAll(request.getInstructorUserIds());
        return mapper.course(course);
    }

    CourseScheduleDto saveSchedule(ScheduleSaveRequest request) throws SQLException {
        InMemoryAcademicState.ScheduleState schedule;
        if (request.isUpdate()) {
            schedule = state.schedules.get(request.getScheduleId());
            if (schedule == null) {
                throw new SQLException("schedule not found");
            }
        } else {
            schedule = new InMemoryAcademicState.ScheduleState(
                    state.scheduleIds.incrementAndGet());
            state.schedules.put(schedule.id, schedule);
        }
        schedule.courseId = request.getCourseId();
        schedule.weekday = request.getWeekday();
        schedule.startPeriod = request.getStartPeriod();
        schedule.endPeriod = request.getEndPeriod();
        schedule.startDate = request.getStartDate();
        schedule.endDate = request.getEndDate();
        schedule.classroomId = request.getClassroomId();
        return mapper.schedule(schedule);
    }

    boolean deleteSchedule(long id) {
        return state.schedules.remove(id) != null;
    }

    CourseScheduleDto findSchedule(long id) {
        return mapper.schedule(state.schedules.get(id));
    }

    boolean allActiveTeachers(List<Long> ids) {
        if (ids == null) {
            return false;
        }
        for (Long id : ids) {
            if (id == null || !state.teachers.containsKey(id)) {
                return false;
            }
        }
        return true;
    }

    boolean classroomAvailable(Long id) {
        if (id == null) {
            return true;
        }
        ClassroomDto classroom = state.classrooms.get(id);
        return classroom != null && "AVAILABLE".equals(classroom.getStatus());
    }

    boolean hasScheduleConflict(ScheduleSaveRequest candidate) {
        for (InMemoryAcademicState.ScheduleState old : state.schedules.values()) {
            if (old.courseId == candidate.getCourseId()
                    && notSame(old.id, candidate.getScheduleId())
                    && overlaps(old, candidate)) {
                return true;
            }
        }
        return false;
    }

    boolean hasClassroomConflict(ScheduleSaveRequest candidate) {
        if (candidate.getClassroomId() == null) {
            return false;
        }
        for (InMemoryAcademicState.ScheduleState old : state.schedules.values()) {
            if (candidate.getClassroomId().equals(old.classroomId)
                    && notSame(old.id, candidate.getScheduleId())
                    && overlaps(old, candidate)) {
                return true;
            }
        }
        return false;
    }

    int count() {
        return state.courses.size();
    }

    private boolean matches(InMemoryAcademicState.CourseState source, CourseQuery query) {
        return (query.getKeyword() == null || contains(source.code, query.getKeyword())
                || contains(source.name, query.getKeyword()))
                && (query.getCourseCode() == null
                || contains(source.code, query.getCourseCode()))
                && (query.getCourseName() == null
                || contains(source.name, query.getCourseName()))
                && (query.getStatus() == null || query.getStatus().equals(source.status))
                && (query.getCourseType() == null || query.getCourseType().equals(source.type));
    }

    private boolean overlaps(InMemoryAcademicState.ScheduleState old,
                             ScheduleSaveRequest candidate) {
        return old.weekday == candidate.getWeekday()
                && old.startPeriod <= candidate.getEndPeriod()
                && old.endPeriod >= candidate.getStartPeriod()
                && datesOverlap(old.startDate, old.endDate,
                candidate.getStartDate(), candidate.getEndDate());
    }

    private static boolean datesOverlap(org.threeten.bp.LocalDate firstStart,
                                       org.threeten.bp.LocalDate firstEnd,
                                       org.threeten.bp.LocalDate secondStart,
                                       org.threeten.bp.LocalDate secondEnd) {
        return (firstStart == null || secondEnd == null || !firstStart.isAfter(secondEnd))
                && (firstEnd == null || secondStart == null || !firstEnd.isBefore(secondStart));
    }

    private static boolean notSame(long id, Long other) {
        return other == null || id != other.longValue();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }
}
