package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import java.sql.SQLException;
import org.threeten.bp.LocalDateTime;

/** 借阅记录持久化边界。 */
public interface BorrowRepository {
    PageResult<BorrowRecordView> search(Connection connection,
                                        BorrowSearchRequest request,
                                        Long borrowerUserId) throws SQLException;

    /** 管理员台账查询；实现可复用全量 owner=null 查询边界。 */
    PageResult<BorrowRecordView> searchAdmin(Connection connection,
                                             BorrowAdminSearchRequest request)
            throws SQLException;

    BorrowRecordView findById(Connection connection, long recordId)
            throws SQLException;

    BorrowRecordView findByIdForUpdate(Connection connection, long recordId)
            throws SQLException;

    BorrowRecordView findActiveByBookAndUserForUpdate(Connection connection,
                                                                 long bookId,
                                                                 long userId)
            throws SQLException;

    BorrowRecordView insert(Connection connection, long bookId, long userId,
                            LocalDateTime issuedAt, LocalDateTime dueAt,
                            long handledBy, String remark) throws SQLException;

    boolean markReturned(Connection connection, long recordId, long handledBy,
                         LocalDateTime returnedAt, String remark) throws SQLException;
}
