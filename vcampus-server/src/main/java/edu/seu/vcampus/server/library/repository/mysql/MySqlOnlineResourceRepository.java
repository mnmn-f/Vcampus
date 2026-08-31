package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.server.library.repository.OnlineResourceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 线上资源仓储。 */
public final class MySqlOnlineResourceRepository implements OnlineResourceRepository {
    private static final String COLUMNS = "id,title,resource_type,url,description,"
            + "publisher_id,status,published_at";

    @Override
    public OnlineResourceView findById(Connection connection, long resourceId)
            throws java.sql.SQLException {
        try (PreparedStatement s = connection.prepareStatement("SELECT " + COLUMNS
                + " FROM online_resources WHERE id=?")) {
            s.setLong(1, resourceId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? JdbcLibrarySupport.resource(r) : null;
            }
        }
    }

    @Override
    public PageResult<OnlineResourceView> search(Connection connection,
                                                  OnlineResourceSearchRequest request)
            throws java.sql.SQLException {
        JdbcLibrarySupport.QueryParts parts = parts(request);
        return JdbcLibrarySupport.page(connection,
                "SELECT COUNT(*) FROM online_resources WHERE 1=1" + parts.where,
                "SELECT " + COLUMNS + " FROM online_resources WHERE 1=1" + parts.where
                        + " ORDER BY published_at DESC,id DESC LIMIT ? OFFSET ?",
                parts.params, request.getPage(), request.getPageSize(),
                new JdbcLibrarySupport.RowReader<OnlineResourceView>() { public OnlineResourceView read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.resource(r); } });
    }

    @Override
    public OnlineResourceView save(Connection c, OnlineResourceUpsertRequest request,
                                   long publisherId) throws java.sql.SQLException {
        long id = request.getId();
        if (id <= 0) {
            String sql = "INSERT INTO online_resources (title,resource_type,url,description,"
                    + "publisher_id,status,published_at) VALUES (?,?,?,?,?,?,IF(?='ACTIVE',NOW(3),NULL))";
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindResource(s, request, publisherId, false); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    if (!keys.next()) throw new java.sql.SQLException("resource id not generated");
                    id = keys.getLong(1);
                }
            }
        } else {
            String sql = "UPDATE online_resources SET title=?,resource_type=?,url=?,"
                    + "description=?,status=?,published_at=IF(?='ACTIVE',COALESCE(published_at,NOW(3)),published_at)"
                    + " WHERE id=?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setString(1, request.getTitle()); s.setString(2, request.getResourceType());
                s.setString(3, request.getUrl()); s.setString(4, request.getDescription());
                s.setString(5, request.getStatus()); s.setString(6, request.getStatus());
                s.setLong(7, id);
                if (s.executeUpdate() == 0) throw new java.sql.SQLException("resource not found");
            }
        }
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS
                + " FROM online_resources WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new java.sql.SQLException("resource not found after save");
                return JdbcLibrarySupport.resource(r);
            }
        }
    }

    private static void bindResource(PreparedStatement s, OnlineResourceUpsertRequest r,
                                     long publisherId, boolean update)
            throws java.sql.SQLException {
        s.setString(1, r.getTitle()); s.setString(2, r.getResourceType());
        s.setString(3, r.getUrl()); s.setString(4, r.getDescription());
        s.setLong(5, publisherId); s.setString(6, r.getStatus());
        s.setString(7, r.getStatus());
    }

    private static JdbcLibrarySupport.QueryParts parts(OnlineResourceSearchRequest r) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        String keyword = JdbcLibrarySupport.clean(r.getKeyword());
        if (keyword != null) {
            where.append(" AND (title LIKE ? OR description LIKE ?)");
            String value = "%" + keyword + "%"; params.add(value); params.add(value);
        }
        String type = JdbcLibrarySupport.clean(r.getResourceType());
        if (type != null) { where.append(" AND resource_type=?"); params.add(type); }
        String status = JdbcLibrarySupport.clean(r.getStatus());
        if (status != null) { where.append(" AND status=?"); params.add(status); }
        return new JdbcLibrarySupport.QueryParts(where.toString(), params);
    }
}
