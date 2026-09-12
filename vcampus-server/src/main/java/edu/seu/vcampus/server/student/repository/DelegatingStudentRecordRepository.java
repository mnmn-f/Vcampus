package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;

import java.sql.Connection;
import java.util.List;

/** 组合档案 DAO 和成绩 DAO 的统一转发层，避免每种存储实现复制接口样板。 */
public abstract class DelegatingStudentRecordRepository implements StudentRecordRepository {
    public interface ProfileStore {
        StudentProfileDto find(Connection c, long id);
        StudentProfilePage search(Connection c, StudentProfileQuery q);
        StudentAccountCandidatePage pendingAccounts(Connection c, StudentAccountCandidateQuery q);
        long pendingAccountId(Connection c, String account);
        boolean userExists(Connection c, long id);
        boolean profileExists(Connection c, long id);
        boolean studentNoExists(Connection c, String no, long excluded);
        void insert(Connection c, StudentProfileWriteRequest request);
        void update(Connection c, StudentProfileWriteRequest request);
    }

    public interface GradeStore {
        StudentGradePage find(Connection c, long id, StudentGradeQuery q);
        List<StudentGradeDto> findAll(Connection c, long id);
        List<StudentGradeDto> findAll(Connection c, long id, String semesterCode, int limit);
        List<StudentGradeDto> findAll(Connection c, long id, String semesterCode,
                                      Long courseId, int limit);
        StudentGradePage review(Connection c, StudentGradeReviewQuery q);
        StudentGradeDto findByEnrollment(Connection c, long id);
        EnrollmentRecord findEnrollment(Connection c, long id);
        boolean teacherOwnsCourse(Connection c, long teacher, long course);
        void upsert(Connection c, long recorder, StudentGradeRecordRequest request);
    }

    private final ProfileStore profiles;
    private final GradeStore grades;

    protected DelegatingStudentRecordRepository(ProfileStore profiles, GradeStore grades) {
        if (profiles == null || grades == null) {
            throw new IllegalArgumentException("student stores are required");
        }
        this.profiles = profiles;
        this.grades = grades;
    }

    @Override
    public final StudentProfileDto findProfile(Connection c, long id) {
        return profiles.find(c, id);
    }

    @Override
    public final StudentProfilePage searchProfiles(Connection c, StudentProfileQuery q) {
        return profiles.search(c, q);
    }

    @Override
    public final StudentAccountCandidatePage searchPendingAccounts(
            Connection c, StudentAccountCandidateQuery q) {
        return profiles.pendingAccounts(c, q);
    }

    @Override
    public final long findPendingAccountId(Connection c, String account) {
        return profiles.pendingAccountId(c, account);
    }

    @Override
    public final StudentGradePage findGrades(Connection c, long id, StudentGradeQuery q) {
        return grades.find(c, id, q);
    }

    @Override
    public final List<StudentGradeDto> findAllGrades(Connection c, long id) {
        return grades.findAll(c, id);
    }

    @Override
    public final List<StudentGradeDto> findAllGrades(Connection c, long id,
                                                       String semesterCode, int limit) {
        return grades.findAll(c, id, semesterCode, limit);
    }

    @Override
    public final List<StudentGradeDto> findAllGrades(Connection c, long id,
                                                       String semesterCode, Long courseId,
                                                       int limit) {
        return grades.findAll(c, id, semesterCode, courseId, limit);
    }

    @Override
    public final StudentGradePage reviewGrades(Connection c, StudentGradeReviewQuery q) {
        return grades.review(c, q);
    }

    @Override
    public final StudentGradeDto findGradeByEnrollment(Connection c, long id) {
        return grades.findByEnrollment(c, id);
    }

    @Override
    public final EnrollmentRecord findEnrollment(Connection c, long id) {
        return grades.findEnrollment(c, id);
    }

    @Override
    public final boolean userExists(Connection c, long id) {
        return profiles.userExists(c, id);
    }

    @Override
    public final boolean profileExists(Connection c, long id) {
        return profiles.profileExists(c, id);
    }

    @Override
    public final boolean studentNoExists(Connection c, String no, long excluded) {
        return profiles.studentNoExists(c, no, excluded);
    }

    @Override
    public final boolean teacherOwnsCourse(Connection c, long teacher, long course) {
        return grades.teacherOwnsCourse(c, teacher, course);
    }

    @Override
    public final void insertProfile(Connection c, StudentProfileWriteRequest request) {
        profiles.insert(c, request);
    }

    @Override
    public final void updateProfile(Connection c, StudentProfileWriteRequest request) {
        profiles.update(c, request);
    }

    @Override
    public final void upsertGrade(Connection c, long recorder,
                                  StudentGradeRecordRequest request) {
        grades.upsert(c, recorder, request);
    }
}
