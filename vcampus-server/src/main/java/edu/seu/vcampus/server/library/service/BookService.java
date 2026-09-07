package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.BookRepository;
import edu.seu.vcampus.server.security.SessionContext;


/** 图书检索、详情和图书管理员维护服务。 */
public final class BookService extends LibraryServiceSupport {
    private final BookRepository books;

    public BookService(BookRepository books, TransactionManager transactions) {
        super(transactions);
        if (books == null) throw new IllegalArgumentException("books is required");
        this.books = books;
    }

    public PageResult<BookDetail> search(final SessionContext session, final BookSearchRequest request) {
        require(session, Permission.LIBRARY_READ);
        final BookSearchRequest query = request == null ? new BookSearchRequest() : request;
        page(query.getPage(), query.getPageSize());
        return execute(new Work<PageResult<BookDetail>>() { public PageResult<BookDetail> run(java.sql.Connection c) throws Exception { return books.search(c, query); } });
    }

    public BookDetail detail(SessionContext session, final long bookId) {
        require(session, Permission.LIBRARY_READ);
        id(bookId, "图书编号不正确");
        BookDetail found = execute(new Work<BookDetail>() { public BookDetail run(java.sql.Connection c) throws Exception { return books.findById(c, bookId); } });
        if (found == null) throw notFound("图书不存在");
        return found;
    }

    public BookDetail save(final SessionContext session, final BookUpsertRequest request) {
        require(session, Permission.LIBRARY_MANAGE);
        if (request == null) throw invalid("图书请求不能为空");
        return execute(new Work<BookDetail>() { public BookDetail run(java.sql.Connection c) throws Exception { return saveInTransaction(c, request, session.getUserId()); } });
    }

    private BookDetail saveInTransaction(java.sql.Connection c, BookUpsertRequest request,
                                         long operatorId) throws java.sql.SQLException {
        BookDetail old = null;
        if (request.getId() > 0) {
            BookDetail found = books.findByIdForUpdate(c, request.getId());
            if (found == null) throw notFound("图书不存在");
            old = found;
        }
        String title = request.getTitle() == null && old != null ? old.getTitle()
                : text(request.getTitle(), "书名");
        int total = request.getTotalCopies() == null ? old == null ? -1
                : old.getTotalCopies() : request.getTotalCopies().intValue();
        int available = request.getAvailableCopies() == null ? old == null ? -1
                : old.getAvailableCopies() : request.getAvailableCopies().intValue();
        if (total < 0 || available < 0 || available > total
                || old != null && total - available < old.getTotalCopies() - old.getAvailableCopies()) {
            throw new LibraryServiceException(LibraryCommands.INVALID_INVENTORY,
                    "库存数量不符合借阅记录");
        }
        String status = request.getStatus() == null || request.getStatus().trim().isEmpty()
                ? old == null ? "ON_SHELF" : old.getStatus() : request.getStatus().trim();
        if (!"ON_SHELF".equals(status) && !"UNAVAILABLE".equals(status)
                && !"ARCHIVED".equals(status)) throw invalid("图书状态不正确");
        Integer year = request.getPublicationYear() == null && old != null
                ? old.getPublicationYear() : request.getPublicationYear();
        byte[] cover = request.getCoverImage() == null && old != null
                ? old.getCoverImage() : request.getCoverImage();
        if (year != null && (year < 1 || year > 9999)) throw invalid("出版年份应为 1 至 9999");
        if (cover != null && cover.length > 131072) throw invalid("封面图片过大");
        if (cover != null && cover.length > 0) {
            try {
                if (javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(cover)) == null)
                    throw invalid("封面格式不正确");
            } catch (java.io.IOException ex) { throw invalid("封面格式不正确"); }
        }
        BookUpsertRequest normalized = new BookUpsertRequest(request.getId(),
                value(request.getIsbn(), old == null ? null : old.getIsbn()), title,
                value(request.getAuthor(), old == null ? null : old.getAuthor()),
                value(request.getPublisher(), old == null ? null : old.getPublisher()),
                value(request.getCategory(), old == null ? null : old.getCategory()),
                Integer.valueOf(total), Integer.valueOf(available),
                value(request.getLocation(), old == null ? null : old.getLocation()),
                value(request.getDescription(), old == null ? null : old.getDescription()), status, year, cover);
        return books.save(c, normalized, operatorId);
    }

    private static String value(String current, String fallback) {
        return current == null ? fallback : current.trim();
    }

    private static LibraryServiceException invalid(String message) {
        return new LibraryServiceException(ResultCodes.INVALID_INPUT, message);
    }

    private static LibraryServiceException notFound(String message) {
        return new LibraryServiceException(ResultCodes.NOT_FOUND, message);
    }
}
