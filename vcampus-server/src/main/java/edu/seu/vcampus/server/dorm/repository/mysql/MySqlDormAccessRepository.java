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
import org.threeten.bp.LocalTime;

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

    /**
     * 门禁时段。策略表是 V5 扩展加的，V1 基线库里没有；这里用独立连接状态之外的
     * 保守写法——查不到（表不存在或没有 id=1 那一行）就退回默认 23:00 / 05:00，
     * 不让「登记一次归宿」因为策略缺失而整个失败。
     */
    LocalTime[] policy(Connection c) {
        try (PreparedStatement s = c.prepareStatement("SELECT curfew_time, dawn_time FROM dorm_access_policies WHERE id = 1");
             ResultSet r = s.executeQuery()) {
            if (r.next()) {
                LocalTime curfew = JdbcTemporal.localTime(r.getTime("curfew_time"));
                LocalTime dawn = JdbcTemporal.localTime(r.getTime("dawn_time"));
                if (curfew != null && dawn != null) return new LocalTime[]{curfew, dawn};
            }
        } catch (SQLException ignored) {
            // 表不存在等情况按默认策略处理
        }
        return new LocalTime[]{LocalTime.of(23, 0), LocalTime.of(5, 0)};
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
