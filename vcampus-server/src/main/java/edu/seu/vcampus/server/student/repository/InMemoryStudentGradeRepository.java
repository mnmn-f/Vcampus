package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;

import java.sql.Connection;
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
    private final Map<Long, String[]> courses = new HashMap<Long, String[]>();
    private final Set<String> instructors = new HashSet<String>();
    private final Map<Long, StudentGradeDto> grades = new HashMap<Long, StudentGradeDto>();
    private long nextGradeId = 1L;

    void addCourse(long courseId, String code, String name) {
        courses.put(courseId, new String[]{code, name});
    }

    void addEnrollment(long id, long studentUserId, long courseId, String status) {
        enrollments.put(id, new EnrollmentRecord(id, studentUserId, courseId, status));
    }

    void addInstructor(long courseId, long teacherUserId) {
        instructors.add(key(courseId, teacherUserId));
    }

    void addGrade(StudentGradeDto grade) {
        grades.put(grade.getEnrollmentId(), grade);
        nextGradeId = Math.max(nextGradeId, grade.getGradeId() + 1L);
    }

    @Override
    public StudentGradePage find(Connection ignored, long studentUserId, StudentGradeQuery query) {
        return page(studentUserId, query.getCourseId(), query.getPage(), query.getPageSize(),
                query.getOffset());
    }

    @Override
    public StudentGradePage review(Connection ignored, StudentGradeReviewQuery query) {
        return page(query.getStudentUserId(), query.getCourseId(), query.getPage(),
                query.getPageSize(), query.getOffset());
    }

    @Override
    public List<StudentGradeDto> findAll(Connection ignored, long studentUserId) {
        List<StudentGradeDto> rows = rows(studentUserId, null);
        return rows;
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
        String[] course = courses.get(enrollment.getCourseId());
        StudentGradeDto old = grades.get(request.getEnrollmentId());
        long gradeId = old == null ? nextGradeId++ : old.getGradeId();
        grades.put(request.getEnrollmentId(), new StudentGradeDto(gradeId,
                enrollment.getId(), enrollment.getStudentUserId(), enrollment.getCourseId(),
                course == null ? "" : course[0], course == null ? "" : course[1],
                request.getScore(), request.getGradePoint(), recorderUserId,
                LocalDateTime.now(), request.getRemark(), enrollment.getStatus()));
    }

    private StudentGradePage page(Long studentUserId, Long courseId, int page,
                                  int pageSize, int offset) {
        List<StudentGradeDto> rows = rows(studentUserId, courseId);
        long total = rows.size();
        int from = Math.min(offset, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new StudentGradePage(rows.subList(from, to), total, page, pageSize);
    }

    private List<StudentGradeDto> rows(Long studentUserId, Long courseId) {
        List<StudentGradeDto> rows = new ArrayList<StudentGradeDto>();
        for (StudentGradeDto grade : grades.values()) {
            EnrollmentRecord enrollment = enrollments.get(grade.getEnrollmentId());
            if (enrollment == null || "DROPPED".equals(enrollment.getStatus())) continue;
            if (studentUserId != null && enrollment.getStudentUserId() != studentUserId) continue;
            if (courseId != null && enrollment.getCourseId() != courseId) continue;
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
}
