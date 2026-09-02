package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.server.library.repository.BookRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL 图书仓储；所有 SQL 使用参数绑定。 */
public final class MySqlBookRepository implements BookRepository {
    private static final String COLUMNS = "id,isbn,title,author,publisher,category,"
            + "total_copies,available_copies,location,description,status,created_at,updated_at";

    @Override
    public PageResult<BookDetail> search(Connection connection, BookSearchRequest request)
            throws java.sql.SQLException {
        JdbcLibrarySupport.QueryParts parts = parts(request);
        return JdbcLibrarySupport.page(connection, "SELECT COUNT(*) FROM books" + parts.where,
                "SELECT " + COLUMNS + " FROM books" + parts.where
                        + " ORDER BY updated_at DESC,id DESC LIMIT ? OFFSET ?",
                parts.params, request.getPage(), request.getPageSize(), new JdbcLibrarySupport.RowReader<BookDetail>() { public BookDetail read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.book(r); } });
    }

    @Override
    public BookDetail findById(Connection connection, long bookId)
            throws java.sql.SQLException {
        return find(connection, bookId, false);
    }

    @Override
    public BookDetail findByIdForUpdate(Connection connection, long bookId)
            throws java.sql.SQLException {
        return find(connection, bookId, true);
    }

    private BookDetail find(Connection connection, long id, boolean lock)
            throws java.sql.SQLException {
        String sql = "SELECT " + COLUMNS + " FROM books WHERE id = ?"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? JdbcLibrarySupport.book(result) : null;
            }
        }
    }

    @Override
    public BookDetail save(Connection connection, BookUpsertRequest request,
                           long operatorId) throws java.sql.SQLException {
        long id = request.getId();
        if (id <= 0) {
            String sql = "INSERT INTO books (isbn,title,author,publisher,category,"
                    + "total_copies,available_copies,location,description,status,created_by)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement statement = connection.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
                bindBook(statement, request, operatorId, false);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new java.sql.SQLException("book id was not generated");
                    }
                    id = keys.getLong(1);
                }
            }
        } else {
            String sql = "UPDATE books SET isbn=?,title=?,author=?,publisher=?,category=?,"
                    + "total_copies=?,available_copies=?,location=?,description=?,status=?"
                    + " WHERE id=?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindBook(statement, request, operatorId, true);
                statement.setLong(11, id);
                if (statement.executeUpdate() == 0) {
                    throw new java.sql.SQLException("book does not exist: " + id);
                }
            }
        }
        return findById(connection, id);
    }

    private static void bindBook(PreparedStatement statement, BookUpsertRequest request,
                                 long operatorId, boolean update)
            throws java.sql.SQLException {
        statement.setString(1, request.getIsbn());
        statement.setString(2, request.getTitle());
        statement.setString(3, request.getAuthor());
        statement.setString(4, request.getPublisher());
        statement.setString(5, request.getCategory());
        statement.setInt(6, request.getTotalCopies().intValue());
        statement.setInt(7, request.getAvailableCopies().intValue());
        statement.setString(8, request.getLocation());
        statement.setString(9, request.getDescription());
        statement.setString(10, request.getStatus());
        if (!update) {
            statement.setLong(11, operatorId);
        }
    }

    @Override
    public boolean decrementAvailable(Connection connection, long bookId)
            throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE books SET available_copies=available_copies-1"
                        + " WHERE id=? AND status='ON_SHELF' AND available_copies>0")) {
            statement.setLong(1, bookId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean incrementAvailable(Connection connection, long bookId)
            throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE books SET available_copies=LEAST(total_copies,available_copies+1)"
                        + " WHERE id=? AND available_copies<total_copies")) {
            statement.setLong(1, bookId);
            return statement.executeUpdate() == 1;
        }
    }

    private static JdbcLibrarySupport.QueryParts parts(BookSearchRequest request) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<Object>();
        String keyword = JdbcLibrarySupport.clean(request.getKeyword());
        if (keyword != null) {
            where.append(" AND (title LIKE ? OR author LIKE ? OR isbn LIKE ? OR category LIKE ?)");
            String value = "%" + keyword + "%";
            params.add(value); params.add(value); params.add(value); params.add(value);
        }
        add(where, params, "category", request.getCategory());
        add(where, params, "status", request.getStatus());
        return new JdbcLibrarySupport.QueryParts(where.toString(), params);
    }

    private static void add(StringBuilder where, List<Object> params, String column,
                            String value) {
        String clean = JdbcLibrarySupport.clean(value);
        if (clean != null) {
            where.append(" AND ").append(column).append("=?");
            params.add(clean);
        }
    }

}
