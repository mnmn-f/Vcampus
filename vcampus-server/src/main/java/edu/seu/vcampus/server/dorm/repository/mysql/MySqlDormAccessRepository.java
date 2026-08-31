package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 门禁进出记录仓储。 */
final class MySqlDormAccessRepository {
    AccessRecordDto add(Connection c, long student, AccessRecordDto request) throws SQLException {
        String type = request.getRecordType();
        if (!"ENTRY".equals(type) && !"EXIT".equals(type)) {
            throw new DormRepositoryException("DORM.INVALID_INPUT", "进出类型不正确");
        }
        String sql = "INSERT INTO access_records(student_user_id,record_type,occurred_at,door_name,source,note)"
                + " VALUES(?,?,COALESCE(?,CURRENT_TIMESTAMP(3)),?,?,?)";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, student); s.setString(2, type);
            if (request.getOccurredAt() == null) s.setNull(3, java.sql.Types.TIMESTAMP);
            else s.setTimestamp(3, JdbcTemporal.timestamp(request.getOccurredAt()));
            s.setString(4, request.getDoorName()); s.setString(5, request.getSource() == null ? "MANUAL" : request.getSource());
            s.setString(6, request.getNote()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("access record id was not generated");
                id = keys.getLong(1);
            }
        }
        return find(c, id);
    }

    DormPage<AccessRecordDto> list(Connection c, Long student, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (student != null) { where += " AND student_user_id=?"; p.add(student); }
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND record_type=?"; p.add(query.getStatus().trim()); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM access_records" + where,
                "SELECT id,student_user_id,record_type,occurred_at,door_name,source,note FROM access_records"
                        + where + " ORDER BY occurred_at DESC,id DESC LIMIT ? OFFSET ?", p, query.getPage(), query.getPageSize(),
                new JdbcDormSupport.Reader<AccessRecordDto>() { public AccessRecordDto read(ResultSet r) throws SQLException { return JdbcDormSupport.access(r); } });
    }

    private AccessRecordDto find(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id,student_user_id,record_type,occurred_at,door_name,source,note FROM access_records WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.access(r) : null; }
        }
    }
}
