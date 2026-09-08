package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormLeaveRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** leave_requests 表 DAO；提交前锁定学生行，避免同一学生的并发重叠申请。 */
public final class MySqlDormLeaveRepository implements DormLeaveRepository {
    private static final String COLUMNS = "id,student_user_id,leave_type,start_at,end_at,reason,status,"
            + "reviewed_by,reviewed_at,review_remark,created_at";

    @Override public void lockStudent(Connection c, long student) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM users WHERE id=? FOR UPDATE")) {
            s.setLong(1, student);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "学生不存在");
            }
        }
    }

    @Override public boolean hasOverlap(Connection c, long student, LocalDateTime start,
                                        LocalDateTime end, Long excluded) throws SQLException {
        String sql = "SELECT 1 FROM leave_requests WHERE student_user_id=?"
                + " AND status IN ('PENDING','APPROVED') AND start_at < ? AND end_at > ?"
                + (excluded == null ? "" : " AND id<>?");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i = 1; s.setLong(i++, student); s.setTimestamp(i++, JdbcTemporal.timestamp(end));
            s.setTimestamp(i++, JdbcTemporal.timestamp(start));
            if (excluded != null) s.setLong(i, excluded.longValue());
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    @Override public LeaveRequestDto submit(Connection c, long student, LeaveSubmitRequest request)
            throws SQLException {
        String sql = "INSERT INTO leave_requests(student_user_id,leave_type,start_at,end_at,reason,status)"
                + " VALUES(?,?,?,?,?,'PENDING')";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, student); s.setString(2, request.getLeaveType());
            s.setTimestamp(3, JdbcTemporal.timestamp(request.getStartAt()));
            s.setTimestamp(4, JdbcTemporal.timestamp(request.getEndAt()));
            s.setString(5, request.getReason()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("leave id was not generated");
                id = keys.getLong(1);
            }
        }
        LeaveRequestDto value = lock(c, id);
        if (value == null) throw new SQLException("leave row disappeared");
        return value;
    }

    @Override public DormPage<LeaveRequestDto> list(Connection c, Long student, LeaveQuery query)
            throws SQLException {
        LeaveQuery q = query == null ? new LeaveQuery() : query;
        List<Object> values = new ArrayList<Object>();
        String where = where(student, q, values);
        String from = " FROM leave_requests" + where;
        return page(c, "SELECT COUNT(*)" + from,
                "SELECT " + COLUMNS + from + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                values, q);
    }

    @Override public LeaveRequestDto lock(Connection c, long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM leave_requests WHERE id=? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? read(r) : null; }
        }
    }

    @Override public LeaveRequestDto cancel(Connection c, long id) throws SQLException {
        LeaveRequestDto old = lock(c, id);
        if (old == null) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "请假申请不存在");
        if (!"PENDING".equals(old.getStatus())) throw invalidState();
        update(c, "UPDATE leave_requests SET status='CANCELLED' WHERE id=? AND status='PENDING'", id);
        return lock(c, id);
    }

    @Override public LeaveRequestDto review(Connection c, long id, long reviewer, boolean approved,
                                             String remark) throws SQLException {
        LeaveRequestDto old = lock(c, id);
        if (old == null) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "请假申请不存在");
        if (!"PENDING".equals(old.getStatus())) throw invalidState();
        String sql = "UPDATE leave_requests SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),"
                + "review_remark=? WHERE id=? AND status='PENDING'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, approved ? "APPROVED" : "REJECTED"); s.setLong(2, reviewer);
            s.setString(3, remark); s.setLong(4, id); s.executeUpdate();
        }
        return lock(c, id);
    }

    private static String where(Long student, LeaveQuery q, List<Object> values) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (student != null) { w.append(" AND student_user_id=?"); values.add(student); }
        else if (q.getStudentUserId() != null) { w.append(" AND student_user_id=?"); values.add(q.getStudentUserId()); }
        if (q.getStatus() != null) { w.append(" AND status=?"); values.add(q.getStatus()); }
        if (q.getStartDate() != null) { w.append(" AND end_at > ?"); values.add(ts(q.getStartDate().atStartOfDay())); }
        if (q.getEndDate() != null) {
            w.append(" AND start_at < ?"); values.add(ts(q.getEndDate().plusDays(1L).atStartOfDay()));
        }
        return w.toString();
    }

    private static DormPage<LeaveRequestDto> page(Connection c, String countSql, String dataSql,
                                                   List<Object> values, LeaveQuery q) throws SQLException {
        long total;
        try (PreparedStatement s = c.prepareStatement(countSql)) {
            bind(s, values); try (ResultSet r = s.executeQuery()) { r.next(); total = r.getLong(1); }
        }
        List<LeaveRequestDto> rows = new ArrayList<LeaveRequestDto>();
        try (PreparedStatement s = c.prepareStatement(dataSql)) {
            int i = bind(s, values); s.setInt(i++, q.getPageSize()); s.setInt(i, (q.getPage() - 1) * q.getPageSize());
            try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(read(r)); }
        }
        return new DormPage<LeaveRequestDto>(q.getPage(), q.getPageSize(), total, rows);
    }

    private static int bind(PreparedStatement s, List<Object> values) throws SQLException {
        int i = 1; for (Object value : values) s.setObject(i++, value); return i;
    }

    private static LeaveRequestDto read(ResultSet r) throws SQLException {
        long reviewer = r.getLong("reviewed_by");
        boolean reviewerNull = r.wasNull();
        return new LeaveRequestDto(r.getLong("id"), r.getLong("student_user_id"), r.getString("leave_type"),
                time(r.getTimestamp("start_at")), time(r.getTimestamp("end_at")), r.getString("reason"),
                r.getString("status"), reviewerNull ? null : Long.valueOf(reviewer),
                time(r.getTimestamp("reviewed_at")), r.getString("review_remark"), time(r.getTimestamp("created_at")));
    }

    private static void update(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); if (s.executeUpdate() != 1) throw invalidState(); }
    }
    private static Timestamp ts(LocalDateTime value) { return JdbcTemporal.timestamp(value); }
    private static LocalDateTime time(Timestamp value) { return JdbcTemporal.localDateTime(value); }
    private static DormRepositoryException invalidState() {
        return new DormRepositoryException(DormCommands.LEAVE_INVALID_STATE, "请假申请当前状态不可操作");
    }
}
