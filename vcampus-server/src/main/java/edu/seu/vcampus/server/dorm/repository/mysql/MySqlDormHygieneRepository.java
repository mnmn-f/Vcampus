package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 卫生检查与整改仓储。 */
final class MySqlDormHygieneRepository {
    DormPage<HygieneInspectionDto> list(Connection c, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (query.getRoomId() != null) { where += " AND room_id=?"; p.add(query.getRoomId()); }
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND status=?"; p.add(query.getStatus().trim()); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM hygiene_inspections" + where,
                "SELECT id,room_id,inspector_id,inspected_at,score,result,issue_description,status,"
                        + "rectified_at,rectification_note FROM hygiene_inspections" + where
                        + " ORDER BY inspected_at DESC,id DESC LIMIT ? OFFSET ?", p, query.getPage(), query.getPageSize(),
                new JdbcDormSupport.Reader<HygieneInspectionDto>() { public HygieneInspectionDto read(ResultSet r) throws SQLException { return JdbcDormSupport.hygiene(r); } });
    }

    HygieneInspectionDto save(Connection c, HygieneInspectionRequest request, long actor)
            throws SQLException {
        String status = request.getStatus() == null ? "NORMAL" : request.getStatus();
        long id = request.getId();
        if (id <= 0) {
            String sql = "INSERT INTO hygiene_inspections(room_id,inspector_id,score,result,issue_description,status,rectified_at,rectification_note)"
                    + " VALUES(?,?,?,?,?,?,IF(?='RECTIFIED',CURRENT_TIMESTAMP(3),NULL),?)";
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                s.setLong(1, request.getRoomId()); s.setLong(2, actor); s.setBigDecimal(3, request.getScore());
                s.setString(4, request.getResult()); s.setString(5, request.getIssueDescription());
                s.setString(6, status); s.setString(7, status); s.setString(8, request.getRectificationNote()); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("inspection id was not generated");
                    id = keys.getLong(1);
                }
            }
        } else {
            String sql = "UPDATE hygiene_inspections SET room_id=?,inspector_id=?,score=?,result=?,issue_description=?,"
                    + "status=?,rectified_at=IF(?='RECTIFIED',COALESCE(rectified_at,CURRENT_TIMESTAMP(3)),rectified_at),"
                    + "rectification_note=? WHERE id=?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setLong(1, request.getRoomId()); s.setLong(2, actor); s.setBigDecimal(3, request.getScore());
                s.setString(4, request.getResult()); s.setString(5, request.getIssueDescription());
                s.setString(6, status); s.setString(7, status); s.setString(8, request.getRectificationNote()); s.setLong(9, id);
                if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.NOT_FOUND, "卫生检查不存在");
            }
        }
        return find(c, id);
    }

    private HygieneInspectionDto find(Connection c, long id) throws SQLException {
        String sql = "SELECT id,room_id,inspector_id,inspected_at,score,result,issue_description,status,rectified_at,rectification_note"
                + " FROM hygiene_inspections WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.hygiene(r) : null; }
        }
    }
}
