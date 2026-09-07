package edu.seu.vcampus.server.campus.handler;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomQuery;
import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionRosterRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.campus.service.CampusException;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** 教务扩展命令适配器；请求身份字段全部由服务层会话覆盖。 */
public final class CampusCommandHandler implements CommandHandler {
    private final String command;
    private final CampusService service;

    public CampusCommandHandler(String command, CampusService service) {
        if (command == null || service == null) throw new IllegalArgumentException("campus dependencies required");
        this.command = command;
        this.service = service;
    }

    @Override public Message handle(Message request, SessionContext session) {
        try {
            Object p = request == null ? null : request.getPayload();
            if (CampusCommands.ANNOUNCEMENT_LIST.equals(command)) return Message.success(request,
                    service.announcementQuery(session, p == null ? null : announcementQuery(p)));
            if (CampusCommands.ANNOUNCEMENT_SAVE.equals(command)) return Message.success(request,
                    service.saveAnnouncement(session, payload(p, CampusAnnouncementSaveRequest.class)));
            if (CampusCommands.ANNOUNCEMENT_REVOKE.equals(command)) return Message.success(request,
                    service.revokeAnnouncement(session, id(p)));
            if (CampusCommands.COMPETITION_LIST.equals(command)) return Message.success(request,
                    service.competitionQuery(session, p == null ? null : competitionQuery(p)));
            if (CampusCommands.COMPETITION_SAVE.equals(command)) return Message.success(request,
                    service.saveCompetition(session, payload(p, CompetitionSaveRequest.class)));
            if (CampusCommands.COMPETITION_REGISTER.equals(command)) return Message.success(request,
                    service.registerCompetition(session, competitionId(p)));
            if (CampusCommands.COMPETITION_CANCEL.equals(command)) {
                service.cancelCompetition(session, competitionId(p)); return Message.success(request, null);
            }
            if (CampusCommands.COMPETITION_ROSTER.equals(command)) {
                CompetitionRosterRequest value = payload(p, CompetitionRosterRequest.class);
                return Message.success(request, service.competitionRoster(session, value.getCompetitionId(), value.getPage()));
            }
            if (CampusCommands.SRTP_MINE.equals(command)) return Message.success(request,
                    service.mySrtp(session, pageQuery(p)));
            if (CampusCommands.SRTP_LIST.equals(command)) return Message.success(request,
                    service.listSrtp(session, pageQuery(p)));
            if (CampusCommands.SRTP_SAVE.equals(command)) return Message.success(request,
                    service.saveSrtp(session, payload(p, SrtpSaveRequest.class)));
            if (CampusCommands.SRTP_REVIEW.equals(command)) return Message.success(request,
                    service.reviewSrtp(session, payload(p, SrtpStatusRequest.class)));
            if (CampusCommands.CLASSROOM_LIST.equals(command)) return Message.success(request,
                    service.classrooms(session, pageQuery(p)));
            if (CampusCommands.CLASSROOM_REQUEST_LIST.equals(command)) return Message.success(request,
                    service.classroomReservations(session, pageQuery(p)));
            if (CampusCommands.CLASSROOM_APPLY.equals(command)) return Message.success(request,
                    service.applyClassroom(session, payload(p, ClassroomReservationRequest.class)));
            if (CampusCommands.CLASSROOM_MINE.equals(command)) return Message.success(request,
                    service.myClassroomReservations(session, pageQuery(p)));
            if (CampusCommands.CLASSROOM_REVIEW.equals(command)) return Message.success(request,
                    service.reviewClassroom(session, payload(p, ClassroomReviewRequest.class)));
            if (CampusCommands.CLASSROOM_CANCEL.equals(command)) return Message.success(request,
                    service.cancelClassroom(session, id(p)));
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的教务扩展操作");
        } catch (CampusException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, CampusCommands.INVALID_INPUT, "请求参数格式不正确");
        }
    }

    @Override public Permission requiredPermission() {
        if (CampusCommands.ANNOUNCEMENT_SAVE.equals(command) || CampusCommands.ANNOUNCEMENT_REVOKE.equals(command)) return Permission.ANNOUNCEMENT_MANAGE;
        if (CampusCommands.COMPETITION_SAVE.equals(command) || CampusCommands.COMPETITION_ROSTER.equals(command)) return Permission.COMPETITION_MANAGE;
        if (CampusCommands.COMPETITION_REGISTER.equals(command) || CampusCommands.COMPETITION_CANCEL.equals(command)) return Permission.COMPETITION_ENROLL;
        if (CampusCommands.SRTP_MINE.equals(command)) return Permission.SRTP_SELF_READ;
        if (CampusCommands.SRTP_SAVE.equals(command)) return null;
        if (CampusCommands.SRTP_REVIEW.equals(command) || CampusCommands.SRTP_LIST.equals(command)) return Permission.SRTP_MANAGE;
        if (CampusCommands.CLASSROOM_APPLY.equals(command) || CampusCommands.CLASSROOM_MINE.equals(command) || CampusCommands.CLASSROOM_CANCEL.equals(command)) return Permission.CLASSROOM_RESERVE;
        if (CampusCommands.CLASSROOM_REVIEW.equals(command)) return Permission.CLASSROOM_APPROVE;
        if (CampusCommands.CLASSROOM_REQUEST_LIST.equals(command)) return Permission.CLASSROOM_APPROVE;
        return null;
    }

    @Override public boolean requiresAuthentication() { return true; }

    private static CampusAnnouncementQuery announcementQuery(Object p) {
        if (p instanceof CampusAnnouncementQuery) return (CampusAnnouncementQuery) p;
        if (p instanceof CampusPageQuery) return new CampusAnnouncementQuery((CampusPageQuery) p, null);
        throw new IllegalArgumentException("announcement query required");
    }
    private static CampusCompetitionQuery competitionQuery(Object p) {
        if (p instanceof CampusCompetitionQuery) return (CampusCompetitionQuery) p;
        if (p instanceof CampusPageQuery) return new CampusCompetitionQuery((CampusPageQuery) p);
        throw new IllegalArgumentException("competition query required");
    }
    private static CampusPageQuery pageQuery(Object p) {
        if (p == null) return null;
        if (p instanceof CampusPageQuery) return (CampusPageQuery) p;
        if (p instanceof CampusAnnouncementQuery) return ((CampusAnnouncementQuery) p).getPage();
        if (p instanceof CampusCompetitionQuery) return ((CampusCompetitionQuery) p).getPage();
        if (p instanceof CampusClassroomQuery) return ((CampusClassroomQuery) p).getPage();
        throw new IllegalArgumentException("page query required");
    }
    private static long competitionId(Object p) {
        if (p instanceof CompetitionRegistrationRequest) return ((CompetitionRegistrationRequest) p).getCompetitionId();
        return id(p);
    }
    private static long id(Object p) {
        if (!(p instanceof CampusIdRequest)) throw new IllegalArgumentException("id request required");
        return ((CampusIdRequest) p).getId();
    }
    private static <T> T payload(Object p, Class<T> type) {
        if (!type.isInstance(p)) throw new IllegalArgumentException(type.getSimpleName() + " required");
        return type.cast(p);
    }
}
