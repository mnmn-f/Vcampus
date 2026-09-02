package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentDetailDto;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.StudentRecordRepository;

/** 学籍与成绩模块对命令处理器暴露的统一服务门面。 */
public final class StudentRecordService {
    private final StudentProfileService profiles;
    private final StudentGradeService grades;

    public StudentRecordService(StudentRecordRepository repository,
                                TransactionManager transactionManager) {
        this(repository, new TransactionManagerRunner(transactionManager));
    }

    public StudentRecordService(StudentRecordRepository repository,
                                StudentTransactionRunner transactions) {
        profiles = new StudentProfileService(repository, transactions);
        grades = new StudentGradeService(repository, transactions);
    }

    public StudentProfileDto getOwnProfile(SessionContext session)
            throws StudentRecordException {
        return profiles.getOwnProfile(session);
    }

    public StudentGradePage getOwnGrades(SessionContext session, StudentGradeQuery query)
            throws StudentRecordException {
        return grades.getOwnGrades(session, query);
    }

    public StudentProfilePage searchProfiles(SessionContext session, StudentProfileQuery query)
            throws StudentRecordException {
        return profiles.search(session, query);
    }

    public StudentDetailDto getProfileDetail(SessionContext session, long studentUserId)
            throws StudentRecordException {
        return profiles.detail(session, studentUserId);
    }

    public StudentProfileDto createProfile(SessionContext session,
                                           StudentProfileWriteRequest request)
            throws StudentRecordException {
        return profiles.create(session, request);
    }

    public StudentProfileDto updateProfile(SessionContext session,
                                           StudentProfileWriteRequest request)
            throws StudentRecordException {
        return profiles.update(session, request);
    }

    public StudentGradeDto recordGrade(SessionContext session,
                                       StudentGradeRecordRequest request)
            throws StudentRecordException {
        return grades.record(session, request);
    }

    public StudentGradePage reviewGrades(SessionContext session,
                                         StudentGradeReviewQuery query)
            throws StudentRecordException {
        return grades.review(session, query);
    }
}
