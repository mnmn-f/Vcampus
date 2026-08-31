package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 无磁盘借阅仓储，用于业务规则和权限测试。 */
public final class InMemoryBorrowRepository implements BorrowRepository {
    private final Map<Long, BorrowRecordView> records = new LinkedHashMap<Long, BorrowRecordView>();
    private long nextId = 1L;

    @Override
    public synchronized PageResult<BorrowRecordView> search(Connection c, BorrowSearchRequest r,
                                                             Long owner) {
        List<BorrowRecordView> found = new ArrayList<BorrowRecordView>();
        for (BorrowRecordView row : records.values()) {
            if (owner != null && row.getBorrowerUserId() != owner.longValue()) continue;
            String status = effective(row);
            if (r.getStatus() != null && !r.getStatus().trim().isEmpty()
                    && !r.getStatus().trim().equalsIgnoreCase(status)) continue;
            found.add(row);
        }
        return InMemoryLibrarySupport.page(found, r.getPage(), r.getPageSize());
    }

    @Override
    public synchronized PageResult<BorrowRecordView> searchAdmin(
            Connection c, BorrowAdminSearchRequest request) {
        BorrowAdminSearchRequest query = request == null
                ? new BorrowAdminSearchRequest() : request;
        List<BorrowRecordView> found = new ArrayList<BorrowRecordView>();
        String keyword = query.getKeyword() == null ? null
                : query.getKeyword().toLowerCase(java.util.Locale.ROOT);
        for (BorrowRecordView row : records.values()) {
            if (query.getStudentId() != null
                    && row.getBorrowerUserId() != query.getStudentId().longValue()) continue;
            String status = effective(row);
            if (query.getStatus() != null && !query.getStatus().equalsIgnoreCase(status)) continue;
            if (keyword != null && !matches(row, keyword)) continue;
            found.add(row);
        }
        Collections.sort(found, new Comparator<BorrowRecordView>() {
            @Override public int compare(BorrowRecordView left, BorrowRecordView right) {
                LocalDateTime a = left.getIssuedAt(), b = right.getIssuedAt();
                if (a == null && b != null) return 1;
                if (a != null && b == null) return -1;
                if (a != null && b != null) {
                    int time = b.compareTo(a); if (time != 0) return time;
                }
                return Long.compare(right.getId(), left.getId());
            }
        });
        return InMemoryLibrarySupport.page(found, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized BorrowRecordView findById(Connection c, long id) {
        return records.get(id);
    }

    @Override
    public synchronized BorrowRecordView findByIdForUpdate(Connection c, long id) {
        return findById(c, id);
    }

    @Override
    public synchronized BorrowRecordView findActiveByBookAndUserForUpdate(
            Connection c, long bookId, long userId) {
        for (BorrowRecordView row : records.values()) {
            if (row.getBookId() == bookId && row.getBorrowerUserId() == userId
                    && active(row.getStatus())) return row;
        }
        return null;
    }

    @Override
    public synchronized BorrowRecordView insert(Connection c, long bookId, long userId,
                                                LocalDateTime issuedAt, LocalDateTime dueAt,
                                                long handledBy, String remark) {
        BorrowRecordView row = new BorrowRecordView(nextId++, bookId, null, userId, null,
                issuedAt, dueAt, null, "BORROWED", 0, remark);
        records.put(row.getId(), row);
        return row;
    }

    @Override
    public synchronized boolean markReturned(Connection c, long id, long handledBy,
                                              LocalDateTime returnedAt, String remark) {
        BorrowRecordView old = records.get(id);
        if (old == null || !active(old.getStatus())) return false;
        records.put(id, new BorrowRecordView(old.getId(), old.getBookId(), old.getBookTitle(),
                old.getBorrowerUserId(), old.getBorrowerName(), old.getIssuedAt(), old.getDueAt(),
                returnedAt, "RETURNED", old.getRenewCount(), remark));
        return true;
    }

    public synchronized int size() { return records.size(); }

    public synchronized void seed(BorrowRecordView value) {
        if (value == null || value.getId() <= 0L) throw new IllegalArgumentException("borrow record required");
        records.put(value.getId(), value);
        nextId = Math.max(nextId, value.getId() + 1L);
    }

    private static boolean active(String status) {
        return "BORROWED".equals(status) || "OVERDUE".equals(status);
    }

    private static String effective(BorrowRecordView row) {
        return "BORROWED".equals(row.getStatus()) && row.getDueAt() != null
                && row.getDueAt().isBefore(LocalDateTime.now()) ? "OVERDUE" : row.getStatus();
    }

    private static boolean matches(BorrowRecordView row, String keyword) {
        return contains(row.getBookTitle(), keyword) || contains(row.getBorrowerName(), keyword)
                || String.valueOf(row.getBorrowerUserId()).contains(keyword)
                || contains(row.getRemark(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(keyword);
    }

}
