package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.server.library.repository.StudyRoomRepository;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 自习室仓储。预约记录由 {@link MySqlStudyRoomReservationRepository} 管理。 */
public final class MySqlStudyRoomRepository implements StudyRoomRepository {
    private static final String COLUMNS = "id,building_name,room_no,capacity,open_time,"
            + "close_time,status,description";

    @Override
    public PageResult<StudyRoomView> search(Connection connection,
                                            StudyRoomSearchRequest request)
            throws java.sql.SQLException {
        JdbcLibrarySupport.QueryParts parts = parts(request);
        return JdbcLibrarySupport.page(connection,
                "SELECT COUNT(*) FROM study_rooms WHERE 1=1" + parts.where,
                "SELECT " + COLUMNS + " FROM study_rooms WHERE 1=1" + parts.where
                        + " ORDER BY building_name,room_no LIMIT ? OFFSET ?",
                parts.params, request.getPage(), request.getPageSize(), new JdbcLibrarySupport.RowReader<StudyRoomView>() { public StudyRoomView read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.room(r); } });
    }

    @Override
    public StudyRoomView findByIdForUpdate(Connection connection, long roomId)
            throws java.sql.SQLException {
        try (PreparedStatement s = connection.prepareStatement("SELECT " + COLUMNS
                + " FROM study_rooms WHERE id=? FOR UPDATE")) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? JdbcLibrarySupport.room(r) : null;
            }
        }
    }

    @Override
    public StudyRoomView save(Connection connection, StudyRoomUpsertRequest request)
            throws java.sql.SQLException {
        long id = request.getId();
        if (id <= 0) {
            String sql = "INSERT INTO study_rooms (building_name,room_no,capacity,open_time,"
                    + "close_time,status,description) VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement s = connection.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
                bindRoom(s, request, false); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    if (!keys.next()) throw new java.sql.SQLException("room id not generated");
                    id = keys.getLong(1);
                }
            }
        } else {
            String sql = "UPDATE study_rooms SET building_name=?,room_no=?,capacity=?,"
                    + "open_time=?,close_time=?,status=?,description=? WHERE id=?";
            try (PreparedStatement s = connection.prepareStatement(sql)) {
                bindRoom(s, request, true); s.setLong(8, id);
                if (s.executeUpdate() == 0) throw new java.sql.SQLException("room not found");
            }
        }
        try (PreparedStatement s = connection.prepareStatement("SELECT " + COLUMNS
                + " FROM study_rooms WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new java.sql.SQLException("room not found after save");
                return JdbcLibrarySupport.room(r);
            }
        }
    }

    private static void bindRoom(PreparedStatement s, StudyRoomUpsertRequest r, boolean update)
            throws java.sql.SQLException {
        s.setString(1, r.getBuildingName()); s.setString(2, r.getRoomNo());
        s.setInt(3, r.getCapacity()); s.setTime(4, JdbcTemporal.time(r.getOpenTime()));
        s.setTime(5, JdbcTemporal.time(r.getCloseTime())); s.setString(6, r.getStatus());
        s.setString(7, r.getDescription());
    }

    private static JdbcLibrarySupport.QueryParts parts(StudyRoomSearchRequest request) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        String keyword = JdbcLibrarySupport.clean(request.getKeyword());
        if (keyword != null) {
            where.append(" AND (building_name LIKE ? OR room_no LIKE ?)");
            String value = "%" + keyword + "%"; params.add(value); params.add(value);
        }
        String status = JdbcLibrarySupport.clean(request.getStatus());
        if (status != null) { where.append(" AND status=?"); params.add(status); }
        if (request.getMinCapacity() != null) {
            where.append(" AND capacity>=?"); params.add(request.getMinCapacity());
        }
        return new JdbcLibrarySupport.QueryParts(where.toString(), params);
    }
}
