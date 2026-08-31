package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.server.library.repository.BorrowRepository;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** MySQL 借阅仓储；库存与记录的事务由上层服务控制。 */
public final class MySqlBorrowRepository implements BorrowRepository {
    private static final String BASE = "SELECT br.id,br.book_id,b.title AS book_title,"
            + "br.borrower_user_id,u.display_name AS borrower_name,br.issued_at,br.due_at,"
            + "br.returned_at,CASE WHEN br.status='BORROWED' AND br.due_at<NOW(3) "
            + "THEN 'OVERDUE' ELSE br.status END AS effective_status,"
            + "br.renew_count,br.remark FROM borrow_records br "
            + "JOIN books b ON b.id=br.book_id JOIN users u ON u.id=br.borrower_user_id";
    private static final String COUNT_BASE = "SELECT COUNT(*) FROM borrow_records br "
            + "JOIN books b ON b.id=br.book_id JOIN users u ON u.id=br.borrower_user_id";

    @Override
    public PageResult<BorrowRecordView> search(Connection connection,
                                               BorrowSearchRequest request,
                                               Long borrowerUserId)
            throws java.sql.SQLException {
        JdbcLibrarySupport.QueryParts parts = parts(request, borrowerUserId);
        return JdbcLibrarySupport.page(connection,
                "SELECT COUNT(*) FROM borrow_records br WHERE 1=1" + parts.where,
                BASE + " WHERE 1=1" + parts.where
                        + " ORDER BY br.issued_at DESC,br.id DESC LIMIT ? OFFSET ?",
                parts.params, request.getPage(), request.getPageSize(), new JdbcLibrarySupport.RowReader<BorrowRecordView>() { public BorrowRecordView read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.borrow(r); } });
    }

    @Override
    public PageResult<BorrowRecordView> searchAdmin(Connection connection,
                                                    BorrowAdminSearchRequest request)
            throws java.sql.SQLException {
        BorrowAdminSearchRequest query = request == null
                ? new BorrowAdminSearchRequest() : request;
        JdbcLibrarySupport.QueryParts parts = adminParts(query);
        return JdbcLibrarySupport.page(connection,
                COUNT_BASE + " WHERE 1=1" + parts.where,
                BASE + " WHERE 1=1" + parts.where
                        + " ORDER BY br.issued_at DESC,br.id DESC LIMIT ? OFFSET ?",
                parts.params, query.getPage(), query.getPageSize(), new JdbcLibrarySupport.RowReader<BorrowRecordView>() { public BorrowRecordView read(ResultSet r) throws java.sql.SQLException { return JdbcLibrarySupport.borrow(r); } });
    }

    @Override
    public BorrowRecordView findById(Connection connection, long recordId)
            throws java.sql.SQLException {
        return find(connection, " WHERE br.id=?", recordId, false);
    }

    @Override
    public BorrowRecordView findByIdForUpdate(Connection connection,
                                                        long recordId)
            throws java.sql.SQLException {
        return find(connection, " WHERE br.id=?", recordId, true);
    }

    @Override
    public BorrowRecordView findActiveByBookAndUserForUpdate(
            Connection connection, long bookId, long userId) throws java.sql.SQLException {
        String where = " WHERE br.book_id=? AND br.borrower_user_id=?"
                + " AND br.status IN ('BORROWED','OVERDUE') FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(BASE + where)) {
            statement.setLong(1, bookId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? JdbcLibrarySupport.borrow(result) : null;
            }
        }
    }

    private BorrowRecordView find(Connection connection, String where, long id,
                                            boolean lock) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(BASE + where
                + (lock ? " FOR UPDATE" : ""))) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? JdbcLibrarySupport.borrow(result) : null;
            }
        }
    }

    @Override
    public BorrowRecordView insert(Connection connection, long bookId, long userId,
                                   LocalDateTime issuedAt, LocalDateTime dueAt,
                                   long handledBy, String remark)
            throws java.sql.SQLException {
        String sql = "INSERT INTO borrow_records (book_id,borrower_user_id,issued_at,due_at,"
                + "status,handled_by,remark) VALUES (?,?,?,?,'BORROWED',?,?)";
        long id;
        try (PreparedStatement statement = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, bookId);
            statement.setLong(2, userId);
            statement.setTimestamp(3, JdbcTemporal.timestamp(issuedAt));
            statement.setTimestamp(4, JdbcTemporal.timestamp(dueAt));
            if (handledBy > 0) statement.setLong(5, handledBy);
            else statement.setNull(5, java.sql.Types.BIGINT);
            statement.setString(6, remark);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new java.sql.SQLException("borrow id not generated");
                id = keys.getLong(1);
            }
        }
        return findById(connection, id);
    }

    @Override
    public boolean markReturned(Connection connection, long recordId, long handledBy,
                                LocalDateTime returnedAt, String remark)
            throws java.sql.SQLException {
        String sql = "UPDATE borrow_records SET returned_at=?,status='RETURNED',"
                + "handled_by=?,remark=? WHERE id=? AND status IN ('BORROWED','OVERDUE')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, JdbcTemporal.timestamp(returnedAt));
            statement.setLong(2, handledBy);
            statement.setString(3, remark);
            statement.setLong(4, recordId);
            return statement.executeUpdate() == 1;
        }
    }

    private static JdbcLibrarySupport.QueryParts parts(BorrowSearchRequest request, Long userId) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        if (userId != null) { where.append(" AND br.borrower_user_id=?"); params.add(userId); }
        String status = JdbcLibrarySupport.clean(request.getStatus());
        if (status != null) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                where.append(" AND (br.status='OVERDUE' OR (br.status='BORROWED'"
                        + " AND br.due_at<NOW(3)))");
            } else {
                where.append(" AND br.status=?"); params.add(status);
            }
        }
        return new JdbcLibrarySupport.QueryParts(where.toString(), params);
    }

    private static JdbcLibrarySupport.QueryParts adminParts(BorrowAdminSearchRequest request) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        if (request.getStudentId() != null) {
            where.append(" AND br.borrower_user_id=?"); params.add(request.getStudentId());
        }
        String status = JdbcLibrarySupport.clean(request.getStatus());
        if (status != null) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                where.append(" AND (br.status='OVERDUE' OR (br.status='BORROWED'"
                        + " AND br.due_at<NOW(3)))");
            } else {
                where.append(" AND br.status=?"); params.add(status);
            }
        }
        String keyword = JdbcLibrarySupport.clean(request.getKeyword());
        if (keyword != null) {
            String like = "%" + keyword + "%";
            where.append(" AND (b.title LIKE ? OR u.display_name LIKE ? OR u.username LIKE ?"
                    + " OR CAST(br.borrower_user_id AS CHAR) LIKE ? OR br.remark LIKE ?)");
            for (int i = 0; i < 5; i++) params.add(like);
        }
        return new JdbcLibrarySupport.QueryParts(where.toString(), params);
    }
}
