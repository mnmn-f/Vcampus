package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.server.library.repository.StudyRoomReservationRepository;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** MySQL 自习室预约仓储；冲突查询在业务事务内执行。 */
public final class MySqlStudyRoomReservationRepository
        implements StudyRoomReservationRepository {
    private static final String BASE = "SELECT rr.id,rr.room_id,CONCAT(sr.building_name,' ',"
            + "sr.room_no) AS room_name,rr.user_id,rr.start_at,rr.end_at,rr.status,"
            + "rr.cancelled_at FROM study_room_reservations rr JOIN study_rooms sr "
            + "ON sr.id=rr.room_id";

    @Override
    public PageResult<StudyRoomReservationView> search(Connection connection,
                                                        StudyRoomReservationSearchRequest request,
                                                        Long userId)
            throws java.sql.SQLException {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        if (userId != null) { where.append(" AND rr.user_id=?"); params.add(userId); }
        String status = JdbcLibrarySupport.clean(request.getStatus());
        if (status != null) { where.append(" AND rr.status=?"); params.add(status); }
        return JdbcLibrarySupport.page(connection,
                "SELECT COUNT(*) FROM study_room_reservations rr WHERE 1=1" + where,
                BASE + " WHERE 1=1" + where
                        + " ORDER BY rr.start_at DESC,rr.id DESC LIMIT ? OFFSET ?",
                params, request.getPage(), request.getPageSize(),
                new JdbcLibrarySupport.RowReader<StudyRoomReservationView>() { public StudyRoomReservationView read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.reservation(r); } });
    }

    @Override
    public StudyRoomReservationView findById(Connection c, long id)
            throws java.sql.SQLException { return find(c, id, false); }

    @Override
    public StudyRoomReservationView findByIdForUpdate(Connection c, long id)
            throws java.sql.SQLException { return find(c, id, true); }

    private StudyRoomReservationView find(Connection c, long id, boolean lock)
            throws java.sql.SQLException {
        try (PreparedStatement s = c.prepareStatement(BASE + " WHERE rr.id=?"
                + (lock ? " FOR UPDATE" : ""))) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? JdbcLibrarySupport.reservation(r) : null;
            }
        }
    }

    @Override
    public boolean hasRoomOverlap(Connection c, long roomId, LocalDateTime startAt,
                                  LocalDateTime endAt) throws java.sql.SQLException {
        return exists(c, "rr.room_id=?", roomId, startAt, endAt);
    }

    @Override
    public boolean hasUserOverlap(Connection c, long userId, LocalDateTime startAt,
                                  LocalDateTime endAt) throws java.sql.SQLException {
        lockUser(c, userId);
        return exists(c, "rr.user_id=?", userId, startAt, endAt);
    }

    private static void lockUser(Connection c, long userId) throws java.sql.SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM users WHERE id=? FOR UPDATE")) {
            s.setLong(1, userId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new java.sql.SQLException("user not found");
            }
        }
    }

    private boolean exists(Connection c, String ownerClause, long owner,
                           LocalDateTime startAt, LocalDateTime endAt)
            throws java.sql.SQLException {
        String sql = "SELECT rr.id FROM study_room_reservations rr WHERE " + ownerClause
                + " AND rr.status='RESERVED' AND rr.start_at<? AND rr.end_at>?"
                + " LIMIT 1 FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, owner); s.setTimestamp(2, JdbcTemporal.timestamp(endAt));
            s.setTimestamp(3, JdbcTemporal.timestamp(startAt));
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    @Override
    public StudyRoomReservationView insert(Connection c, long roomId, long userId,
                                            LocalDateTime startAt, LocalDateTime endAt)
            throws java.sql.SQLException {
        String sql = "INSERT INTO study_room_reservations (room_id,user_id,start_at,end_at,"
                + "status) VALUES (?,?,?,?,'RESERVED')";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, roomId); s.setLong(2, userId);
            s.setTimestamp(3, JdbcTemporal.timestamp(startAt));
            s.setTimestamp(4, JdbcTemporal.timestamp(endAt)); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new java.sql.SQLException("reservation id not generated");
                id = keys.getLong(1);
            }
        }
        return findById(c, id);
    }

    @Override
    public boolean cancel(Connection c, long reservationId, LocalDateTime cancelledAt)
            throws java.sql.SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE study_room_reservations SET status='CANCELLED',cancelled_at=?"
                        + " WHERE id=? AND status='RESERVED'")) {
            s.setTimestamp(1, JdbcTemporal.timestamp(cancelledAt)); s.setLong(2, reservationId);
            return s.executeUpdate() == 1;
        }
    }

}
