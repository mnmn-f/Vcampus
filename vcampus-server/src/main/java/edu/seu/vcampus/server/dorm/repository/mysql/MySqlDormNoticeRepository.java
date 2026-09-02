package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.db.JdbcTemporal;
import java.sql.*;
import java.util.*;
import org.threeten.bp.LocalDateTime;

/** MySQL persistence for scoped dorm notices and expiry processing. */
final class MySqlDormNoticeRepository {
    private static final String COLUMNS = "a.id, a.title, a.content, a.status, a.publish_at, a.expire_at, a.publisher_id, COALESCE(x.notice_type, 'GENERAL') AS notice_type, COALESCE(x.scope_type, 'ALL') AS scope_type, x.scope_building_id, x.scope_room_id, b.building_code, r.room_no, COALESCE(x.pinned, 0) AS pinned, x.pinned_at";
    private static final String FROM = " FROM announcements a LEFT JOIN dorm_notice_extras x ON x.announcement_id = a.id LEFT JOIN dorm_rooms r ON r.id = x.scope_room_id LEFT JOIN dorm_buildings b ON b.id = COALESCE(x.scope_building_id, r.building_id)";

    DormPage<NoticeExtraDto> list(Connection c, DormPageQuery query, boolean manage, Long viewerRoom) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query; List<Object> params = new ArrayList<Object>(); StringBuilder where = new StringBuilder(" WHERE a.module_code = 'DORM'");
        if (!manage) {
            where.append(" AND a.status = 'PUBLISHED' AND (a.publish_at IS NULL OR a.publish_at <= CURRENT_TIMESTAMP(3)) AND (a.expire_at IS NULL OR a.expire_at > CURRENT_TIMESTAMP(3)) AND (COALESCE(x.scope_type, 'ALL') = 'ALL'");
            if (viewerRoom != null) { where.append(" OR (x.scope_type = 'ROOM' AND x.scope_room_id = ?) OR (x.scope_type = 'BUILDING' AND x.scope_building_id = (SELECT vr.building_id FROM dorm_rooms vr WHERE vr.id = ?))"); params.add(viewerRoom); params.add(viewerRoom); }
            where.append(')');
        }
        String status = JdbcDormSupport.clean(q.getStatus()); if (status != null) { where.append(manage ? " AND a.status = ?" : " AND COALESCE(x.notice_type, 'GENERAL') = ?"); params.add(status); }
        String keyword = JdbcDormSupport.clean(q.getKeyword()); if (keyword != null) { where.append(" AND (a.title LIKE ? OR a.content LIKE ?)"); String like = "%" + keyword + "%"; params.add(like); params.add(like); }
        String order = " ORDER BY COALESCE(x.pinned, 0) DESC, x.pinned_at DESC, COALESCE(a.publish_at, a.created_at) DESC, a.id DESC";
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where, "SELECT " + COLUMNS + FROM + where + order + " LIMIT ? OFFSET ?", params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<NoticeExtraDto>() {
            @Override public NoticeExtraDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.notice(r); }
        });
    }

    NoticeExtraDto find(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE a.module_code = 'DORM' AND a.id = ?")) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? MySqlDormExtSupport.notice(r) : null; } }
    }

    NoticeExtraDto save(Connection c, NoticeExtraRequest request, long actor) throws SQLException {
        String sql = "INSERT INTO dorm_notice_extras(announcement_id,notice_type,scope_type,scope_building_id,scope_room_id,pinned,pinned_at,updated_by) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE notice_type = VALUES(notice_type), scope_type = VALUES(scope_type), scope_building_id = VALUES(scope_building_id), scope_room_id = VALUES(scope_room_id), pinned_at = CASE WHEN VALUES(pinned) = 0 THEN NULL WHEN pinned = 1 THEN pinned_at ELSE VALUES(pinned_at) END, pinned = VALUES(pinned), updated_by = VALUES(updated_by)";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, request.getAnnouncementId()); s.setString(2, request.getNoticeType()); s.setString(3, request.getScopeType()); nullable(s, 4, request.getScopeBuildingId()); nullable(s, 5, request.getScopeRoomId()); s.setBoolean(6, request.isPinned()); if (request.isPinned()) s.setTimestamp(7, JdbcTemporal.timestamp(LocalDateTime.now())); else s.setNull(7, Types.TIMESTAMP); s.setLong(8, actor); s.executeUpdate(); }
        return find(c, request.getAnnouncementId());
    }

    int expire(Connection c, LocalDateTime now) throws SQLException {
        String sql = "UPDATE announcements SET status = 'EXPIRED' WHERE module_code = 'DORM' AND status = 'PUBLISHED' AND expire_at IS NOT NULL AND expire_at < ?";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setTimestamp(1, JdbcTemporal.timestamp(now == null ? LocalDateTime.now() : now)); return s.executeUpdate(); }
    }

    private static void nullable(PreparedStatement s, int index, Long value) throws SQLException {
        if (value == null) s.setNull(index, Types.BIGINT); else s.setLong(index, value.longValue());
    }
}
