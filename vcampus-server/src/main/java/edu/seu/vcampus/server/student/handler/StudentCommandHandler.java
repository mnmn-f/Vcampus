package edu.seu.vcampus.server.student.handler;

import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.service.StudentRecordException;
import edu.seu.vcampus.server.student.service.StudentRecordService;

/** 将学籍命令适配到统一服务门面；身份始终来自路由器解析的会话。 */
public final class StudentCommandHandler implements CommandHandler {
    private final String command;
    private final Permission permission;
    private final StudentRecordService service;

    public StudentCommandHandler(String command, Permission permission,
                                 StudentRecordService service) {
        if (command == null || command.trim().isEmpty() || permission == null
                || service == null) {
            throw new IllegalArgumentException("student handler dependencies are required");
        }
        this.command = command;
        this.permission = permission;
        this.service = service;
    }

    @Override
    public Permission requiredPermission() { return permission; }

    @Override
    public boolean requiresAuthentication() { return true; }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            if (is(StudentCommands.SELF_PROFILE)) return Message.success(request,
                    service.getOwnProfile(session));
            if (is(StudentCommands.SELF_GRADES)) return Message.success(request,
                    service.getOwnGrades(session, gradeQuery(request)));
            if (is(StudentCommands.PROFILE_SEARCH)) return Message.success(request,
                    service.searchProfiles(session, profileQuery(request)));
            if (is(StudentCommands.PROFILE_DETAIL)) return Message.success(request,
                    service.getProfileDetail(session, targetId(request)));
            if (is(StudentCommands.PROFILE_CREATE)) return Message.success(request,
                    service.createProfile(session, writeRequest(request)));
            if (is(StudentCommands.PROFILE_UPDATE)) return Message.success(request,
                    service.updateProfile(session, writeRequest(request)));
            if (is(StudentCommands.GRADE_RECORD)) return Message.success(request,
                    service.recordGrade(session, gradeRecord(request)));
            if (is(StudentCommands.GRADE_REVIEW)) return Message.success(request,
                    service.reviewGrades(session, reviewQuery(request)));
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的学籍操作");
        } catch (StudentRecordException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "请求参数格式不正确");
        }
    }

    private boolean is(String expected) { return expected.equals(command); }

    private static StudentProfileQuery profileQuery(Message request) {
        if (request.getPayload() == null) return StudentProfileQuery.firstPage();
        if (!(request.getPayload() instanceof StudentProfileQuery)) throw bad();
        return (StudentProfileQuery) request.getPayload();
    }

    private static StudentGradeQuery gradeQuery(Message request) {
        if (request.getPayload() == null) return StudentGradeQuery.firstPage();
        if (!(request.getPayload() instanceof StudentGradeQuery)) throw bad();
        return (StudentGradeQuery) request.getPayload();
    }

    private static StudentGradeReviewQuery reviewQuery(Message request) {
        if (request.getPayload() == null) return StudentGradeReviewQuery.firstPage();
        if (!(request.getPayload() instanceof StudentGradeReviewQuery)) throw bad();
        return (StudentGradeReviewQuery) request.getPayload();
    }

    private static StudentProfileWriteRequest writeRequest(Message request) {
        if (!(request.getPayload() instanceof StudentProfileWriteRequest)) throw bad();
        return (StudentProfileWriteRequest) request.getPayload();
    }

    private static StudentGradeRecordRequest gradeRecord(Message request) {
        if (!(request.getPayload() instanceof StudentGradeRecordRequest)) throw bad();
        return (StudentGradeRecordRequest) request.getPayload();
    }

    private static long targetId(Message request) {
        Object payload = request.getPayload();
        if (!(payload instanceof Number)) throw bad();
        return ((Number) payload).longValue();
    }

    private static IllegalArgumentException bad() {
        return new IllegalArgumentException("invalid student payload");
    }
}
