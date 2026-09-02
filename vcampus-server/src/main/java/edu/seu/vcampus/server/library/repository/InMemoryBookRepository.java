package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 无磁盘测试仓储；生产环境使用 MySqlBookRepository。 */
public final class InMemoryBookRepository implements BookRepository {
    private final Map<Long, BookDetail> books = new LinkedHashMap<Long, BookDetail>();
    private long nextId = 1L;

    public synchronized void seed(BookDetail book) {
        if (book == null || book.getId() <= 0) throw new IllegalArgumentException("book");
        books.put(book.getId(), book);
        nextId = Math.max(nextId, book.getId() + 1L);
    }

    @Override
    public synchronized PageResult<BookDetail> search(Connection c, BookSearchRequest r) {
        List<BookDetail> found = new ArrayList<BookDetail>();
        String keyword = InMemoryLibrarySupport.lower(r.getKeyword());
        String category = InMemoryLibrarySupport.lower(r.getCategory());
        String status = InMemoryLibrarySupport.lower(r.getStatus());
        for (BookDetail b : books.values()) {
            String text = InMemoryLibrarySupport.lower(b.getTitle() + " "
                    + InMemoryLibrarySupport.safe(b.getAuthor()) + " "
                    + InMemoryLibrarySupport.safe(b.getIsbn()) + " "
                    + InMemoryLibrarySupport.safe(b.getCategory()));
            if (keyword != null && !text.contains(keyword)) continue;
            if (category != null && !category.equals(InMemoryLibrarySupport.lower(b.getCategory()))) continue;
            if (status != null && !status.equals(InMemoryLibrarySupport.lower(b.getStatus()))) continue;
            found.add(b);
        }
        return InMemoryLibrarySupport.page(found, r.getPage(), r.getPageSize());
    }

    @Override
    public synchronized BookDetail findById(Connection c, long id) {
        return books.get(id);
    }

    @Override
    public synchronized BookDetail findByIdForUpdate(Connection c, long id) {
        return findById(c, id);
    }

    @Override
    public synchronized BookDetail save(Connection c, BookUpsertRequest r, long operatorId) {
        long id = r.getId() <= 0 ? nextId++ : r.getId();
        BookDetail old = books.get(id);
        LocalDateTime now = LocalDateTime.now();
        BookDetail value = new BookDetail(id, r.getIsbn(), r.getTitle(), r.getAuthor(),
                r.getPublisher(), r.getCategory(), r.getTotalCopies().intValue(),
                r.getAvailableCopies().intValue(), r.getLocation(), r.getDescription(),
                r.getStatus(), old == null ? now : old.getCreatedAt(), now);
        books.put(id, value);
        return value;
    }

    @Override
    public synchronized boolean decrementAvailable(Connection c, long id) {
        BookDetail b = books.get(id);
        if (b == null || b.getAvailableCopies() <= 0 || !"ON_SHELF".equals(b.getStatus())) return false;
        books.put(id, copy(b, b.getAvailableCopies() - 1));
        return true;
    }

    @Override
    public synchronized boolean incrementAvailable(Connection c, long id) {
        BookDetail b = books.get(id);
        if (b == null || b.getAvailableCopies() >= b.getTotalCopies()) return false;
        books.put(id, copy(b, b.getAvailableCopies() + 1));
        return true;
    }

    private static BookDetail copy(BookDetail b, int available) {
        return new BookDetail(b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublisher(),
                b.getCategory(), b.getTotalCopies(), available, b.getLocation(), b.getDescription(),
                b.getStatus(), b.getCreatedAt(), LocalDateTime.now());
    }

}
