package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.BookRepository;
import edu.seu.vcampus.server.library.repository.BorrowRepository;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;

/** 学生借还和本人借阅记录服务；用户编号永远取自 SessionContext。 */
public final class BorrowService extends LibraryServiceSupport {
    private static final int BORROW_DAYS = 30;
    private final BookRepository books;
    private final BorrowRepository records;

    public BorrowService(BookRepository books, BorrowRepository records,
                         TransactionManager transactions) {
        super(transactions);
        if (books == null || records == null) throw new IllegalArgumentException("repositories required");
        this.books = books;
        this.records = records;
    }

    public PageResult<BorrowRecordView> mine(final SessionContext session,
                                             final BorrowSearchRequest request) {
        require(session, Permission.LIBRARY_BORROW);
        final BorrowSearchRequest query = request == null ? new BorrowSearchRequest() : request;
        page(query.getPage(), query.getPageSize());
        return execute(new Work<PageResult<BorrowRecordView>>() { public PageResult<BorrowRecordView> run(java.sql.Connection c) throws Exception { return records.search(c, query, Long.valueOf(session.getUserId())); } });
    }

    public PageResult<BorrowRecordView> adminList(final SessionContext session,
                                                  final BorrowAdminSearchRequest request) {
        require(session, Permission.LIBRARY_MANAGE);
        final BorrowAdminSearchRequest query = request == null
                ? new BorrowAdminSearchRequest() : request;
        if (query.getStudentId() != null) id(query.getStudentId().longValue(), "学生编号不正确");
        page(query.getPage(), query.getPageSize());
        return execute(new Work<PageResult<BorrowRecordView>>() { public PageResult<BorrowRecordView> run(java.sql.Connection c) throws Exception { return records.searchAdmin(c, query); } });
    }

    public BorrowRecordView borrow(final SessionContext session, final BorrowRequest request) {
        require(session, Permission.LIBRARY_BORROW);
        if (request == null) throw invalid("借书请求不能为空");
        id(request.getBookId(), "图书编号不正确");
        return execute(new Work<BorrowRecordView>() { public BorrowRecordView run(java.sql.Connection c) throws Exception { return borrowInTransaction(c, request.getBookId(), session.getUserId()); } });
    }

    private BorrowRecordView borrowInTransaction(java.sql.Connection c, long bookId, long userId)
            throws java.sql.SQLException {
        BookDetail book = books.findByIdForUpdate(c, bookId);
        if (book == null) throw notFound("图书不存在");
        if (!"ON_SHELF".equals(book.getStatus()) || book.getAvailableCopies() <= 0) {
            throw new LibraryServiceException(LibraryCommands.BOOK_UNAVAILABLE, "当前图书暂无可借库存");
        }
        if (records.findActiveByBookAndUserForUpdate(c, bookId, userId) != null) {
            throw new LibraryServiceException(LibraryCommands.ALREADY_BORROWED, "你已经借阅了这本书");
        }
        LocalDateTime issuedAt = LocalDateTime.now();
        BorrowRecordView result = records.insert(c, bookId, userId, issuedAt,
                issuedAt.plusDays(BORROW_DAYS), userId, null);
        if (!books.decrementAvailable(c, bookId)) {
            throw new LibraryServiceException(LibraryCommands.BOOK_UNAVAILABLE, "库存已被其他借阅占用");
        }
        return result;
    }

    public BorrowRecordView returnBook(final SessionContext session, final long recordId) {
        require(session, Permission.LIBRARY_BORROW);
        id(recordId, "借阅记录编号不正确");
        return execute(new Work<BorrowRecordView>() { public BorrowRecordView run(java.sql.Connection c) throws Exception { return returnInTransaction(c, recordId, session.getUserId()); } });
    }

    private BorrowRecordView returnInTransaction(java.sql.Connection c, long recordId, long userId)
            throws java.sql.SQLException {
        BorrowRecordView before = records.findById(c, recordId);
        if (before == null) throw notFound("借阅记录不存在");
        if (before.getBorrowerUserId() != userId) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "不能归还他人的借阅记录");
        }
        BookDetail book = books.findByIdForUpdate(c, before.getBookId());
        if (book == null) throw notFound("关联图书不存在");
        BorrowRecordView locked = records.findByIdForUpdate(c, recordId);
        if (locked == null) throw notFound("借阅记录不存在");
        if (locked.getBorrowerUserId() != userId) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "不能归还他人的借阅记录");
        }
        if (!"BORROWED".equals(locked.getStatus())
                && !"OVERDUE".equals(locked.getStatus())) {
            throw new LibraryServiceException(LibraryCommands.BORROW_NOT_ACTIVE, "该借阅记录已经归还");
        }
        LocalDateTime returnedAt = LocalDateTime.now();
        if (!records.markReturned(c, recordId, userId, returnedAt, locked.getRemark())
                || !books.incrementAvailable(c, book.getId())) {
            throw new LibraryServiceException(ResultCodes.CONFLICT, "借阅库存状态已发生变化，请重试");
        }
        return new BorrowRecordView(locked.getId(), locked.getBookId(),
                locked.getBookTitle(), locked.getBorrowerUserId(),
                locked.getBorrowerName(), locked.getIssuedAt(),
                locked.getDueAt(), returnedAt, "RETURNED",
                locked.getRenewCount(), locked.getRemark());
    }

    private static LibraryServiceException invalid(String message) {
        return new LibraryServiceException(ResultCodes.INVALID_INPUT, message);
    }

    private static LibraryServiceException notFound(String message) {
        return new LibraryServiceException(ResultCodes.NOT_FOUND, message);
    }
}
