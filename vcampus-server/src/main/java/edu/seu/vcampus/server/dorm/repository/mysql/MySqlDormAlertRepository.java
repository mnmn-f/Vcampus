package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;

import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** MySQL 未归/异常记录仓储。 */
final class MySqlDormAlertRepository {
    DormPage<LateReturnAlertDto> list(Connection c, Long student, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (student != null) { where.append(" AND student_user_id=?"); p.add(student); }
        String status = JdbcDormSupport.clean(query.getStatus());
        if (status != null) { where.append(" AND status=?"); p.add(status); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM late_return_alerts" + where.toString(),
                "SELECT id,student_user_id,alert_date,detected_at,status,handled_by,handled_at,note"
                        + " FROM late_return_alerts" + where.toString() + " ORDER BY alert_date DESC,id DESC LIMIT ? OFFSET ?",
                p, query.getPage(), query.getPageSize(), new JdbcDormSupport.Reader<LateReturnAlertDto>() { public LateReturnAlertDto read(ResultSet r) throws SQLException { return JdbcDormSupport.alert(r); } });
    }

    /**
     * 开一条待处理晚归。表上 (student_user_id, alert_date) 唯一，用 INSERT IGNORE 让「同一天
     * 刷了两次晚归卡」只留一条，受影响行数就是有没有新建。
     */
    boolean open(Connection c, long student, org.threeten.bp.LocalDate date, org.threeten.bp.LocalDateTime detectedAt)
            throws SQLException {
        String sql = "INSERT IGNORE INTO late_return_alerts(student_user_id,alert_date,detected_at,status)"
                + " VALUES(?,?,COALESCE(?,CURRENT_TIMESTAMP(3)),'OPEN')";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, student); s.setDate(2, JdbcTemporal.date(date));
            if (detectedAt == null) s.setNull(3, java.sql.Types.TIMESTAMP);
            else s.setTimestamp(3, JdbcTemporal.timestamp(detectedAt));
            return s.executeUpdate() == 1;
        }
    }

    LateReturnAlertDto lock(Connection c, long id) throws SQLException {
        String sql = "SELECT id,student_user_id,alert_date,detected_at,status,handled_by,handled_at,note"
                + " FROM late_return_alerts WHERE id=? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.alert(r) : null; }
        }
    }

    LateReturnAlertDto handle(Connection c, long id, long actor, LateReturnHandleRequest request)
            throws SQLException {
        String status = request.getStatus();
        if (!"CONFIRMED".equals(status) && !"CLEARED".equals(status) && !"IGNORED".equals(status)) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "未归处理状态不正确");
        }
        String sql = "UPDATE late_return_alerts SET status=?,handled_by=?,handled_at=CURRENT_TIMESTAMP(3),note=?"
                + " WHERE id=? AND status='OPEN'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status); s.setLong(2, actor); s.setString(3, request.getNote()); s.setLong(4, id);
            if (s.executeUpdate() != 1) {
                LateReturnAlertDto old = lock(c, id);
                throw new DormRepositoryException(old == null ? DormCommands.ALERT_NOT_FOUND
                        : DormCommands.ALERT_INVALID_STATE, old == null ? "未归记录不存在" : "未归记录已处理");
            }
        }
        return lock(c, id);
    }
}
