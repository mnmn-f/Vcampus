package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 教室和预约内存专责仓储。 */
final class InMemoryCampusClassroomRepository implements CampusClassroomRepository {
    private final InMemoryCampusState state;
    InMemoryCampusClassroomRepository(InMemoryCampusState state) { this.state = state; }

    @Override public synchronized CampusPage<CampusClassroomDto> listClassrooms(Connection c,
            CampusPageQuery q) {
        List<CampusClassroomDto> rows = new ArrayList<CampusClassroomDto>();
        for (CampusClassroomDto value : state.classrooms.values()) {
            if (!InMemoryCampusSupport.matches(q, value.getStatus(), value.getBuildingName(),
                    value.getRoomNo(), value.getClassroomType())) continue;
            rows.add(value);
        }
        return InMemoryCampusSupport.page(rows, q);
    }

    @Override public synchronized CampusClassroomDto findClassroom(Connection c, long id) {
        return state.classrooms.get(Long.valueOf(id));
    }

    @Override public synchronized CampusClassroomDto lockClassroom(Connection c, long id) {
        return findClassroom(c, id);
    }

    @Override public synchronized CampusPage<ClassroomReservationDto> listReservations(Connection c,
            Long applicantId, CampusPageQuery q) {
        List<ClassroomReservationDto> rows = new ArrayList<ClassroomReservationDto>();
        for (ClassroomReservationDto value : state.reservations.values()) {
            if (applicantId != null && value.getApplicantId() != applicantId.longValue()) continue;
            if (!InMemoryCampusSupport.matches(q, value.getStatus(), value.getPurpose(),
                    value.getBuildingName(), value.getRoomNo())) continue;
            rows.add(value);
        }
        return InMemoryCampusSupport.page(rows, q);
    }

    @Override public synchronized ClassroomReservationDto findReservation(Connection c, long id) {
        return state.reservations.get(Long.valueOf(id));
    }

    @Override public synchronized ClassroomReservationDto lockReservation(Connection c, long id) {
        return findReservation(c, id);
    }

    @Override public synchronized ClassroomReservationDto createReservation(Connection c,
            ClassroomReservationRequest r, long applicant) {
        CampusClassroomDto room = findClassroom(c, r.getClassroomId());
        if (room == null) throw new CampusRepositoryException("CAMPUS.CLASSROOM_NOT_FOUND", "教室不存在");
        long id = state.nextReservation++;
        ClassroomReservationDto value = new ClassroomReservationDto(id, room.getId(),
                room.getBuildingName(), room.getRoomNo(), applicant, r.getPurpose(), r.getStartAt(),
                r.getEndAt(), "PENDING", null, null, null);
        state.reservations.put(Long.valueOf(id), value);
        return value;
    }

    @Override public synchronized ClassroomReservationDto review(Connection c,
            ClassroomReviewRequest r, long reviewer) {
        ClassroomReservationDto old = findReservation(c, r.getReservationId());
        if (old == null) throw new CampusRepositoryException("CAMPUS.CLASSROOM_NOT_FOUND", "预约不存在");
        ClassroomReservationDto value = new ClassroomReservationDto(old.getId(), old.getClassroomId(),
                old.getBuildingName(), old.getRoomNo(), old.getApplicantId(), old.getPurpose(),
                old.getStartAt(), old.getEndAt(), r.getStatus(), reviewer, LocalDateTime.now(),
                r.getRemark());
        state.reservations.put(Long.valueOf(old.getId()), value);
        return value;
    }

    @Override public synchronized ClassroomReservationDto cancelReservation(Connection c, long id,
            long applicant) {
        ClassroomReservationDto old = findReservation(c, id);
        if (old == null) throw new CampusRepositoryException("CAMPUS.CLASSROOM_NOT_FOUND", "预约不存在");
        if (old.getApplicantId() != applicant) {
            throw new CampusRepositoryException("CAMPUS.CLASSROOM_FORBIDDEN", "只能操作本人的预约");
        }
        ClassroomReservationDto value = new ClassroomReservationDto(old.getId(), old.getClassroomId(),
                old.getBuildingName(), old.getRoomNo(), old.getApplicantId(), old.getPurpose(),
                old.getStartAt(), old.getEndAt(), "CANCELLED", old.getReviewedBy(),
                old.getReviewedAt(), old.getReviewRemark());
        state.reservations.put(Long.valueOf(id), value);
        return value;
    }

    @Override public synchronized boolean hasConflict(Connection c, long classroomId,
            LocalDateTime start, LocalDateTime end, long excluded) {
        for (ClassroomReservationDto value : state.reservations.values()) {
            if (value.getId() != excluded && value.getClassroomId() == classroomId
                    && "APPROVED".equals(value.getStatus())
                    && InMemoryCampusSupport.overlaps(start, end, value.getStartAt(), value.getEndAt())) return true;
        }
        return false;
    }

    @Override public synchronized boolean hasApplicantConflict(Connection c, long applicantId,
            LocalDateTime start, LocalDateTime end) {
        for (ClassroomReservationDto value : state.reservations.values()) {
            if (value.getApplicantId() == applicantId
                    && ("PENDING".equals(value.getStatus()) || "APPROVED".equals(value.getStatus()))
                    && InMemoryCampusSupport.overlaps(start, end, value.getStartAt(), value.getEndAt())) return true;
        }
        return false;
    }
}
