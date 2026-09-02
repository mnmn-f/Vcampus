package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

final class DemoBookService {
    private final List<BookDetail> books = DemoLibraryData.books();
    private final List<BorrowRecordView> borrowings = DemoLibraryData.borrowings();
    private long nextBorrowId = 203L;

    PageResult<BookDetail> search(BookSearchRequest request) {
        BookSearchRequest query = request == null ? new BookSearchRequest() : request;
        List<BookDetail> result = new ArrayList<BookDetail>();
        for (BookDetail value : books) {
            String searchable = value.getTitle() + " " + value.getAuthor() + " "
                    + value.getIsbn() + " " + value.getCategory();
            if (DemoLibrarySupport.matches(searchable, query.getKeyword())
                    && DemoLibrarySupport.same(query.getCategory(), value.getCategory())
                    && DemoLibrarySupport.same(query.getStatus(), value.getStatus())) {
                result.add(value);
            }
        }
        return DemoLibrarySupport.page(result, query.getPage(), query.getPageSize());
    }

    BookDetail detail(long bookId) throws NetworkClientException {
        return requireBook(bookId);
    }

    BorrowRecordView borrow(BorrowRequest request) throws NetworkClientException {
        if (request == null) throw DemoLibrarySupport.error("请选择要借阅的图书");
        BookDetail book = requireBook(request.getBookId());
        if (!"ON_SHELF".equals(book.getStatus()) || book.getAvailableCopies() < 1) {
            throw DemoLibrarySupport.error("该图书当前不可借阅");
        }
        for (BorrowRecordView current : borrowings) {
            if (current.getBookId() == book.getId() && active(current.getStatus())) {
                throw DemoLibrarySupport.error("你已经借阅了这本书，请先归还");
            }
        }
        replaceBook(withAvailable(book, book.getAvailableCopies() - 1));
        LocalDateTime now = LocalDateTime.now();
        BorrowRecordView record = new BorrowRecordView(nextBorrowId++, book.getId(),
                book.getTitle(), 1L, "演示学生", now, now.plusDays(30), null,
                "BORROWED", 0, "Demo 模式借阅");
        borrowings.add(0, record);
        return record;
    }

    BorrowRecordView returnBook(long recordId) throws NetworkClientException {
        for (int i = 0; i < borrowings.size(); i++) {
            BorrowRecordView old = borrowings.get(i);
            if (old.getId() != recordId) continue;
            if (!active(old.getStatus())) throw DemoLibrarySupport.error("该借阅记录已经归还");
            BorrowRecordView returned = copyBorrow(old, "RETURNED", LocalDateTime.now());
            borrowings.set(i, returned);
            BookDetail book = findBook(old.getBookId());
            if (book != null) replaceBook(withAvailable(book,
                    Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1)));
            return returned;
        }
        throw DemoLibrarySupport.error("未找到借阅记录");
    }

    PageResult<BorrowRecordView> myBorrowings(BorrowSearchRequest request) {
        BorrowSearchRequest query = request == null ? new BorrowSearchRequest() : request;
        return DemoLibrarySupport.page(filter(query.getStatus(), null, null), query.getPage(),
                query.getPageSize());
    }

    PageResult<BorrowRecordView> adminBorrowings(BorrowAdminSearchRequest request) {
        BorrowAdminSearchRequest query = request == null
                ? new BorrowAdminSearchRequest() : request;
        return DemoLibrarySupport.page(filter(query.getStatus(), query.getStudentId(),
                query.getKeyword()), query.getPage(), query.getPageSize());
    }

    BookDetail save(BookUpsertRequest request) throws NetworkClientException {
        if (request == null) throw DemoLibrarySupport.error("图书信息不能为空");
        String title = DemoLibrarySupport.required(request.getTitle(), "书名不能为空");
        int total = DemoLibrarySupport.number(request.getTotalCopies());
        int available = DemoLibrarySupport.number(request.getAvailableCopies());
        if (total < 0 || available < 0 || available > total) {
            throw DemoLibrarySupport.error("库存数量不正确");
        }
        long id = request.getId() > 0L ? request.getId() : nextBookId();
        BookDetail old = findBook(id);
        LocalDateTime now = LocalDateTime.now();
        BookDetail value = new BookDetail(id, request.getIsbn(), title, request.getAuthor(),
                request.getPublisher(), request.getCategory(), total, available,
                request.getLocation(), request.getDescription(),
                DemoLibrarySupport.text(request.getStatus(), "ON_SHELF"),
                old == null ? now : old.getCreatedAt(), now);
        replaceBook(value);
        return value;
    }

    private List<BorrowRecordView> filter(String status, Long student, String keyword) {
        List<BorrowRecordView> result = new ArrayList<BorrowRecordView>();
        for (BorrowRecordView value : borrowings) {
            if (DemoLibrarySupport.same(status, value.getStatus())
                    && (student == null || student.longValue() == value.getBorrowerUserId())
                    && DemoLibrarySupport.matches(value.getBookTitle() + " "
                    + value.getBorrowerName() + " " + value.getBorrowerUserId(), keyword)) {
                result.add(value);
            }
        }
        return result;
    }

    private BookDetail requireBook(long id) throws NetworkClientException {
        BookDetail value = findBook(id);
        if (value == null) throw DemoLibrarySupport.error("未找到图书");
        return value;
    }

    private BookDetail findBook(long id) {
        for (BookDetail value : books) if (value.getId() == id) return value;
        return null;
    }

    private void replaceBook(BookDetail value) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId() == value.getId()) { books.set(i, value); return; }
        }
        books.add(value);
    }

    private long nextBookId() {
        long id = 1L;
        for (BookDetail value : books) id = Math.max(id, value.getId() + 1L);
        return id;
    }

    private static boolean active(String status) {
        return "BORROWED".equals(status) || "OVERDUE".equals(status);
    }

    private static BookDetail withAvailable(BookDetail book, int available) {
        return new BookDetail(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getCategory(), book.getTotalCopies(), available,
                book.getLocation(), book.getDescription(), book.getStatus(), book.getCreatedAt(),
                LocalDateTime.now());
    }

    private static BorrowRecordView copyBorrow(BorrowRecordView value, String status,
                                                LocalDateTime returned) {
        return new BorrowRecordView(value.getId(), value.getBookId(), value.getBookTitle(),
                value.getBorrowerUserId(), value.getBorrowerName(), value.getIssuedAt(),
                value.getDueAt(), returned, status, value.getRenewCount(), value.getRemark());
    }
}
