package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;

import java.sql.Connection;
import java.util.List;

/** 学籍聚合的持久化边界；所有方法使用调用方提供的事务连接。 */
public interface StudentRecordRepository {
    StudentProfileDto findProfile(Connection connection, long userId);

    StudentProfilePage searchProfiles(Connection connection, StudentProfileQuery query);

    StudentGradePage findGrades(Connection connection, long studentUserId,
                                StudentGradeQuery query);

    List<StudentGradeDto> findAllGrades(Connection connection, long studentUserId);

    List<StudentGradeDto> findAllGrades(Connection connection, long studentUserId,
                                        String semesterCode, int limit);

    List<StudentGradeDto> findAllGrades(Connection connection, long studentUserId,
                                        String semesterCode, Long courseId, int limit);

    StudentGradePage reviewGrades(Connection connection, StudentGradeReviewQuery query);

    StudentGradeDto findGradeByEnrollment(Connection connection, long enrollmentId);

    EnrollmentRecord findEnrollment(Connection connection, long enrollmentId);

    boolean userExists(Connection connection, long userId);

    boolean profileExists(Connection connection, long userId);

    boolean studentNoExists(Connection connection, String studentNo,
                            long excludedUserId);

    boolean teacherOwnsCourse(Connection connection, long teacherUserId, long courseId);

    void insertProfile(Connection connection, StudentProfileWriteRequest request);

    void updateProfile(Connection connection, StudentProfileWriteRequest request);

    void upsertGrade(Connection connection, long recorderUserId,
                     StudentGradeRecordRequest request);
}
