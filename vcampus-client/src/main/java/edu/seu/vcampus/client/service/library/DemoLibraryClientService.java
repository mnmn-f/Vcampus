package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Demo 模式的可操作图书馆服务；数据只保存在本次客户端进程中。 */
public final class DemoLibraryClientService implements LibraryClientService {
    private final List<BookDetail> books = DemoLibraryData.books();
    private final List<BorrowRecordView> borrowings = DemoLibraryData.borrowings();
    private final List<StudyRoomView> rooms = DemoLibraryData.rooms();
    private final List<StudyRoomReservationView> reservations = DemoLibraryData.reservations();
    private final List<OnlineResourceView> resources = DemoLibraryData.resources();
    private final List<OnlineResourceAccessLogDto> accessLogs =
            new ArrayList<OnlineResourceAccessLogDto>();
    private long nextBorrowId = 203L;
    private long nextReservationId = 402L;
    private long nextAccessLogId = 1L;

    @Override
    public synchronized PageResult<BookDetail> searchBooks(BookSearchRequest request) {
        BookSearchRequest query = request == null ? new BookSearchRequest() : request;
        List<BookDetail> result = new ArrayList<BookDetail>();
        for (BookDetail value : books) {
            String searchable = value.getTitle() + " " + value.getAuthor() + " "
                    + value.getIsbn() + " " + value.getCategory();
            if (matches(searchable, query.getKeyword())
                    && same(query.getCategory(), value.getCategory())
                    && same(query.getStatus(), value.getStatus())) result.add(value);
        }
        return page(result, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized BookDetail bookDetail(long bookId) throws NetworkClientException {
        return requireBook(bookId);
    }

    @Override
    public synchronized BorrowRecordView borrow(BorrowRequest request)
            throws NetworkClientException {
        if (request == null) throw error("请选择要借阅的图书");
        BookDetail book = requireBook(request.getBookId());
        if (!"ON_SHELF".equals(book.getStatus()) || book.getAvailableCopies() < 1) {
            throw error("该图书当前不可借阅");
        }
        for (BorrowRecordView current : borrowings) {
            if (current.getBookId() == book.getId() && active(current.getStatus())) {
                throw error("你已经借阅了这本书，请先归还");
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

    @Override
    public synchronized BorrowRecordView returnBook(long recordId)
            throws NetworkClientException {
        for (int i = 0; i < borrowings.size(); i++) {
            BorrowRecordView old = borrowings.get(i);
            if (old.getId() != recordId) continue;
            if (!active(old.getStatus())) throw error("该借阅记录已经归还");
            BorrowRecordView returned = copyBorrow(old, "RETURNED", LocalDateTime.now());
            borrowings.set(i, returned);
            BookDetail book = findBook(old.getBookId());
            if (book != null) replaceBook(withAvailable(book,
                    Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1)));
            return returned;
        }
        throw error("未找到借阅记录");
    }

    @Override
    public synchronized PageResult<BorrowRecordView> myBorrowings(BorrowSearchRequest request) {
        BorrowSearchRequest query = request == null ? new BorrowSearchRequest() : request;
        return page(filterBorrowings(query.getStatus(), null, null), query.getPage(),
                query.getPageSize());
    }

    @Override
    public synchronized PageResult<BorrowRecordView> adminBorrowings(
            BorrowAdminSearchRequest request) {
        BorrowAdminSearchRequest query = request == null
                ? new BorrowAdminSearchRequest() : request;
        return page(filterBorrowings(query.getStatus(), query.getStudentId(),
                query.getKeyword()), query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized BookDetail saveBook(BookUpsertRequest request)
            throws NetworkClientException {
        if (request == null) throw error("图书信息不能为空");
        String title = required(request.getTitle(), "书名不能为空");
        int total = number(request.getTotalCopies());
        int available = number(request.getAvailableCopies());
        if (total < 0 || available < 0 || available > total) throw error("库存数量不正确");
        long id = request.getId() > 0L ? request.getId() : nextBookId();
        BookDetail old = findBook(id);
        LocalDateTime now = LocalDateTime.now();
        BookDetail value = new BookDetail(id, request.getIsbn(), title,
                request.getAuthor(), request.getPublisher(), request.getCategory(), total,
                available, request.getLocation(), request.getDescription(),
                text(request.getStatus(), "ON_SHELF"), old == null ? now : old.getCreatedAt(), now);
        replaceBook(value);
        return value;
    }

    @Override
    public synchronized PageResult<StudyRoomView> searchStudyRooms(
            StudyRoomSearchRequest request) {
        StudyRoomSearchRequest query = request == null
                ? new StudyRoomSearchRequest() : request;
        List<StudyRoomView> result = new ArrayList<StudyRoomView>();
        for (StudyRoomView value : rooms) {
            boolean capacity = query.getMinCapacity() == null
                    || value.getCapacity() >= query.getMinCapacity().intValue();
            if (capacity && matches(value.getBuildingName() + " " + value.getRoomNo(),
                    query.getKeyword()) && same(query.getStatus(), value.getStatus())) {
                result.add(value);
            }
        }
        return page(result, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized StudyRoomReservationView reserveStudyRoom(
            StudyRoomReservationRequest request) throws NetworkClientException {
        if (request == null || request.getStartAt() == null || request.getEndAt() == null) {
            throw error("预约信息不能为空");
        }
        StudyRoomView room = requireRoom(request.getRoomId());
        if (!"OPEN".equals(room.getStatus())) throw error("自习室当前不可预约");
        if (!request.getEndAt().isAfter(request.getStartAt())
                || request.getStartAt().isBefore(LocalDateTime.now())
                || !request.getStartAt().toLocalDate().equals(request.getEndAt().toLocalDate())) {
            throw error("预约时间不正确");
        }
        if (request.getStartAt().toLocalTime().isBefore(room.getOpenTime())
                || request.getEndAt().toLocalTime().isAfter(room.getCloseTime())) {
            throw error("预约时间不在开放时段内");
        }
        for (StudyRoomReservationView current : reservations) {
            if (!"RESERVED".equals(current.getStatus())) continue;
            if (overlaps(current, request)) {
                throw error(current.getRoomId() == room.getId()
                        ? "该自习室时段已被预约" : "你在该时段已有其他预约");
            }
        }
        StudyRoomReservationView value = new StudyRoomReservationView(nextReservationId++,
                room.getId(), room.getBuildingName() + " " + room.getRoomNo(), 1L,
                request.getStartAt(), request.getEndAt(), "RESERVED", null);
        reservations.add(0, value);
        return value;
    }

    @Override
    public synchronized StudyRoomReservationView cancelReservation(long id)
            throws NetworkClientException {
        for (int i = 0; i < reservations.size(); i++) {
            StudyRoomReservationView old = reservations.get(i);
            if (old.getId() != id) continue;
            if (!"RESERVED".equals(old.getStatus())) throw error("该预约已经取消");
            StudyRoomReservationView value = new StudyRoomReservationView(old.getId(),
                    old.getRoomId(), old.getRoomName(), old.getUserId(), old.getStartAt(),
                    old.getEndAt(), "CANCELLED", LocalDateTime.now());
            reservations.set(i, value);
            return value;
        }
        throw error("未找到预约记录");
    }

    @Override
    public synchronized PageResult<StudyRoomReservationView> reservations(
            StudyRoomReservationSearchRequest request) {
        StudyRoomReservationSearchRequest query = request == null
                ? new StudyRoomReservationSearchRequest() : request;
        List<StudyRoomReservationView> result = new ArrayList<StudyRoomReservationView>();
        for (StudyRoomReservationView value : reservations) {
            if (same(query.getStatus(), value.getStatus())) result.add(value);
        }
        return page(result, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized StudyRoomView saveStudyRoom(StudyRoomUpsertRequest request)
            throws NetworkClientException {
        if (request == null) throw error("自习室信息不能为空");
        required(request.getBuildingName(), "楼栋不能为空");
        required(request.getRoomNo(), "房间号不能为空");
        if (request.getCapacity() <= 0 || request.getOpenTime() == null
                || request.getCloseTime() == null
                || !request.getCloseTime().isAfter(request.getOpenTime())) {
            throw error("容量或开放时段不正确");
        }
        long id = request.getId() > 0L ? request.getId() : nextRoomId();
        StudyRoomView value = new StudyRoomView(id, request.getBuildingName(),
                request.getRoomNo(), request.getCapacity(), request.getOpenTime(),
                request.getCloseTime(), text(request.getStatus(), "OPEN"),
                request.getDescription());
        replaceRoom(value);
        return value;
    }

    @Override
    public synchronized PageResult<OnlineResourceView> searchResources(
            OnlineResourceSearchRequest request) {
        OnlineResourceSearchRequest query = request == null
                ? new OnlineResourceSearchRequest() : request;
        List<OnlineResourceView> result = new ArrayList<OnlineResourceView>();
        for (OnlineResourceView value : resources) {
            if (matches(value.getTitle() + " " + value.getResourceType() + " "
                    + value.getDescription(), query.getKeyword())
                    && same(query.getResourceType(), value.getResourceType())
                    && same(query.getStatus(), value.getStatus())) result.add(value);
        }
        return page(result, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized OnlineResourceView saveResource(OnlineResourceUpsertRequest request)
            throws NetworkClientException {
        if (request == null) throw error("资源信息不能为空");
        required(request.getTitle(), "资源名称不能为空");
        required(request.getResourceType(), "资源类型不能为空");
        required(request.getUrl(), "资源地址不能为空");
        long id = request.getId() > 0L ? request.getId() : nextResourceId();
        OnlineResourceView old = findResource(id);
        LocalDateTime published = "ACTIVE".equals(request.getStatus())
                ? old == null || old.getPublishedAt() == null
                ? LocalDateTime.now() : old.getPublishedAt()
                : old == null ? null : old.getPublishedAt();
        OnlineResourceView value = new OnlineResourceView(id, request.getTitle(),
                request.getResourceType(), request.getUrl(), request.getDescription(), 5L,
                text(request.getStatus(), "ACTIVE"), published);
        replaceResource(value);
        return value;
    }

    @Override
    public synchronized OnlineResourceView accessResource(long resourceId)
            throws NetworkClientException {
        OnlineResourceView value = findResource(resourceId);
        if (value == null) throw error("未找到线上资源");
        if (!"ACTIVE".equals(value.getStatus())) throw error("该线上资源当前已停用");
        accessLogs.add(0, new OnlineResourceAccessLogDto(nextAccessLogId++, value.getId(),
                1L, value.getTitle(), "demo_student", "演示学生", LocalDateTime.now()));
        return value;
    }

    @Override
    public synchronized OnlineResourceAccessLogPage searchResourceAccessLogs(
            OnlineResourceAccessLogQuery request) {
        OnlineResourceAccessLogQuery query = request == null
                ? new OnlineResourceAccessLogQuery() : request;
        List<OnlineResourceAccessLogDto> result = new ArrayList<OnlineResourceAccessLogDto>();
        for (OnlineResourceAccessLogDto value : accessLogs) {
            if (query.getResourceId() != null
                    && query.getResourceId().longValue() != value.getResourceId()) continue;
            if (query.getUserId() != null
                    && query.getUserId().longValue() != value.getUserId()) continue;
            if (query.getFrom() != null && value.getAccessedAt().isBefore(query.getFrom())) continue;
            if (query.getTo() != null && value.getAccessedAt().isAfter(query.getTo())) continue;
            result.add(value);
        }
        int from = Math.min((query.getPage() - 1) * query.getPageSize(), result.size());
        int to = Math.min(from + query.getPageSize(), result.size());
        return new OnlineResourceAccessLogPage(
                new ArrayList<OnlineResourceAccessLogDto>(result.subList(from, to)),
                query.getPage(), query.getPageSize(), result.size());
    }

    private List<BorrowRecordView> filterBorrowings(String status, Long student,
                                                     String keyword) {
        List<BorrowRecordView> result = new ArrayList<BorrowRecordView>();
        for (BorrowRecordView value : borrowings) {
            if (same(status, value.getStatus())
                    && (student == null || student.longValue() == value.getBorrowerUserId())
                    && matches(value.getBookTitle() + " " + value.getBorrowerName() + " "
                    + value.getBorrowerUserId(), keyword)) result.add(value);
        }
        return result;
    }

    private static boolean overlaps(StudyRoomReservationView current,
                                    StudyRoomReservationRequest request) {
        return current.getStartAt().isBefore(request.getEndAt())
                && current.getEndAt().isAfter(request.getStartAt());
    }

    private BookDetail requireBook(long id) throws NetworkClientException {
        BookDetail value = findBook(id);
        if (value == null) throw error("未找到图书");
        return value;
    }

    private BookDetail findBook(long id) {
        for (BookDetail value : books) if (value.getId() == id) return value;
        return null;
    }

    private StudyRoomView requireRoom(long id) throws NetworkClientException {
        for (StudyRoomView value : rooms) if (value.getId() == id) return value;
        throw error("未找到自习室");
    }

    private OnlineResourceView findResource(long id) {
        for (OnlineResourceView value : resources) if (value.getId() == id) return value;
        return null;
    }

    private void replaceBook(BookDetail value) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId() == value.getId()) { books.set(i, value); return; }
        }
        books.add(value);
    }

    private void replaceRoom(StudyRoomView value) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getId() == value.getId()) { rooms.set(i, value); return; }
        }
        rooms.add(value);
    }

    private void replaceResource(OnlineResourceView value) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == value.getId()) { resources.set(i, value); return; }
        }
        resources.add(value);
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

    private long nextBookId() {
        long id = 1L;
        for (BookDetail value : books) id = Math.max(id, value.getId() + 1L);
        return id;
    }

    private long nextRoomId() {
        long id = 1L;
        for (StudyRoomView value : rooms) id = Math.max(id, value.getId() + 1L);
        return id;
    }

    private long nextResourceId() {
        long id = 1L;
        for (OnlineResourceView value : resources) id = Math.max(id, value.getId() + 1L);
        return id;
    }

    private static int number(Integer value) { return value == null ? 0 : value.intValue(); }

    private static String required(String value, String message)
            throws NetworkClientException {
        if (value == null || value.trim().length() == 0) throw error(message);
        return value.trim();
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static boolean active(String status) {
        return "BORROWED".equals(status) || "OVERDUE".equals(status);
    }

    private static boolean same(String expected, String actual) {
        return expected == null || expected.trim().length() == 0
                || expected.equalsIgnoreCase(actual);
    }

    private static boolean matches(String source, String keyword) {
        return keyword == null || keyword.trim().length() == 0 || source != null
                && source.toLowerCase(Locale.ROOT).contains(
                keyword.trim().toLowerCase(Locale.ROOT));
    }

    private static NetworkClientException error(String message) {
        return new NetworkClientException("DEMO.LIBRARY", message);
    }

    private static <T> PageResult<T> page(List<T> values, int page, int pageSize) {
        int p = page < 1 ? 1 : page;
        int size = pageSize < 1 ? 20 : pageSize;
        int from = Math.min((p - 1) * size, values.size());
        int to = Math.min(from + size, values.size());
        return new PageResult<T>(new ArrayList<T>(values.subList(from, to)), p, size,
                values.size());
    }
}
