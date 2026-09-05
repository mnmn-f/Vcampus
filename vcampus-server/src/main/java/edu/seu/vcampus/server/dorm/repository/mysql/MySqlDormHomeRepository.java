package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.ext.DormHomeSummaryDto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 学生首屏摘要的 MySQL 查询。
 *
 * <p>拆成四条小查询而不是一条大 JOIN：住宿、床位、账单、卫生分别有自己的过滤条件
 * （在住、已占用、未缴、最近一次），揉进一条语句要靠多层子查询和外连接，读起来
 * 和改起来都很痛，而这四条各自都走现成索引，代价可以忽略。</p>
 */
final class MySqlDormHomeRepository {

    DormHomeSummaryDto summary(Connection c, long studentId) throws SQLException {
        Residence residence = residence(c, studentId);
        if (residence == null) return DormHomeSummaryDto.empty();
        int occupied = occupiedBeds(c, residence.roomId);
        BigDecimal unpaid = BigDecimal.ZERO;
        int unpaidCount = 0;
        String sql = "SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total FROM utility_allocations"
                + " WHERE student_user_id = ? AND status = 'UNPAID'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, studentId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) { unpaidCount = r.getInt("cnt"); unpaid = r.getBigDecimal("total"); }
            }
        }
        int pending = 0;
        try (PreparedStatement s = c.prepareStatement(
                "SELECT COUNT(*) FROM accommodation_requests WHERE student_user_id = ? AND status = 'PENDING'")) {
            s.setLong(1, studentId);
            try (ResultSet r = s.executeQuery()) { if (r.next()) pending = r.getInt(1); }
        }
        Hygiene hygiene = latestHygiene(c, residence.roomId);
        return new DormHomeSummaryDto(true, residence.roomId, residence.buildingName, residence.roomNo,
                residence.bedNo, residence.capacity, occupied,
                unpaid == null ? BigDecimal.ZERO : unpaid, unpaidCount,
                hygiene == null ? null : hygiene.score,
                hygiene == null ? null : hygiene.result,
                hygiene == null ? null : hygiene.status,
                hygiene == null ? null : hygiene.issue,
                hygiene == null ? null : hygiene.inspectedAt,
                pending);
    }

    private Residence residence(Connection c, long studentId) throws SQLException {
        String sql = "SELECT r.id AS room_id, r.room_no, r.capacity, b.building_name, bd.bed_no"
                + " FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " JOIN dorm_rooms r ON r.id = bd.room_id"
                + " JOIN dorm_buildings b ON b.id = r.building_id"
                + " WHERE ar.student_user_id = ? AND ar.status = 'ACTIVE'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, studentId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                Residence value = new Residence();
                value.roomId = r.getLong("room_id");
                value.roomNo = r.getString("room_no");
                value.capacity = r.getInt("capacity");
                value.buildingName = r.getString("building_name");
                value.bedNo = r.getString("bed_no");
                return value;
            }
        }
    }

    private int occupiedBeds(Connection c, long roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE bd.room_id = ? AND ar.status = 'ACTIVE'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        }
    }

    private Hygiene latestHygiene(Connection c, long roomId) throws SQLException {
        String sql = "SELECT score, result, status, issue_description, inspected_at FROM hygiene_inspections"
                + " WHERE room_id = ? ORDER BY inspected_at DESC, id DESC LIMIT 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                Hygiene value = new Hygiene();
                value.score = r.getBigDecimal("score");
                value.result = r.getString("result");
                value.status = r.getString("status");
                value.issue = r.getString("issue_description");
                value.inspectedAt = JdbcDormSupport.localTimestamp(r, "inspected_at");
                return value;
            }
        }
    }

    private static final class Residence {
        private long roomId;
        private String roomNo;
        private String buildingName;
        private String bedNo;
        private int capacity;
    }

    private static final class Hygiene {
        private BigDecimal score;
        private String result;
        private String status;
        private String issue;
        private org.threeten.bp.LocalDateTime inspectedAt;
    }
}
