package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;

/** 测试和离线演示使用的组合仓储。 */
public final class InMemoryCampusRepository extends CampusRepositoryComposite {
    private final InMemoryCampusState state;

    public InMemoryCampusRepository() {
        this(new InMemoryCampusState());
    }

    private InMemoryCampusRepository(InMemoryCampusState state) {
        super(new InMemoryCampusAnnouncementRepository(state),
                new InMemoryCampusCompetitionRepository(state),
                new InMemoryCampusSrtpRepository(state),
                new InMemoryCampusClassroomRepository(state));
        this.state = state;
    }

    public synchronized void addAnnouncement(CampusAnnouncementDto value) {
        if (value == null || value.getId() <= 0) throw new IllegalArgumentException("announcement required");
        state.announcements.put(value.getId(), value);
        state.nextAnnouncement = Math.max(state.nextAnnouncement, value.getId() + 1L);
    }
    public synchronized void addCompetition(CompetitionDto value) {
        if (value == null || value.getId() <= 0) throw new IllegalArgumentException("competition required");
        state.competitions.put(value.getId(), value);
        state.nextCompetition = Math.max(state.nextCompetition, value.getId() + 1L);
    }
    public synchronized void addRegistration(CompetitionRegistrationDto value) {
        if (value == null || value.getCompetitionId() <= 0 || value.getStudentUserId() <= 0) {
            throw new IllegalArgumentException("registration required");
        }
        state.registrations.put(value.getCompetitionId() + ":" + value.getStudentUserId(), value);
    }
    public synchronized void addSrtp(SrtpRecordDto value) {
        if (value == null || value.getId() <= 0) throw new IllegalArgumentException("srtp required");
        state.srtp.put(value.getId(), value);
        state.nextSrtp = Math.max(state.nextSrtp, value.getId() + 1L);
    }
    public synchronized void addClassroom(CampusClassroomDto value) {
        if (value == null || value.getId() <= 0) throw new IllegalArgumentException("classroom required");
        state.classrooms.put(value.getId(), value);
    }
    public synchronized void addReservation(ClassroomReservationDto value) {
        if (value == null || value.getId() <= 0) throw new IllegalArgumentException("reservation required");
        state.reservations.put(value.getId(), value);
        state.nextReservation = Math.max(state.nextReservation, value.getId() + 1L);
    }
}
