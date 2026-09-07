package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
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

import java.sql.Connection;
import java.sql.SQLException;

/** 组合专责仓储，避免内存和 MySQL 门面复制转发逻辑。 */
public abstract class CampusRepositoryComposite implements CampusRepository {
    private final CampusAnnouncementRepository announcements;
    private final CampusCompetitionRepository competitions;
    private final CampusSrtpRepository srtp;
    private final CampusClassroomRepository classrooms;

    protected CampusRepositoryComposite(CampusAnnouncementRepository announcements,
            CampusCompetitionRepository competitions, CampusSrtpRepository srtp,
            CampusClassroomRepository classrooms) {
        this.announcements = announcements;
        this.competitions = competitions;
        this.srtp = srtp;
        this.classrooms = classrooms;
    }

    @Override public CampusPage<CampusAnnouncementDto> list(Connection c, CampusPageQuery q,
            String module, String role, boolean drafts) throws SQLException {
        return announcements.list(c, q, module, role, drafts);
    }
    @Override public CampusAnnouncementDto findAnnouncement(Connection c, long id) throws SQLException {
        return announcements.findAnnouncement(c, id);
    }
    @Override public CampusAnnouncementDto lockAnnouncement(Connection c, long id) throws SQLException {
        return announcements.lockAnnouncement(c, id);
    }
    @Override public CampusAnnouncementDto save(Connection c, CampusAnnouncementSaveRequest r,
            long actor) throws SQLException { return announcements.save(c, r, actor); }

    @Override public CampusPage<CompetitionDto> list(Connection c, CampusPageQuery q,
            boolean drafts) throws SQLException { return competitions.list(c, q, drafts); }
    @Override public CompetitionDto findCompetition(Connection c, long id) throws SQLException {
        return competitions.findCompetition(c, id);
    }
    @Override public CompetitionDto lockCompetition(Connection c, long id) throws SQLException {
        return competitions.lockCompetition(c, id);
    }
    @Override public CompetitionDto save(Connection c, CompetitionSaveRequest r, long actor)
            throws SQLException { return competitions.save(c, r, actor); }
    @Override public CompetitionRegistrationDto findRegistration(Connection c, long comp,
            long student, boolean lock) throws SQLException {
        return competitions.findRegistration(c, comp, student, lock);
    }
    @Override public long countRegistered(Connection c, long comp) throws SQLException {
        return competitions.countRegistered(c, comp);
    }
    @Override public CompetitionRegistrationDto register(Connection c, long comp, long student)
            throws SQLException { return competitions.register(c, comp, student); }
    @Override public void cancelRegistration(Connection c, long comp, long student) throws SQLException {
        competitions.cancelRegistration(c, comp, student);
    }
    @Override public CampusPage<CompetitionRegistrationDto> roster(Connection c, long comp,
            CampusPageQuery q) throws SQLException { return competitions.roster(c, comp, q); }

    @Override public CampusPage<SrtpRecordDto> list(Connection c, CampusPageQuery q, Long student)
            throws SQLException { return srtp.list(c, q, student); }
    @Override public SrtpRecordDto findSrtp(Connection c, long id) throws SQLException {
        return srtp.findSrtp(c, id);
    }
    @Override public SrtpRecordDto lockSrtp(Connection c, long id) throws SQLException {
        return srtp.lockSrtp(c, id);
    }
    @Override public SrtpRecordDto save(Connection c, SrtpSaveRequest r, long student, long actor,
            boolean admin) throws SQLException { return srtp.save(c, r, student, actor, admin); }
    @Override public SrtpRecordDto review(Connection c, SrtpStatusRequest r, long reviewer)
            throws SQLException { return srtp.review(c, r, reviewer); }

    @Override public CampusPage<CampusClassroomDto> listClassrooms(Connection c, CampusPageQuery q)
            throws SQLException { return classrooms.listClassrooms(c, q); }
    @Override public CampusClassroomDto findClassroom(Connection c, long id) throws SQLException {
        return classrooms.findClassroom(c, id);
    }
    @Override public CampusClassroomDto lockClassroom(Connection c, long id) throws SQLException {
        return classrooms.lockClassroom(c, id);
    }
    @Override public CampusPage<ClassroomReservationDto> listReservations(Connection c, Long id,
            CampusPageQuery q) throws SQLException { return classrooms.listReservations(c, id, q); }
    @Override public ClassroomReservationDto findReservation(Connection c, long id)
            throws SQLException { return classrooms.findReservation(c, id); }
    @Override public ClassroomReservationDto lockReservation(Connection c, long id)
            throws SQLException { return classrooms.lockReservation(c, id); }
    @Override public ClassroomReservationDto createReservation(Connection c,
            ClassroomReservationRequest r, long applicant) throws SQLException {
        return classrooms.createReservation(c, r, applicant);
    }
    @Override public ClassroomReservationDto review(Connection c, ClassroomReviewRequest r,
            long reviewer) throws SQLException { return classrooms.review(c, r, reviewer); }
    @Override public ClassroomReservationDto cancelReservation(Connection c, long id, long applicant)
            throws SQLException { return classrooms.cancelReservation(c, id, applicant); }
    @Override public boolean hasConflict(Connection c, long room,
            org.threeten.bp.LocalDateTime start, org.threeten.bp.LocalDateTime end, long excluded)
            throws SQLException { return classrooms.hasConflict(c, room, start, end, excluded); }
    @Override public boolean hasApplicantConflict(Connection c, long applicant,
            org.threeten.bp.LocalDateTime start, org.threeten.bp.LocalDateTime end) throws SQLException {
        return classrooms.hasApplicantConflict(c, applicant, start, end);
    }
}
