package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.server.campus.repository.CampusRepositoryException;
import edu.seu.vcampus.server.campus.repository.CampusSrtpRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** srtp_records 表的 MySQL 专责仓储。 */
public final class MySqlCampusSrtpRepository implements CampusSrtpRepository {
    private static final String COLUMNS = "id,project_code,student_user_id,title,description,credits,status,submitted_at,reviewed_by,reviewed_at,review_remark";

    @Override public CampusPage<SrtpRecordDto> list(Connection c, CampusPageQuery q, Long student)
            throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>(); String where = " WHERE 1=1";
        if (student != null) { where += " AND student_user_id=?"; values.add(student); }
        if (query.getStatus() != null) { where += " AND status=?"; values.add(query.getStatus()); }
        if (query.getKeyword() != null) { where += " AND (project_code LIKE ? OR title LIKE ?)"; values.add("%" + query.getKeyword() + "%"); values.add("%" + query.getKeyword() + "%"); }
        return JdbcCampusSupport.page(c, "SELECT COUNT(*) FROM srtp_records" + where,
                "SELECT " + COLUMNS + " FROM srtp_records" + where + " ORDER BY submitted_at DESC,id DESC LIMIT ? OFFSET ?",
                values, query, new JdbcCampusSupport.Mapper<SrtpRecordDto>() { public SrtpRecordDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.srtp(r); } });
    }

    @Override public SrtpRecordDto findSrtp(Connection c, long id) throws SQLException { return find(c, id, false); }
    @Override public SrtpRecordDto lockSrtp(Connection c, long id) throws SQLException { return find(c, id, true); }

    @Override public SrtpRecordDto save(Connection c, SrtpSaveRequest r, long defaultStudent,
            long actor, boolean administrator) throws SQLException {
        try {
            if (r.getId() == null) return insert(c, r, defaultStudent, administrator);
            SrtpRecordDto old = lockSrtp(c, r.getId().longValue());
            if (old == null) throw new CampusRepositoryException(CampusCommands.SRTP_NOT_FOUND, "SRTP记录不存在");
            long student = administrator && r.getStudentUserId() != null ? r.getStudentUserId().longValue() : old.getStudentUserId();
            String sql = "UPDATE srtp_records SET project_code=?,student_user_id=?,title=?,description=?,credits=?,status=? WHERE id=?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                bind(s, r, student, 1); s.setLong(7, old.getId()); s.executeUpdate();
            }
            return findSrtp(c, old.getId());
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new CampusRepositoryException(CampusCommands.INVALID_INPUT, "项目编号已存在", ex);
        }
    }

    @Override public SrtpRecordDto review(Connection c, SrtpStatusRequest r, long reviewer)
            throws SQLException {
        String sql = "UPDATE srtp_records SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),review_remark=? WHERE id=? AND status='SUBMITTED'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.getStatus()); s.setLong(2, reviewer); s.setString(3, r.getRemark()); s.setLong(4, r.getRecordId());
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.SRTP_INVALID_STATE, "该记录已处理");
        }
        return findSrtp(c, r.getRecordId());
    }

    private SrtpRecordDto insert(Connection c, SrtpSaveRequest r, long defaultStudent,
            boolean administrator) throws SQLException {
        long student = administrator && r.getStudentUserId() != null ? r.getStudentUserId().longValue() : defaultStudent;
        String sql = "INSERT INTO srtp_records(project_code,student_user_id,title,description,credits,status) VALUES(?,?,?,?,?,?)";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, r, student, 1); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("srtp id missing"); id = keys.getLong(1); }
        }
        return findSrtp(c, id);
    }

    private SrtpRecordDto find(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM srtp_records WHERE id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.srtp(r) : null; } }
    }

    private static int bind(PreparedStatement s, SrtpSaveRequest r, long student, int i) throws SQLException {
        s.setString(i++, r.getProjectCode()); s.setLong(i++, student); s.setString(i++, r.getTitle()); s.setString(i++, r.getDescription());
        if (r.getCredits() == null) s.setNull(i++, java.sql.Types.DECIMAL); else s.setBigDecimal(i++, r.getCredits());
        s.setString(i++, r.getStatus() == null ? "SUBMITTED" : r.getStatus()); return i;
    }
}
