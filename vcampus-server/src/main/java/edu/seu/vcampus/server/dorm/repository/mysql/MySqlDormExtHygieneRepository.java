package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** MySQL persistence for hygiene inspections and generated tasks. */
final class MySqlDormExtHygieneRepository {
    private static final String TASK_COLUMNS = "t.id, t.room_id, b.building_code, r.room_no, t.task_type, t.plan_date, t.status, t.inspection_id, t.source_inspection_id";
    private static final String TASK_FROM = " FROM dorm_hygiene_tasks t JOIN dorm_rooms r ON r.id = t.room_id JOIN dorm_buildings b ON b.id = r.building_id";

    long createInspection(Connection c, long room, long inspector, LocalDateTime at, BigDecimal total,
                          String result, String status, String issue) throws SQLException {
        String sql = "INSERT INTO hygiene_inspections(room_id,inspector_id,inspected_at,score,result,issue_description,status) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, room); s.setLong(2, inspector); s.setTimestamp(3, JdbcTemporal.timestamp(at)); s.setBigDecimal(4, total);
            s.setString(5, result); s.setString(6, JdbcDormSupport.clean(issue)); s.setString(7, status); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("hygiene inspection id was not generated"); return keys.getLong(1); }
        }
    }

    void saveItemScores(Connection c, long inspection, List<HygieneItemScoreDto> items) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO dorm_hygiene_item_scores(inspection_id,item_code,score,deduct_reason) VALUES(?,?,?,?)")) {
            for (HygieneItemScoreDto item : items) { s.setLong(1, inspection); s.setString(2, item.getItemCode()); s.setBigDecimal(3, item.getScore()); s.setString(4, JdbcDormSupport.clean(item.getDeductReason())); s.addBatch(); }
            s.executeBatch();
        }
    }

    HygieneDetailDto detail(Connection c, long inspection) throws SQLException {
        String sql = "SELECT h.id, h.room_id, b.building_code, r.room_no, h.inspector_id, h.inspected_at, h.score, h.issue_description FROM hygiene_inspections h JOIN dorm_rooms r ON r.id = h.room_id JOIN dorm_buildings b ON b.id = r.building_id WHERE h.id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, inspection); try (ResultSet r = s.executeQuery()) {
            if (!r.next()) return null;
            BigDecimal total = r.getBigDecimal("score"); LocalDateTime at = JdbcDormSupport.localTimestamp(r, "inspected_at"); boolean rectify = DormHygieneRules.needRectify(total);
            return new HygieneDetailDto(r.getLong("id"), r.getLong("room_id"), r.getString("building_code"), r.getString("room_no"), r.getLong("inspector_id"), at, total, DormHygieneRules.level(total), rectify, rectify ? DormHygieneRules.recheckDate(at.toLocalDate()) : null, r.getString("issue_description"), itemScores(c, inspection));
        } }
    }

    List<Long> rooms(Connection c, Long building) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id FROM dorm_rooms WHERE status <> 'CLOSED'"); if (building != null) sql.append(" AND building_id = ?"); sql.append(" ORDER BY id");
        List<Long> rows = new ArrayList<Long>(); try (PreparedStatement s = c.prepareStatement(sql.toString())) { if (building != null) s.setLong(1, building.longValue()); try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(Long.valueOf(r.getLong(1))); } } return rows;
    }

    boolean createTaskIfAbsent(Connection c, long room, String type, LocalDate date, Long source) throws SQLException {
        String sql = "INSERT IGNORE INTO dorm_hygiene_tasks(room_id,task_type,plan_date,source_inspection_id) VALUES(?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, room); s.setString(2, type); s.setDate(3, JdbcTemporal.date(date)); if (source == null) s.setNull(4, Types.BIGINT); else s.setLong(4, source.longValue()); return s.executeUpdate() > 0; }
    }

    int markTasksDone(Connection c, long room, LocalDate before, long inspection) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_hygiene_tasks SET status = 'DONE', inspection_id = ? WHERE room_id = ? AND status = 'PENDING' AND plan_date <= ?")) { s.setLong(1, inspection); s.setLong(2, room); s.setDate(3, JdbcTemporal.date(before)); return s.executeUpdate(); }
    }

    DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery query) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query; List<Object> params = new ArrayList<Object>(); StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) { where.append(" AND t.room_id = ?"); params.add(q.getRoomId()); } if (q.getBuildingId() != null) { where.append(" AND r.building_id = ?"); params.add(q.getBuildingId()); }
        String status = JdbcDormSupport.clean(q.getStatus()); if (status != null) { where.append(" AND t.status = ?"); params.add(status); }
        String keyword = JdbcDormSupport.clean(q.getKeyword()); if (keyword != null) { where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ? OR t.task_type LIKE ?)"); String like = "%" + keyword + "%"; params.add(like); params.add(like); params.add(like); }
        String count = "SELECT COUNT(*)" + TASK_FROM + where; String data = "SELECT " + TASK_COLUMNS + TASK_FROM + where + " ORDER BY CASE t.status WHEN 'PENDING' THEN 0 WHEN 'SKIPPED' THEN 1 ELSE 2 END, CASE t.task_type WHEN 'RECHECK' THEN 0 ELSE 1 END, t.plan_date ASC, t.id ASC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, count, data, params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<HygieneTaskDto>() {
            @Override public HygieneTaskDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.task(r); }
        });
    }

    private static List<HygieneItemScoreDto> itemScores(Connection c, long inspection) throws SQLException {
        List<HygieneItemScoreDto> rows = new ArrayList<HygieneItemScoreDto>();
        try (PreparedStatement s = c.prepareStatement("SELECT item_code, score, deduct_reason FROM dorm_hygiene_item_scores WHERE inspection_id = ? ORDER BY id")) { s.setLong(1, inspection); try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(new HygieneItemScoreDto(r.getString("item_code"), r.getBigDecimal("score"), r.getString("deduct_reason"))); } }
        return rows;
    }
}
