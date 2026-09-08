package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.academic.GpaScale;

import java.sql.Connection;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 成绩服务测试桩；用显式选课和授课关系模拟数据范围。 */
final class InMemoryStudentGradeRepository
        implements DelegatingStudentRecordRepository.GradeStore {
    private final Map<Long, EnrollmentRecord> enrollments =
            new HashMap<Long, EnrollmentRecord>();
    private final Map<Long, CourseInfo> courses = new HashMap<Long, CourseInfo>();
    private final Set<String> instructors = new HashSet<String>();
    private final Map<Long, StudentGradeDto> grades = new HashMap<Long, StudentGradeDto>();
    private long nextGradeId = 1L;

    void addCourse(long courseId, String code, String name) {
        addCourse(courseId, code, name, null, null);
    }

    void addCourse(long courseId, String code, String name, BigDecimal credits,
                   String semesterCode) {
        courses.put(courseId, new CourseInfo(code, name, credits, semesterCode));
    }

    void addEnrollment(long id, long studentUserId, long courseId, String status) {
        enrollments.put(id, new EnrollmentRecord(id, studentUserId, courseId, status));
    }

    void addInstructor(long courseId, long teacherUserId) {
        instructors.add(key(courseId, teacherUserId));
    }

    void addGrade(StudentGradeDto grade) {
        grades.put(grade.getEnrollmentId(), normalize(grade));
        nextGradeId = Math.max(nextGradeId, grade.getGradeId() + 1L);
    }

    private static StudentGradeDto normalize(StudentGradeDto grade) {
        if (grade.getScore() == null) return grade;
        return new StudentGradeDto(grade.getGradeId(), grade.getEnrollmentId(),
                grade.getStudentUserId(), grade.getCourseId(), grade.getCourseCode(),
                grade.getCourseName(), grade.getScore(), GpaScale.point(grade.getScore()),
                grade.getRecordedBy(), grade.getRecordedAt(), grade.getRemark(),
                grade.getEnrollmentStatus(), grade.getSemesterCode(), grade.getCredits(),
                grade.isGpaIncluded());
    }

    @Override
    public StudentGradePage find(Connection ignored, long studentUserId, StudentGradeQuery query) {
        return page(studentUserId, query.getCourseId(), query.getSemesterCode(),
                query.getPage(), query.getPageSize(),
                query.getOffset());
    }

    @Override
    public StudentGradePage review(Connection ignored, StudentGradeReviewQuery query) {
        return page(query.getStudentUserId(), query.getCourseId(), null, query.getPage(),
                query.getPageSize(), query.getOffset());
    }

    @Override
    public List<StudentGradeDto> findAll(Connection ignored, long studentUserId) {
        return findAll(ignored, studentUserId, null, 0);
    }

    @Override
    public List<StudentGradeDto> findAll(Connection ignored, long studentUserId,
                                         String semesterCode, int limit) {
        return findAll(ignored, studentUserId, semesterCode, null, limit);
    }

    @Override
    public List<StudentGradeDto> findAll(Connection ignored, long studentUserId,
                                         String semesterCode, Long courseId, int limit) {
        List<StudentGradeDto> result = rows(studentUserId, courseId, semesterCode);
        if (limit > 0 && result.size() > limit) {
            return new ArrayList<StudentGradeDto>(result.subList(0, limit));
        }
        return result;
    }

    @Override
    public StudentGradeDto findByEnrollment(Connection ignored, long enrollmentId) {
        return grades.get(enrollmentId);
    }

    @Override
    public EnrollmentRecord findEnrollment(Connection ignored, long enrollmentId) {
        return enrollments.get(enrollmentId);
    }

    @Override
    public boolean teacherOwnsCourse(Connection ignored, long teacherUserId, long courseId) {
        return instructors.contains(key(courseId, teacherUserId));
    }

    @Override
    public void upsert(Connection ignored, long recorderUserId, StudentGradeRecordRequest request) {
        EnrollmentRecord enrollment = enrollments.get(request.getEnrollmentId());
        CourseInfo course = courses.get(enrollment.getCourseId());
        StudentGradeDto old = grades.get(request.getEnrollmentId());
        long gradeId = old == null ? nextGradeId++ : old.getGradeId();
        grades.put(request.getEnrollmentId(), new StudentGradeDto(gradeId,
                enrollment.getId(), enrollment.getStudentUserId(), enrollment.getCourseId(),
                course == null ? "" : course.code, course == null ? "" : course.name,
                request.getScore(), GpaScale.point(request.getScore()), recorderUserId,
                LocalDateTime.now(), request.getRemark(), enrollment.getStatus(),
                course == null ? null : course.semesterCode,
                course == null ? null : course.credits, true));
    }

    private StudentGradePage page(Long studentUserId, Long courseId, String semesterCode,
                                  int page,
                                  int pageSize, int offset) {
        List<StudentGradeDto> rows = rows(studentUserId, courseId, semesterCode);
        long total = rows.size();
        int from = Math.min(offset, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new StudentGradePage(rows.subList(from, to), total, page, pageSize);
    }

    private List<StudentGradeDto> rows(Long studentUserId, Long courseId,
                                       String semesterCode) {
        List<StudentGradeDto> rows = new ArrayList<StudentGradeDto>();
        for (StudentGradeDto grade : grades.values()) {
            EnrollmentRecord enrollment = enrollments.get(grade.getEnrollmentId());
            if (enrollment == null || "DROPPED".equals(enrollment.getStatus())) continue;
            if (studentUserId != null && enrollment.getStudentUserId() != studentUserId) continue;
            if (courseId != null && enrollment.getCourseId() != courseId) continue;
            if (semesterCode != null && !semesterCode.equals(grade.getSemesterCode())) continue;
            rows.add(grade);
        }
        Collections.sort(rows, new Comparator<StudentGradeDto>() {
            @Override
            public int compare(StudentGradeDto left, StudentGradeDto right) {
                int code = left.getCourseCode().compareTo(right.getCourseCode());
                return code == 0 ? Long.compare(left.getEnrollmentId(), right.getEnrollmentId()) : code;
            }
        });
        return rows;
    }

    private static String key(long courseId, long teacherUserId) {
        return courseId + ":" + teacherUserId;
    }

    private static final class CourseInfo {
        private final String code;
        private final String name;
        private final BigDecimal credits;
        private final String semesterCode;

        private CourseInfo(String code, String name, BigDecimal credits, String semesterCode) {
            this.code = code;
            this.name = name;
            this.credits = credits;
            this.semesterCode = semesterCode;
        }
    }
}
