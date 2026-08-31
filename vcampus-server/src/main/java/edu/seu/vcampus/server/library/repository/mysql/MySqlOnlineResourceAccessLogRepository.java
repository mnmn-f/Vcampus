package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.server.library.repository.OnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** MySQL 线上资源访问日志 DAO；查询只返回非敏感摘要。 */
public final class MySqlOnlineResourceAccessLogRepository
        implements OnlineResourceAccessLogRepository {
    @Override
    public void append(Connection connection, OnlineResourceAccessLogDto log) throws SQLException {
        String sql = "INSERT INTO online_resource_access_logs "
                + "(resource_id,user_id,accessed_at,client_ip) VALUES (?,?,CURRENT_TIMESTAMP(3),NULL)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, log.getResourceId());
            statement.setLong(2, log.getUserId());
            statement.executeUpdate();
        }
    }

    @Override
    public OnlineResourceAccessLogPage search(Connection connection,
                                              OnlineResourceAccessLogQuery query) throws SQLException {
        OnlineResourceAccessLogQuery q = query == null ? new OnlineResourceAccessLogQuery() : query;
        Filters filters = filters(q);
        String from = " FROM online_resource_access_logs l"
                + " JOIN online_resources r ON r.id=l.resource_id"
                + " JOIN users u ON u.id=l.user_id";
        String sql = "SELECT l.id,l.resource_id,l.user_id,r.title,u.username,u.display_name,l.accessed_at"
                + from + " WHERE 1=1" + filters.where
                + " ORDER BY l.accessed_at DESC,l.id DESC LIMIT ? OFFSET ?";
        List<OnlineResourceAccessLogDto> items = new ArrayList<OnlineResourceAccessLogDto>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bind(statement, filters.params, 1);
            statement.setInt(index++, q.getPageSize()); statement.setInt(index, q.getOffset());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(read(result));
            }
            return new OnlineResourceAccessLogPage(items, q.getPage(), q.getPageSize(),
                    count(connection, from, filters));
        }
    }

    private static long count(Connection connection, String from, Filters filters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*)" + from + " WHERE 1=1" + filters.where)) {
            bind(statement, filters.params, 1);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); }
        }
    }

    private static Filters filters(OnlineResourceAccessLogQuery q) {
        StringBuilder where = new StringBuilder(); List<Object> params = new ArrayList<Object>();
        if (q.getResourceId() != null) { where.append(" AND l.resource_id=?"); params.add(q.getResourceId()); }
        if (q.getUserId() != null) { where.append(" AND l.user_id=?"); params.add(q.getUserId()); }
        if (q.getFrom() != null) { where.append(" AND l.accessed_at>=?"); params.add(JdbcTemporal.timestamp(q.getFrom())); }
        if (q.getTo() != null) { where.append(" AND l.accessed_at<=?"); params.add(JdbcTemporal.timestamp(q.getTo())); }
        return new Filters(where.toString(), params);
    }

    private static int bind(PreparedStatement statement, List<Object> params, int index)
            throws SQLException {
        for (Object value : params) {
            if (value instanceof Long) statement.setLong(index++, ((Long) value).longValue());
            else statement.setTimestamp(index++, (Timestamp) value);
        }
        return index;
    }

    private static OnlineResourceAccessLogDto read(ResultSet result) throws SQLException {
        Timestamp timestamp = result.getTimestamp(7);
        LocalDateTime accessedAt = JdbcTemporal.localDateTime(timestamp);
        return new OnlineResourceAccessLogDto(result.getLong(1), result.getLong(2), result.getLong(3),
                result.getString(4), result.getString(5), result.getString(6), accessedAt);
    }

    private static final class Filters {
        private final String where;
        private final List<Object> params;
        private Filters(String where, List<Object> params) { this.where = where; this.params = params; }
    }
}
