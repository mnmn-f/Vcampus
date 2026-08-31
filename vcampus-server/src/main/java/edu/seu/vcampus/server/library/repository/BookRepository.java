package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import java.sql.SQLException;

/** 图书持久化边界；连接由业务事务传入，仓储不提交事务。 */
public interface BookRepository {
    PageResult<BookDetail> search(Connection connection, BookSearchRequest request)
            throws SQLException;

    BookDetail findById(Connection connection, long bookId)
            throws SQLException;

    BookDetail findByIdForUpdate(Connection connection, long bookId)
            throws SQLException;

    BookDetail save(Connection connection, BookUpsertRequest request, long operatorId)
            throws SQLException;

    boolean decrementAvailable(Connection connection, long bookId) throws SQLException;

    boolean incrementAvailable(Connection connection, long bookId) throws SQLException;
}
