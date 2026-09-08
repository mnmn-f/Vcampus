package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormFacilityRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.threeten.bp.LocalDate;

/** 住宿变更的短 SQL 操作，供主仓储复用。 */
final class MySqlDormAccommodationOps {
    private MySqlDormAccommodationOps() { }

    static void requireStudent(Connection c, long student) throws SQLException {
        String sql = "SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id=u.id"
                + " JOIN roles r ON r.id=ur.role_id WHERE u.id=? AND u.status='ACTIVE' AND r.code='STUDENT'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, student);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new DormRepositoryException(DormCommands.INVALID_INPUT, "学生账号不存在或已停用");
            }
        }
    }

    static void requireAvailable(Connection c, DormFacilityRepository facilities, long bed)
            throws SQLException {
        DormBedDto value = facilities.findBed(c, bed, true);
        if (value == null) throw new DormRepositoryException(DormCommands.BED_NOT_FOUND, "床位不存在");
        if (!"AVAILABLE".equals(value.getStatus())) {
            throw new DormRepositoryException(DormCommands.BED_OCCUPIED, "床位已被占用或不可用");
        }
    }

    static void insertRecord(Connection c, long student, long bed, LocalDate date, long actor)
            throws SQLException {
        String sql = "INSERT INTO accommodation_records(student_user_id,bed_id,start_date,status,created_by)"
                + " VALUES(?,?,?,'ACTIVE',?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, student); s.setLong(2, bed); s.setDate(3, JdbcTemporal.date(date)); s.setLong(4, actor);
            s.executeUpdate();
        }
    }

    static void occupy(Connection c, long bed) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_beds SET status='OCCUPIED'"
                + " WHERE id=? AND status='AVAILABLE'")) {
            s.setLong(1, bed);
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.BED_OCCUPIED, "床位已被占用或不可用");
        }
    }

    static void release(Connection c, long bed) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_beds SET status='AVAILABLE'"
                + " WHERE id=? AND status='OCCUPIED'")) {
            s.setLong(1, bed);
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.BED_UNAVAILABLE, "原床位状态不一致");
        }
    }

    static void endRecord(Connection c, long record, LocalDate date) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE accommodation_records"
                + " SET status='ENDED',end_date=? WHERE id=? AND status='ACTIVE'")) {
            s.setDate(1, JdbcTemporal.date(date)); s.setLong(2, record);
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
    }
}
