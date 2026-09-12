package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 报修工单仓储；合法状态转换由服务层校验，SQL 仍锁定目标行。 */
final class MySqlDormRepairRepository {
    private static final String COLUMNS = "o.id,o.room_id,o.reporter_id,o.category,o.description,o.priority,o.status,"
            + "o.handler_id,o.submitted_at,o.accepted_at,o.completed_at,o.evaluation_score,o.evaluation_note,"
            + "b.building_name AS building_name,r.room_no AS room_no,reporter.display_name AS reporter_name,"
            + "reporter.username AS reporter_username,handler.display_name AS handler_name,"
            + "handler.username AS handler_username";
    private static final String FROM = " FROM repair_orders o"
            + " JOIN dorm_rooms r ON r.id=o.room_id"
            + " JOIN dorm_buildings b ON b.id=r.building_id"
            + " JOIN users reporter ON reporter.id=o.reporter_id"
            + " LEFT JOIN users handler ON handler.id=o.handler_id";

    DormPage<RepairOrderDto> list(Connection c, Long reporter, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (reporter != null) { where += " AND o.reporter_id=?"; p.add(reporter); }
        if (query.getRoomId() != null) { where += " AND o.room_id=?"; p.add(query.getRoomId()); }
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND o.status=?"; p.add(query.getStatus().trim()); }
        if (JdbcDormSupport.clean(query.getKeyword()) != null) {
            String value = "%" + query.getKeyword().trim() + "%";
            where += " AND (reporter.display_name LIKE ? OR reporter.username LIKE ?"
                    + " OR r.room_no LIKE ? OR b.building_name LIKE ? OR o.category LIKE ? OR o.description LIKE ?)";
            for (int i = 0; i < 6; i++) p.add(value);
        }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                "SELECT " + COLUMNS + FROM + where
                        + " ORDER BY o.submitted_at DESC,o.id DESC LIMIT ? OFFSET ?", p, query.getPage(), query.getPageSize(),
                new JdbcDormSupport.Reader<RepairOrderDto>() { public RepairOrderDto read(ResultSet r) throws SQLException { return JdbcDormSupport.repair(r); } });
    }

    RepairOrderDto create(Connection c, RepairCreateRequest request, long reporter) throws SQLException {
        String sql = "INSERT INTO repair_orders(room_id,reporter_id,category,description,priority,status)"
                + " VALUES(?,?,?,?,?,'SUBMITTED')";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, request.getRoomId()); s.setLong(2, reporter); s.setString(3, request.getCategory());
            s.setString(4, request.getDescription()); s.setString(5, request.getPriority()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("repair id was not generated");
                id = keys.getLong(1);
            }
        }
        return lock(c, id);
    }

    RepairOrderDto lock(Connection c, long id) throws SQLException {
        try (PreparedStatement lock = c.prepareStatement("SELECT id FROM repair_orders WHERE id=? FOR UPDATE")) {
            lock.setLong(1, id);
            try (ResultSet r = lock.executeQuery()) { if (!r.next()) return null; }
        }
        String sql = "SELECT " + COLUMNS + FROM + " WHERE o.id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.repair(r) : null; }
        }
    }

    RepairOrderDto update(Connection c, RepairStatusRequest request, long actor) throws SQLException {
        RepairOrderDto old = lock(c, request.getOrderId());
        if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        String sql = "UPDATE repair_orders SET status=?,evaluation_score=?,evaluation_note=?,"
                + "accepted_at=IF(?='ACCEPTED' AND accepted_at IS NULL,CURRENT_TIMESTAMP(3),accepted_at),"
                + "completed_at=IF(?='COMPLETED',CURRENT_TIMESTAMP(3),completed_at) WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, request.getStatus());
            if (request.getEvaluationScore() == null) s.setNull(2, java.sql.Types.TINYINT);
            else s.setInt(2, request.getEvaluationScore());
            s.setString(3, request.getEvaluationNote()); s.setString(4, request.getStatus());
            s.setString(5, request.getStatus()); s.setLong(6, request.getOrderId());
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        }
        if ("ACCEPTED".equals(request.getStatus()) || "IN_PROGRESS".equals(request.getStatus())) {
            try (PreparedStatement s = c.prepareStatement("UPDATE repair_orders SET handler_id=COALESCE(handler_id,?) WHERE id=?")) {
                s.setLong(1, actor); s.setLong(2, request.getOrderId()); s.executeUpdate();
            }
        }
        return lock(c, request.getOrderId());
    }

    RepairOrderDto evaluate(Connection c, RepairEvaluationRequest request, long student) throws SQLException {
        RepairOrderDto old = lock(c, request.getOrderId());
        if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        if (!"COMPLETED".equals(old.getStatus()) || old.getEvaluationScore() != null) {
            throw new DormRepositoryException(DormCommands.REPAIR_INVALID_STATE, "报修工单当前不可评价");
        }
        String sql = "UPDATE repair_orders SET evaluation_score=?,evaluation_note=?"
                + " WHERE id=? AND status='COMPLETED' AND evaluation_score IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, request.getScore()); s.setString(2, request.getNote()); s.setLong(3, request.getOrderId());
            if (s.executeUpdate() != 1) throw new DormRepositoryException(
                    DormCommands.REPAIR_INVALID_STATE, "报修工单当前不可评价");
        }
        return lock(c, request.getOrderId());
    }
}
