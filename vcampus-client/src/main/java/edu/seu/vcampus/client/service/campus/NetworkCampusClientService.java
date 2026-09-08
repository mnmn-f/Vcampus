package edu.seu.vcampus.client.service.campus;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionRosterRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.CampusCommands;

import java.io.Serializable;

/** 真实 Socket 客户端；当前会话令牌由 ClientSession 注入每次请求。 */
public final class NetworkCampusClientService implements CampusClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkCampusClientService(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) throw new IllegalArgumentException("campus client dependencies required");
        this.network = network;
        this.session = session;
    }
    public NetworkCampusClientService(ClientGateway gateway, ClientSession session) {
        this(new NetworkClientService(gateway), session);
    }

    @Override public CampusPage<CampusAnnouncementDto> announcements(CampusAnnouncementQuery q) throws NetworkClientException { return page(call(CampusCommands.ANNOUNCEMENT_LIST, q)); }
    @Override public CampusAnnouncementDto saveAnnouncement(CampusAnnouncementSaveRequest r) throws NetworkClientException { return value(call(CampusCommands.ANNOUNCEMENT_SAVE, r), CampusAnnouncementDto.class); }
    @Override public CampusAnnouncementDto revokeAnnouncement(long id) throws NetworkClientException { return value(call(CampusCommands.ANNOUNCEMENT_REVOKE, new CampusIdRequest(id)), CampusAnnouncementDto.class); }
    @Override public CampusPage<CompetitionDto> competitions(CampusCompetitionQuery q) throws NetworkClientException { return page(call(CampusCommands.COMPETITION_LIST, q)); }
    @Override public CompetitionDto saveCompetition(CompetitionSaveRequest r) throws NetworkClientException { return value(call(CampusCommands.COMPETITION_SAVE, r), CompetitionDto.class); }
    @Override public CompetitionRegistrationDto registerCompetition(long id) throws NetworkClientException { return value(call(CampusCommands.COMPETITION_REGISTER, new CompetitionRegistrationRequest(id)), CompetitionRegistrationDto.class); }
    @Override public void cancelCompetition(long id) throws NetworkClientException { call(CampusCommands.COMPETITION_CANCEL, new CompetitionRegistrationRequest(id)); }
    @Override public CampusPage<CompetitionRegistrationDto> competitionRoster(long id, CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.COMPETITION_ROSTER, new CompetitionRosterRequest(id, q))); }
    @Override public CampusPage<CompetitionRegistrationDto> myCompetitionRegistrations(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.COMPETITION_MINE, q)); }
    @Override public CampusPage<SrtpRecordDto> mySrtp(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.SRTP_MINE, q)); }
    @Override public CampusPage<SrtpRecordDto> listSrtp(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.SRTP_LIST, q)); }
    @Override public SrtpRecordDto saveSrtp(SrtpSaveRequest r) throws NetworkClientException { return value(call(CampusCommands.SRTP_SAVE, r), SrtpRecordDto.class); }
    @Override public SrtpRecordDto reviewSrtp(SrtpStatusRequest r) throws NetworkClientException { return value(call(CampusCommands.SRTP_REVIEW, r), SrtpRecordDto.class); }
    @Override public CampusPage<CampusClassroomDto> classrooms(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.CLASSROOM_LIST, q)); }
    @Override public ClassroomReservationDto applyClassroom(ClassroomReservationRequest r) throws NetworkClientException { return value(call(CampusCommands.CLASSROOM_APPLY, r), ClassroomReservationDto.class); }
    @Override public CampusPage<ClassroomReservationDto> myClassroomReservations(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.CLASSROOM_MINE, q)); }
    @Override public CampusPage<ClassroomReservationDto> classroomReservations(CampusPageQuery q) throws NetworkClientException { return page(call(CampusCommands.CLASSROOM_REQUEST_LIST, q)); }
    @Override public ClassroomReservationDto reviewClassroom(ClassroomReviewRequest r) throws NetworkClientException { return value(call(CampusCommands.CLASSROOM_REVIEW, r), ClassroomReservationDto.class); }
    @Override public ClassroomReservationDto cancelClassroom(long id) throws NetworkClientException { return value(call(CampusCommands.CLASSROOM_CANCEL, new CampusIdRequest(id)), ClassroomReservationDto.class); }

    private Message call(String command, Serializable payload) throws NetworkClientException {
        if (!session.isAuthenticated()) throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
        network.setSessionToken(session.getSessionToken());
        return network.request(command, payload);
    }

    @SuppressWarnings("unchecked")
    private static <T> CampusPage<T> page(Message response) throws NetworkClientException {
        if (response == null || !(response.getPayload() instanceof CampusPage)) throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "教务扩展分页响应格式不正确");
        return (CampusPage<T>) response.getPayload();
    }

    private static <T> T value(Message response, Class<T> type) throws NetworkClientException {
        if (response == null || !type.isInstance(response.getPayload())) throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "教务扩展响应格式不正确");
        return type.cast(response.getPayload());
    }
}
