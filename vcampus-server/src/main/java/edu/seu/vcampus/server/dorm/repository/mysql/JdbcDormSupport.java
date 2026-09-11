package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** 宿舍 MySQL 分页、绑定和行映射辅助类。 */
final class JdbcDormSupport {
    private JdbcDormSupport() { }

    static int offset(int page, int size) {
        long value = ((long) page - 1L) * size;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    static void bind(PreparedStatement statement, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
    }

    static <T> DormPage<T> page(Connection connection, String countSql, String dataSql,
                                List<Object> params, int page, int size, Reader<T> reader)
            throws SQLException {
        long total;
        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            bind(statement, params);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                total = result.getLong(1);
            }
        }
        List<T> values = new ArrayList<T>();
        try (PreparedStatement statement = connection.prepareStatement(dataSql)) {
            bind(statement, params);
            statement.setInt(params.size() + 1, size);
            statement.setInt(params.size() + 2, offset(page, size));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(reader.read(result));
            }
        }
        return new DormPage<T>(page, size, total, values);
    }

    static String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    static Timestamp timestamp(ResultSet r, String column) throws SQLException { return r.getTimestamp(column); }
    static org.threeten.bp.LocalDateTime localTimestamp(ResultSet r, String column) throws SQLException {
        Timestamp value = timestamp(r, column);
        return JdbcTemporal.localDateTime(value);
    }
    static org.threeten.bp.LocalDate localDate(ResultSet r, String column) throws SQLException {
        Date value = r.getDate(column);
        return JdbcTemporal.localDate(value);
    }

    static DormBuildingDto building(ResultSet r) throws SQLException {
        return new DormBuildingDto(r.getLong("id"), r.getString("building_code"),
                r.getString("building_name"), r.getString("address"),
                r.getString("gender_policy"), r.getString("status"));
    }

    static DormRoomDto room(ResultSet r) throws SQLException {
        return new DormRoomDto(r.getLong("id"), r.getLong("building_id"),
                r.getString("building_code"), r.getString("building_name"),
                r.getString("room_no"), r.getInt("floor_no"), r.getInt("capacity"),
                r.getString("room_type"), r.getString("status"), r.getString("description"),
                r.getInt("occupied_beds"));
    }

    static DormBedDto bed(ResultSet r) throws SQLException {
        long occupant = r.getLong("occupant_user_id");
        return new DormBedDto(r.getLong("id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"), r.getString("bed_no"),
                r.getString("status"), r.wasNull() ? null : Long.valueOf(occupant));
    }

    static AccommodationDto accommodation(ResultSet r) throws SQLException {
        return new AccommodationDto(r.getLong("id"), r.getLong("student_user_id"),
                r.getLong("bed_id"), r.getLong("room_id"), r.getLong("building_id"),
                r.getString("building_code"), r.getString("building_name"),
                r.getString("room_no"), r.getString("bed_no"), localDate(r, "start_date"),
                localDate(r, "end_date"), r.getString("status"));
    }

    static AccommodationRequestDto request(ResultSet r) throws SQLException {
        Long reviewer = longOrNull(r, "reviewed_by");
        return new AccommodationRequestDto(r.getLong("id"), r.getLong("student_user_id"),
                r.getString("request_type"), longOrNull(r, "current_record_id"),
                longOrNull(r, "requested_bed_id"), r.getString("reason"), r.getString("status"),
                reviewer, localTimestamp(r, "reviewed_at"),
                r.getString("review_remark"), localTimestamp(r, "created_at"),
                stringOrNull(r, "student_name"), stringOrNull(r, "current_location"));
    }

    /** 列不在结果集里（旧查询没 JOIN）时返回 null，而不是抛 SQLException。 */
    private static String stringOrNull(ResultSet r, String column) {
        try { return r.getString(column); } catch (SQLException missing) { return null; }
    }

    static AccessRecordDto access(ResultSet r) throws SQLException {
        return new AccessRecordDto(r.getLong("id"), r.getLong("student_user_id"),
                r.getString("record_type"), localTimestamp(r, "occurred_at"),
                r.getString("door_name"), r.getString("source"), r.getString("note"));
    }

    static LateReturnAlertDto alert(ResultSet r) throws SQLException {
        long handled = r.getLong("handled_by");
        return new LateReturnAlertDto(r.getLong("id"), r.getLong("student_user_id"),
                localDate(r, "alert_date"), localTimestamp(r, "detected_at"),
                r.getString("status"), r.wasNull() ? null : Long.valueOf(handled),
                localTimestamp(r, "handled_at"), r.getString("note"));
    }

    static HygieneInspectionDto hygiene(ResultSet r) throws SQLException {
        return new HygieneInspectionDto(r.getLong("id"), r.getLong("room_id"),
                r.getLong("inspector_id"), localTimestamp(r, "inspected_at"),
                r.getBigDecimal("score"), r.getString("result"),
                r.getString("issue_description"), r.getString("status"),
                localTimestamp(r, "rectified_at"), r.getString("rectification_note"));
    }

    static RepairOrderDto repair(ResultSet r) throws SQLException {
        Long handler = longOrNull(r, "handler_id");
        int score = r.getInt("evaluation_score");
        boolean scoreNull = r.wasNull();
        return new RepairOrderDto(r.getLong("id"), r.getLong("room_id"), r.getLong("reporter_id"),
                r.getString("category"), r.getString("description"), r.getString("priority"),
                r.getString("status"), handler,
                localTimestamp(r, "submitted_at"), localTimestamp(r, "accepted_at"),
                localTimestamp(r, "completed_at"), scoreNull ? null : Integer.valueOf(score),
                r.getString("evaluation_note"));
    }

    static UtilityBillDto bill(ResultSet r) throws SQLException {
        long tx = r.getLong("paid_transaction_id");
        boolean transactionNull = r.wasNull();
        long student = r.getLong("student_user_id");
        boolean studentNull = r.wasNull();
        return new UtilityBillDto(r.getLong("allocation_id"), r.getLong("bill_id"),
                r.getLong("room_id"), studentNull ? null : Long.valueOf(student),
                r.getString("room_no"), localDate(r, "period_start"),
                localDate(r, "period_end"), r.getBigDecimal("electricity_units"),
                r.getBigDecimal("water_units"), r.getBigDecimal("total_amount"),
                r.getBigDecimal("allocated_amount"), r.getString("bill_status"),
                r.getString("allocation_status"), localTimestamp(r, "due_at"),
                transactionNull ? null : Long.valueOf(tx), localTimestamp(r, "paid_at"));
    }

    static DormAnnouncementDto announcement(ResultSet r) throws SQLException {
        return new DormAnnouncementDto(r.getLong("id"), r.getString("title"),
                r.getString("content"), r.getString("visible_scope"),
                longOrNull(r, "target_role_id"), r.getString("status"),
                localTimestamp(r, "publish_at"), localTimestamp(r, "expire_at"),
                r.getLong("publisher_id"));
    }

    static Long longOrNull(ResultSet r, String column) throws SQLException {
        long value = r.getLong(column);
        return r.wasNull() ? null : Long.valueOf(value);
    }

    interface Reader<T> { T read(ResultSet result) throws SQLException; }
}
