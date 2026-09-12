package edu.seu.vcampus.client.service.student;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.student.StudentDetailDto;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportQuery;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;

/** 学籍页面使用的网络服务边界，不依赖 Swing 组件。 */
public interface StudentRecordClientService {
    StudentProfileDto getOwnProfile() throws NetworkClientException;
    StudentGradePage getOwnGrades(StudentGradeQuery query) throws NetworkClientException;
    StudentGradeReportDto getOwnGradeReport(StudentGradeQuery query)
            throws NetworkClientException;
    StudentGradeExportDto exportOwnGrades(StudentGradeExportQuery query)
            throws NetworkClientException;
    StudentProfilePage searchProfiles(StudentProfileQuery query) throws NetworkClientException;
    default StudentAccountCandidatePage searchPendingAccounts(StudentAccountCandidateQuery query)
            throws NetworkClientException {
        return new StudentAccountCandidatePage(java.util.Collections.emptyList(), 0L, 1, 100);
    }
    StudentDetailDto getProfileDetail(long studentUserId) throws NetworkClientException;
    StudentProfileDto createProfile(StudentProfileWriteRequest request)
            throws NetworkClientException;
    default StudentProfileDto createProfile(StudentProfileCreateRequest request)
            throws NetworkClientException {
        throw new NetworkClientException("COMMON.NOT_SUPPORTED", "待建档接口不可用");
    }
    StudentProfileDto updateProfile(StudentProfileWriteRequest request)
            throws NetworkClientException;
    StudentGradeDto recordGrade(StudentGradeRecordRequest request)
            throws NetworkClientException;
    StudentGradePage reviewGrades(StudentGradeReviewQuery query)
            throws NetworkClientException;
}
