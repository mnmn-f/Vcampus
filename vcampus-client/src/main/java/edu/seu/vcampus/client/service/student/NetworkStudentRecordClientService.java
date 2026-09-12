package edu.seu.vcampus.client.service.student;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
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
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.StudentCommands;

import java.io.Serializable;

/** 学籍客户端服务；只组装消息和校验响应类型，业务规则在服务端执行。 */
public final class NetworkStudentRecordClientService implements StudentRecordClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkStudentRecordClientService(NetworkClientService network,
                                             ClientSession session) {
        if (network == null || session == null) {
            throw new IllegalArgumentException("student client dependencies are required");
        }
        this.network = network;
        this.session = session;
    }

    public NetworkStudentRecordClientService(ClientGateway gateway, ClientSession session) {
        this(new NetworkClientService(gateway), session);
    }

    @Override
    public StudentProfileDto getOwnProfile() throws NetworkClientException {
        return payload(request(StudentCommands.SELF_PROFILE, null),
                StudentProfileDto.class);
    }

    @Override
    public StudentGradePage getOwnGrades(StudentGradeQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.SELF_GRADES, query),
                StudentGradePage.class);
    }

    @Override
    public StudentGradeReportDto getOwnGradeReport(StudentGradeQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.SELF_GRADE_REPORT, query),
                StudentGradeReportDto.class);
    }

    @Override
    public StudentGradeExportDto exportOwnGrades(StudentGradeExportQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.SELF_GRADE_EXPORT, query),
                StudentGradeExportDto.class);
    }

    @Override
    public StudentProfilePage searchProfiles(StudentProfileQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_SEARCH, query),
                StudentProfilePage.class);
    }

    @Override
    public StudentAccountCandidatePage searchPendingAccounts(StudentAccountCandidateQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_CANDIDATES, query),
                StudentAccountCandidatePage.class);
    }

    @Override
    public StudentDetailDto getProfileDetail(long studentUserId)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_DETAIL,
                Long.valueOf(studentUserId)), StudentDetailDto.class);
    }

    @Override
    public StudentProfileDto createProfile(StudentProfileWriteRequest request)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_CREATE, request),
                StudentProfileDto.class);
    }

    @Override
    public StudentProfileDto createProfile(StudentProfileCreateRequest request)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_CREATE, request),
                StudentProfileDto.class);
    }

    @Override
    public StudentProfileDto updateProfile(StudentProfileWriteRequest request)
            throws NetworkClientException {
        return payload(request(StudentCommands.PROFILE_UPDATE, request),
                StudentProfileDto.class);
    }

    @Override
    public StudentGradeDto recordGrade(StudentGradeRecordRequest request)
            throws NetworkClientException {
        return payload(request(StudentCommands.GRADE_RECORD, request),
                StudentGradeDto.class);
    }

    @Override
    public StudentGradePage reviewGrades(StudentGradeReviewQuery query)
            throws NetworkClientException {
        return payload(request(StudentCommands.GRADE_REVIEW, query),
                StudentGradePage.class);
    }

    private Message request(String command, Serializable body)
            throws NetworkClientException {
        if (!session.isAuthenticated()) {
            throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        network.setSessionToken(session.getSessionToken());
        return network.request(command, body);
    }

    private <T> T payload(Message response,
                          Class<T> type) throws NetworkClientException {
        if (response == null || !response.isSuccess()) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "服务器响应无效");
        }
        Object value = response.getPayload();
        if (!type.isInstance(value)) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "学籍响应格式不正确");
        }
        return type.cast(value);
    }

    /** 在认证服务完成登录后调用；客户端不自行生成或修改身份字段。 */
    public void synchronizeSessionToken() {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
    }
}
