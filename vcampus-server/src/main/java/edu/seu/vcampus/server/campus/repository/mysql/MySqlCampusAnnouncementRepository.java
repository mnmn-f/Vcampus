package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.server.campus.repository.CampusAnnouncementRepository;
import edu.seu.vcampus.server.campus.repository.CampusRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** announcements 表的 MySQL 专责仓储。 */
public final class MySqlCampusAnnouncementRepository implements CampusAnnouncementRepository {
    private static final String COLUMNS = "id,module_code,title,content,visible_scope,target_role_id,status,publish_at,expire_at,publisher_id";

    @Override public CampusPage<CampusAnnouncementDto> list(Connection c, CampusPageQuery q,
            String module, String role, boolean drafts) throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (module != null) { where += " AND module_code=?"; values.add(module); }
        if (!drafts) where += " AND status IN ('PUBLISHED','SCHEDULED') AND (publish_at IS NULL OR publish_at<=CURRENT_TIMESTAMP(3)) AND (expire_at IS NULL OR expire_at>CURRENT_TIMESTAMP(3))";
        if (role != null) {
            where += " AND (visible_scope='ALL' OR target_role_id IN (SELECT id FROM roles WHERE code=?))";
            values.add(role);
        }
        if (query.getStatus() != null) { where += " AND status=?"; values.add(query.getStatus()); }
        if (query.getKeyword() != null) { where += " AND (title LIKE ? OR content LIKE ?)"; values.add("%" + query.getKeyword() + "%"); values.add("%" + query.getKeyword() + "%"); }
        String count = "SELECT COUNT(*) FROM announcements" + where;
        String data = "SELECT " + COLUMNS + " FROM announcements" + where
                + " ORDER BY COALESCE(publish_at,created_at) DESC,id DESC LIMIT ? OFFSET ?";
        return JdbcCampusSupport.page(c, count, data, values, query, new JdbcCampusSupport.Mapper<CampusAnnouncementDto>() { public CampusAnnouncementDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.announcement(r); } });
    }

    @Override public CampusAnnouncementDto findAnnouncement(Connection c, long id) throws SQLException {
        return find(c, id, false);
    }

    @Override public CampusAnnouncementDto lockAnnouncement(Connection c, long id) throws SQLException {
        return find(c, id, true);
    }

    @Override public CampusAnnouncementDto save(Connection c, CampusAnnouncementSaveRequest r,
            long publisher) throws SQLException {
        Long roleId = resolveRole(c, r);
        long id = r.getId() == null ? insert(c, r, roleId, publisher) : update(c, r, roleId);
        CampusAnnouncementDto value = findAnnouncement(c, id);
        if (value == null) throw new CampusRepositoryException(CampusCommands.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
        return value;
    }

    private long insert(Connection c, CampusAnnouncementSaveRequest r, Long roleId, long publisher) throws SQLException {
        String sql = "INSERT INTO announcements(module_code,title,content,visible_scope,target_role_id,status,publish_at,expire_at,publisher_id) VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, r, roleId, 1); s.setLong(9, publisher); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("announcement id missing"); return keys.getLong(1); }
        }
    }

    private long update(Connection c, CampusAnnouncementSaveRequest r, Long roleId) throws SQLException {
        String sql = "UPDATE announcements SET module_code=?,title=?,content=?,visible_scope=?,target_role_id=?,status=?,publish_at=?,expire_at=? WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, r, roleId, 1); s.setLong(9, r.getId().longValue());
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
            return r.getId().longValue();
        }
    }

    private CampusAnnouncementDto find(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM announcements WHERE id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.announcement(r) : null; } }
    }

    private Long resolveRole(Connection c, CampusAnnouncementSaveRequest r) throws SQLException {
        if (r.getTargetRoleId() != null) return r.getTargetRoleId();
        if (r.getTargetRoleCode() == null) return null;
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM roles WHERE code=?")) {
            s.setString(1, r.getTargetRoleCode());
            try (ResultSet result = s.executeQuery()) {
                if (!result.next()) throw new CampusRepositoryException(CampusCommands.ANNOUNCEMENT_VISIBILITY, "目标角色不存在");
                return Long.valueOf(result.getLong(1));
            }
        }
    }

    private static int bind(PreparedStatement s, CampusAnnouncementSaveRequest r, Long roleId, int i) throws SQLException {
        s.setString(i++, r.getModuleCode()); s.setString(i++, r.getTitle()); s.setString(i++, r.getContent());
        s.setString(i++, r.getVisibleScope() == null ? "ALL" : r.getVisibleScope());
        if (roleId == null) s.setNull(i++, java.sql.Types.BIGINT); else s.setLong(i++, roleId.longValue());
        s.setString(i++, r.getStatus() == null ? "DRAFT" : r.getStatus());
        if (r.getPublishAt() == null) s.setNull(i++, java.sql.Types.TIMESTAMP); else s.setTimestamp(i++, JdbcTemporal.timestamp(r.getPublishAt()));
        if (r.getExpireAt() == null) s.setNull(i++, java.sql.Types.TIMESTAMP); else s.setTimestamp(i++, JdbcTemporal.timestamp(r.getExpireAt()));
        return i;
    }
}
