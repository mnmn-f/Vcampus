package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.server.campus.repository.CampusRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

/** 教务扩展模块统一服务门面；各业务规则位于同包专责服务。 */
public final class CampusService {
    private final CampusAnnouncementService announcements;
    private final CampusCompetitionService competitions;
    private final CampusSrtpService srtp;
    private final CampusClassroomService classrooms;

    public CampusService(CampusRepository repository, TransactionManager transactions) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        announcements = new CampusAnnouncementService(repository, transactions);
        competitions = new CampusCompetitionService(repository, transactions);
        srtp = new CampusSrtpService(repository, transactions);
        classrooms = new CampusClassroomService(repository, transactions);
    }
    public CampusService(CampusRepository repository) { this(repository, null); }

    public CampusPage<CampusAnnouncementDto> announcements(SessionContext s, CampusPageQuery q) { return announcements.list(s, new CampusAnnouncementQuery(q, null)); }
    public CampusPage<CampusAnnouncementDto> announcementQuery(SessionContext s, CampusAnnouncementQuery q) { return announcements.list(s, q); }
    public CampusPage<CampusAnnouncementDto> listAnnouncements(SessionContext s, CampusPageQuery q) { return announcements(s, q); }
    public CampusPage<CampusAnnouncementDto> queryAnnouncements(SessionContext s, CampusPageQuery q) { return announcements(s, q); }
    public CampusAnnouncementDto saveAnnouncement(SessionContext s, CampusAnnouncementSaveRequest r) { return announcements.save(s, r); }
    public CampusAnnouncementDto revokeAnnouncement(SessionContext s, long id) { return announcements.revoke(s, id); }

    public CampusPage<CompetitionDto> competitions(SessionContext s, CampusPageQuery q) { return competitions.list(s, new CampusCompetitionQuery(q)); }
    public CampusPage<CompetitionDto> competitionQuery(SessionContext s, CampusCompetitionQuery q) { return competitions.list(s, q); }
    public CampusPage<CompetitionDto> listCompetitions(SessionContext s, CampusPageQuery q) { return competitions(s, q); }
    public CompetitionDto saveCompetition(SessionContext s, CompetitionSaveRequest r) { return competitions.save(s, r); }
    public CompetitionRegistrationDto registerCompetition(SessionContext s, long id) { return competitions.register(s, id); }
    public CompetitionRegistrationDto register(SessionContext s, long id) { return registerCompetition(s, id); }
    public void cancelCompetition(SessionContext s, long id) { competitions.cancel(s, id); }
    public void cancelRegistration(SessionContext s, long id) { cancelCompetition(s, id); }
    public CampusPage<CompetitionRegistrationDto> competitionRoster(SessionContext s, long id, CampusPageQuery q) { return competitions.roster(s, id, q); }

    public CampusPage<SrtpRecordDto> mySrtp(SessionContext s, CampusPageQuery q) { return srtp.mine(s, q); }
    public CampusPage<SrtpRecordDto> studentSrtp(SessionContext s, CampusPageQuery q) { return mySrtp(s, q); }
    public CampusPage<SrtpRecordDto> listSrtp(SessionContext s, CampusPageQuery q) { return srtp.list(s, q); }
    public SrtpRecordDto saveSrtp(SessionContext s, SrtpSaveRequest r) { return srtp.save(s, r); }
    public SrtpRecordDto reviewSrtp(SessionContext s, SrtpStatusRequest r) { return srtp.review(s, r); }

    public CampusPage<CampusClassroomDto> classrooms(SessionContext s, CampusPageQuery q) { return classrooms.classrooms(s, q); }
    public CampusPage<CampusClassroomDto> listClassrooms(SessionContext s, CampusPageQuery q) { return classrooms(s, q); }
    public ClassroomReservationDto applyClassroom(SessionContext s, ClassroomReservationRequest r) { return classrooms.apply(s, r); }
    public ClassroomReservationDto applyReservation(SessionContext s, ClassroomReservationRequest r) { return applyClassroom(s, r); }
    public CampusPage<ClassroomReservationDto> myClassroomReservations(SessionContext s, CampusPageQuery q) { return classrooms.mine(s, q); }
    public CampusPage<ClassroomReservationDto> classroomReservations(SessionContext s, CampusPageQuery q) { return classrooms.all(s, q); }
    public ClassroomReservationDto reviewClassroom(SessionContext s, ClassroomReviewRequest r) { return classrooms.review(s, r); }
    public ClassroomReservationDto approveReservation(SessionContext s, ClassroomReviewRequest r) { return reviewClassroom(s, r); }
    public ClassroomReservationDto cancelClassroom(SessionContext s, long id) { return classrooms.cancel(s, id); }
    public ClassroomReservationDto cancelReservation(SessionContext s, long id) { return cancelClassroom(s, id); }
}
