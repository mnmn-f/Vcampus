package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormAnnouncementRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 宿舍公告仓储，严格限制 module_code=DORM。 */
public final class MySqlDormAnnouncementRepository implements DormAnnouncementRepository {
    private static final String COLUMNS = "id,title,content,visible_scope,target_role_id,status,publish_at,expire_at,publisher_id";

    @Override
    public DormPage<DormAnnouncementDto> list(Connection c, DormPageQuery q, boolean drafts) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE module_code='DORM'";
        if (!drafts) where += " AND status='PUBLISHED' AND (publish_at IS NULL OR publish_at<=CURRENT_TIMESTAMP(3))"
                + " AND (expire_at IS NULL OR expire_at>CURRENT_TIMESTAMP(3))";
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND status=?"; p.add(query.getStatus().trim()); }
        if (JdbcDormSupport.clean(query.getKeyword()) != null) {
            where += " AND (title LIKE ? OR content LIKE ?)";
            String value = "%" + query.getKeyword().trim() + "%"; p.add(value); p.add(value);
        }
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM announcements" + where,
                "SELECT " + COLUMNS + " FROM announcements" + where
                        + " ORDER BY COALESCE(publish_at,created_at) DESC,id DESC LIMIT ? OFFSET ?",
                p, query.getPage(), query.getPageSize(), new JdbcDormSupport.Reader<DormAnnouncementDto>() { public DormAnnouncementDto read(ResultSet r) throws SQLException { return JdbcDormSupport.announcement(r); } });
    }

    @Override
    public DormAnnouncementDto save(Connection c, AnnouncementSaveRequest request, long actor)
            throws SQLException {
        long id = request.getId();
        if (id <= 0) {
            String sql = "INSERT INTO announcements(module_code,title,content,visible_scope,target_role_id,status,publish_at,expire_at,publisher_id)"
                    + " VALUES('DORM',?,?,?,?,?,?,?,?)";
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(s, request, actor, false); s.executeUpdate();
                try (ResultSet r = s.getGeneratedKeys()) {
                    if (!r.next()) throw new SQLException("announcement id was not generated");
                    id = r.getLong(1);
                }
            }
        } else {
            String sql = "UPDATE announcements SET title=?,content=?,visible_scope=?,target_role_id=?,status=?,publish_at=?,expire_at=?"
                    + " WHERE id=? AND module_code='DORM'";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                bind(s, request, actor, true); s.setLong(8, id);
                if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.NOT_FOUND, "宿舍公告不存在");
            }
        }
        return find(c, id);
    }

    private DormAnnouncementDto find(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM announcements WHERE id=? AND module_code='DORM'")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.announcement(r) : null; }
        }
    }

    private static void bind(PreparedStatement s, AnnouncementSaveRequest r, long actor, boolean update)
            throws SQLException {
        s.setString(1, r.getTitle()); s.setString(2, r.getContent());
        s.setString(3, r.getVisibleScope() == null ? "ALL" : r.getVisibleScope());
        if (r.getTargetRoleId() == null) s.setNull(4, java.sql.Types.BIGINT); else s.setLong(4, r.getTargetRoleId());
        s.setString(5, r.getStatus() == null ? "DRAFT" : r.getStatus());
        if (r.getPublishAt() == null) s.setNull(6, java.sql.Types.TIMESTAMP); else s.setTimestamp(6, JdbcTemporal.timestamp(r.getPublishAt()));
        if (r.getExpireAt() == null) s.setNull(7, java.sql.Types.TIMESTAMP); else s.setTimestamp(7, JdbcTemporal.timestamp(r.getExpireAt()));
        if (!update) s.setLong(8, actor);
    }
}
