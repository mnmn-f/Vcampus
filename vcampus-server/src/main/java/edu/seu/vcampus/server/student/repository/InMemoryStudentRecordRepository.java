package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;

import java.math.BigDecimal;

/** 可重复使用的学籍与成绩内存仓储，供单元测试和本地演示使用。 */
public final class InMemoryStudentRecordRepository
        extends DelegatingStudentRecordRepository {
    private final InMemoryStudentProfileRepository profileStore;
    private final InMemoryStudentGradeRepository gradeStore;

    public InMemoryStudentRecordRepository() {
        this(new InMemoryStudentProfileRepository(), new InMemoryStudentGradeRepository());
    }

    private InMemoryStudentRecordRepository(InMemoryStudentProfileRepository profiles,
                                            InMemoryStudentGradeRepository grades) {
        super(profiles, grades);
        this.profileStore = profiles;
        this.gradeStore = grades;
    }

    public void addUser(long userId, String account, String displayName) {
        profileStore.addUser(userId, account, displayName);
    }

    public void addUser(long userId) {
        addUser(userId, "user" + userId, "用户" + userId);
    }

    public void addCourse(long courseId, String code, String name) {
        gradeStore.addCourse(courseId, code, name);
    }

    public void addCourse(long courseId, String code, String name, BigDecimal credits,
                          String semesterCode) {
        gradeStore.addCourse(courseId, code, name, credits, semesterCode);
    }

    public void addEnrollment(long id, long studentUserId, long courseId, String status) {
        gradeStore.addEnrollment(id, studentUserId, courseId, status);
    }

    public void addInstructor(long courseId, long teacherUserId) {
        gradeStore.addInstructor(courseId, teacherUserId);
    }

    public void addGrade(StudentGradeDto grade) {
        gradeStore.addGrade(grade);
    }

    /** 直接补一条成绩请求，便于测试查询和分页，不绕过业务服务。 */
    public void addGrade(long recorderUserId, StudentGradeRecordRequest request) {
        gradeStore.upsert(null, recorderUserId, request);
    }
}
