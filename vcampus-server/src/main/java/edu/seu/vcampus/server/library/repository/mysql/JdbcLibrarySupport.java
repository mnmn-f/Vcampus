package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Time;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** MySQL 行映射和分页的无状态辅助方法。 */
final class JdbcLibrarySupport {
    private JdbcLibrarySupport() {
    }

    static int offset(int page, int pageSize) {
        long value = ((long) page - 1L) * pageSize;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    static void bind(PreparedStatement statement, java.util.List<Object> params)
            throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    static <T> PageResult<T> page(Connection connection, String countSql, String dataSql,
                                  List<Object> params, int page, int pageSize,
                                  RowReader<T> reader) throws SQLException {
        long total;
        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            bind(statement, params);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                total = result.getLong(1);
            }
        }
        List<T> rows = new ArrayList<T>();
        try (PreparedStatement statement = connection.prepareStatement(dataSql)) {
            bind(statement, params);
            statement.setInt(params.size() + 1, pageSize);
            statement.setInt(params.size() + 2, offset(page, pageSize));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) rows.add(reader.read(result));
            }
        }
        return new PageResult<T>(rows, page, pageSize, total);
    }

    static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    static final class QueryParts {
        final String where;
        final java.util.List<Object> params;

        QueryParts(String where, java.util.List<Object> params) {
            this.where = where;
            this.params = params;
        }
    }

    interface RowReader<T> {
        T read(ResultSet result) throws SQLException;
    }

    static LocalDateTime timestamp(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return JdbcTemporal.localDateTime(value);
    }

    static LocalTime time(ResultSet result, String column) throws SQLException {
        Time value = result.getTime(column);
        return JdbcTemporal.localTime(value);
    }

    static BookDetail book(ResultSet r) throws SQLException {
        return new BookDetail(r.getLong("id"), r.getString("isbn"),
                r.getString("title"), r.getString("author"),
                r.getString("publisher"), r.getString("category"),
                r.getInt("total_copies"), r.getInt("available_copies"),
                r.getString("location"), r.getString("description"),
                r.getString("status"), timestamp(r, "created_at"),
                timestamp(r, "updated_at"), (Integer) r.getObject("publication_year"), r.getBytes("cover_image"));
    }

    static BorrowRecordView borrow(ResultSet r) throws SQLException {
        return new BorrowRecordView(r.getLong("id"), r.getLong("book_id"),
                r.getString("book_title"), r.getLong("borrower_user_id"),
                r.getString("borrower_name"), timestamp(r, "issued_at"),
                timestamp(r, "due_at"), timestamp(r, "returned_at"),
                r.getString("effective_status"), r.getInt("renew_count"),
                r.getString("remark"));
    }

    static StudyRoomView room(ResultSet r) throws SQLException {
        return new StudyRoomView(r.getLong("id"), r.getString("building_name"),
                r.getString("room_no"), r.getInt("capacity"),
                time(r, "open_time"), time(r, "close_time"),
                r.getString("status"), r.getString("description"));
    }

    static StudyRoomReservationView reservation(ResultSet r) throws SQLException {
        return new StudyRoomReservationView(r.getLong("id"), r.getLong("room_id"),
                r.getString("room_name"), r.getLong("user_id"),
                timestamp(r, "start_at"), timestamp(r, "end_at"),
                r.getString("status"), timestamp(r, "cancelled_at"));
    }

    static OnlineResourceView resource(ResultSet r) throws SQLException {
        return new OnlineResourceView(r.getLong("id"), r.getString("title"),
                r.getString("resource_type"), r.getString("url"),
                r.getString("description"), r.getLong("publisher_id"),
                r.getString("status"), timestamp(r, "published_at"));
    }
}
