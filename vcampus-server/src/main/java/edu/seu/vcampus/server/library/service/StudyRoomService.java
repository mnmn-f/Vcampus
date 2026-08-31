package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.StudyRoomRepository;
import edu.seu.vcampus.server.library.repository.StudyRoomReservationRepository;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/** 自习室检索、预约和图书管理员维护服务。 */
public final class StudyRoomService extends LibraryServiceSupport {
    private final StudyRoomRepository rooms;
    private final StudyRoomReservationRepository reservations;

    public StudyRoomService(StudyRoomRepository rooms,
                            StudyRoomReservationRepository reservations,
                            TransactionManager transactions) {
        super(transactions);
        if (rooms == null || reservations == null) throw new IllegalArgumentException("repositories required");
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public PageResult<StudyRoomView> search(final SessionContext session, final StudyRoomSearchRequest request) {
        require(session, Permission.LIBRARY_READ);
        final StudyRoomSearchRequest query = request == null ? new StudyRoomSearchRequest() : request;
        page(query.getPage(), query.getPageSize());
        return execute(new Work<PageResult<StudyRoomView>>() { public PageResult<StudyRoomView> run(java.sql.Connection c) throws Exception { return rooms.search(c, query); } });
    }

    public StudyRoomView save(final SessionContext session, final StudyRoomUpsertRequest request) {
        require(session, Permission.STUDY_ROOM_MANAGE);
        validateRoom(request);
        return execute(new Work<StudyRoomView>() {
            @Override public StudyRoomView run(java.sql.Connection c) throws Exception {
                if (request.getId() > 0 && rooms.findByIdForUpdate(c, request.getId()) == null) throw new LibraryServiceException(ResultCodes.NOT_FOUND, "自习室不存在");
                return rooms.save(c, request);
            }
        });
    }

    public StudyRoomReservationView reserve(final SessionContext session,
                                            final StudyRoomReservationRequest request) {
        require(session, Permission.STUDY_ROOM_RESERVE);
        validateReservation(request);
        return execute(new Work<StudyRoomReservationView>() { public StudyRoomReservationView run(java.sql.Connection c) throws Exception { return reserveInTransaction(c, request, session.getUserId()); } });
    }

    private StudyRoomReservationView reserveInTransaction(java.sql.Connection c,
                                                           StudyRoomReservationRequest request,
                                                           long userId)
            throws java.sql.SQLException {
        StudyRoomView found = rooms.findByIdForUpdate(c, request.getRoomId());
        if (found == null) throw new LibraryServiceException(ResultCodes.NOT_FOUND, "自习室不存在");
        StudyRoomView room = found;
        if (!"OPEN".equals(room.getStatus())) {
            throw new LibraryServiceException(LibraryCommands.ROOM_CLOSED, "自习室当前未开放");
        }
        LocalTime start = request.getStartAt().toLocalTime();
        LocalTime end = request.getEndAt().toLocalTime();
        if (start.isBefore(room.getOpenTime()) || end.isAfter(room.getCloseTime())) {
            throw new LibraryServiceException(LibraryCommands.INVALID_TIME, "预约时间不在开放时段内");
        }
        if (reservations.hasUserOverlap(c, userId, request.getStartAt(), request.getEndAt())) {
            throw new LibraryServiceException(LibraryCommands.RESERVATION_DUPLICATE,
                    "你在该时段已有自习室预约");
        }
        if (reservations.hasRoomOverlap(c, request.getRoomId(), request.getStartAt(), request.getEndAt())) {
            throw new LibraryServiceException(LibraryCommands.RESERVATION_CONFLICT,
                    "该自习室时段已被预约");
        }
        return reservations.insert(c, request.getRoomId(), userId,
                request.getStartAt(), request.getEndAt());
    }

    public StudyRoomReservationView cancel(final SessionContext session, final long reservationId) {
        if (session == null) throw new LibraryServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(Permission.STUDY_ROOM_RESERVE)
                && !session.allows(Permission.STUDY_ROOM_MANAGE)) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "当前职责无权取消预约");
        }
        id(reservationId, "预约编号不正确");
        return execute(new Work<StudyRoomReservationView>() { public StudyRoomReservationView run(java.sql.Connection c) throws Exception { return cancelInTransaction(c, reservationId, session); } });
    }

    private StudyRoomReservationView cancelInTransaction(java.sql.Connection c, long id,
                                                         SessionContext session)
            throws java.sql.SQLException {
        StudyRoomReservationView current = reservations.findByIdForUpdate(c, id);
        if (current == null) throw new LibraryServiceException(ResultCodes.NOT_FOUND, "预约记录不存在");
        boolean manager = session.allows(Permission.STUDY_ROOM_MANAGE);
        if (!manager && current.getUserId() != session.getUserId()) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "不能取消他人的预约");
        }
        if (!"RESERVED".equals(current.getStatus())) {
            throw new LibraryServiceException(LibraryCommands.RESERVATION_CONFLICT, "该预约已不能取消");
        }
        LocalDateTime cancelledAt = LocalDateTime.now();
        if (!reservations.cancel(c, id, cancelledAt)) {
            throw new LibraryServiceException(LibraryCommands.RESERVATION_CONFLICT, "预约状态已发生变化");
        }
        StudyRoomReservationView old = current;
        return new StudyRoomReservationView(old.getId(), old.getRoomId(), old.getRoomName(),
                old.getUserId(), old.getStartAt(), old.getEndAt(), "CANCELLED", cancelledAt);
    }

    public PageResult<StudyRoomReservationView> listReservations(
            final SessionContext session, final StudyRoomReservationSearchRequest request) {
        if (session == null) throw new LibraryServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        final StudyRoomReservationSearchRequest query = request == null
                ? new StudyRoomReservationSearchRequest() : request;
        page(query.getPage(), query.getPageSize());
        Long owner;
        if (session.allows(Permission.STUDY_ROOM_MANAGE)) owner = null;
        else { require(session, Permission.STUDY_ROOM_RESERVE); owner = session.getUserId(); }
        final Long user = owner;
        return execute(new Work<PageResult<StudyRoomReservationView>>() { public PageResult<StudyRoomReservationView> run(java.sql.Connection c) throws Exception { return reservations.search(c, query, user); } });
    }

    private static void validateRoom(StudyRoomUpsertRequest r) {
        if (r == null) throw invalid("自习室请求不能为空");
        text(r.getBuildingName(), "楼栋"); text(r.getRoomNo(), "房间号");
        if (r.getCapacity() <= 0 || r.getOpenTime() == null || r.getCloseTime() == null
                || !r.getCloseTime().isAfter(r.getOpenTime())) throw invalid("自习室参数不正确");
        if (!"OPEN".equals(r.getStatus()) && !"MAINTENANCE".equals(r.getStatus())
                && !"CLOSED".equals(r.getStatus())) throw invalid("自习室状态不正确");
    }

    private static void validateReservation(StudyRoomReservationRequest r) {
        if (r == null) throw invalid("预约请求不能为空");
        id(r.getRoomId(), "自习室编号不正确");
        if (r.getStartAt() == null || r.getEndAt() == null
                || !r.getEndAt().isAfter(r.getStartAt())
                || !r.getStartAt().toLocalDate().equals(r.getEndAt().toLocalDate())
                || r.getStartAt().isBefore(LocalDateTime.now())) {
            throw new LibraryServiceException(LibraryCommands.INVALID_TIME, "预约时间不正确");
        }
    }

    private static LibraryServiceException invalid(String message) {
        return new LibraryServiceException(ResultCodes.INVALID_INPUT, message);
    }
}
