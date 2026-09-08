package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.repository.InMemoryBorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomReservationRepository;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.library.service.LibraryServiceException;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 图书馆核心库存、冲突和职责权限的无数据库测试。 */
public final class LibraryServiceTest {
    private InMemoryBookRepository books;
    private InMemoryBorrowRepository borrows;
    private InMemoryStudyRoomRepository rooms;
    private InMemoryStudyRoomReservationRepository reservations;
    private InMemoryOnlineResourceRepository resources;
    private InMemoryOnlineResourceAccessLogRepository accessLogs;
    private LibraryService service;
    private SessionContext student;
    private SessionContext otherStudent;
    private SessionContext librarian;

    @Before
    public void setUp() {
        books = new InMemoryBookRepository();
        books.seed(new BookDetail(1L, "I-1", "Java", "A", "P", "编程", 2, 2,
                "A-1", null, "ON_SHELF", LocalDateTime.now(), LocalDateTime.now()));
        borrows = new InMemoryBorrowRepository();
        rooms = new InMemoryStudyRoomRepository();
        rooms.seed(new StudyRoomView(1L, "图书馆", "A", 8, LocalTime.of(8, 0),
                LocalTime.of(22, 0), "OPEN", null));
        reservations = new InMemoryStudyRoomReservationRepository();
        resources = new InMemoryOnlineResourceRepository();
        resources.seed(new OnlineResourceView(1L, "开放资源", "DOCUMENTATION",
                "https://example.com/open", "开放资源", 20L, "ACTIVE", LocalDateTime.now()));
        resources.seed(new OnlineResourceView(2L, "停用资源", "DOCUMENTATION",
                "https://example.com/closed", "停用资源", 20L, "INACTIVE", null));
        accessLogs = new InMemoryOnlineResourceAccessLogRepository();
        service = new LibraryService(books, borrows, rooms, reservations,
                resources, accessLogs, null);
        student = session(10L, Role.STUDENT);
        otherStudent = session(11L, Role.STUDENT);
        librarian = session(20L, Role.LIBRARIAN);
    }

    @Test
    public void borrowDecrementsStockAndDuplicateBorrowIsRejected() {
        final BorrowRecordView record = service.borrow(student, new BorrowRequest(1L));
        assertEquals(1, books.findById(null, 1L).getAvailableCopies());
        assertEquals(10L, record.getBorrowerUserId());
        assertCode(LibraryCommands.ALREADY_BORROWED,
                new Runnable() { public void run() { service.borrow(student, new BorrowRequest(1L)); } });
    }

    @Test
    public void returnIsIdempotentlyRejectedAfterFirstReturn() {
        final BorrowRecordView record = service.borrow(student, new BorrowRequest(1L));
        BorrowRecordView returned = service.returnBook(student, record.getId());
        assertEquals("RETURNED", returned.getStatus());
        assertEquals(2, books.findById(null, 1L).getAvailableCopies());
        assertCode(LibraryCommands.BORROW_NOT_ACTIVE,
                new Runnable() { public void run() { service.returnBook(student, record.getId()); } });
    }

    @Test
    public void reservationChecksRoomOverlapAndOwnOverlap() {
        final LocalDate date = LocalDate.now().plusDays(1);
        StudyRoomReservationRequest first = new StudyRoomReservationRequest(1L,
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(11, 0)));
        service.reserveStudyRoom(student, first);
        assertCode(LibraryCommands.RESERVATION_DUPLICATE,
                new Runnable() { public void run() { service.reserveStudyRoom(student, new StudyRoomReservationRequest(1L,
                        LocalDateTime.of(date, LocalTime.of(10, 0)),
                        LocalDateTime.of(date, LocalTime.of(12, 0)))); } });
        assertCode(LibraryCommands.RESERVATION_CONFLICT,
                new Runnable() { public void run() { service.reserveStudyRoom(otherStudent, new StudyRoomReservationRequest(1L,
                        LocalDateTime.of(date, LocalTime.of(10, 0)),
                        LocalDateTime.of(date, LocalTime.of(12, 0)))); } });
    }

    @Test
    public void onlyLibrarianCanMaintainBooks() {
        assertCode(ResultCodes.FORBIDDEN, new Runnable() { public void run() { service.saveBook(student,
                new BookUpsertRequest(0L, "I-2", "DB", null, null, null,
                        Integer.valueOf(1), Integer.valueOf(1), null, null, "ON_SHELF")); } });
        BookDetail saved = service.saveBook(librarian, new BookUpsertRequest(0L, "I-2",
                "DB", null, null, null, Integer.valueOf(1), Integer.valueOf(1),
                null, null, "ON_SHELF"));
        assertEquals("DB", saved.getTitle());
    }

    @Test
    public void authorizationUsesActiveRoleOnly() {
        final SessionContext multi = new SessionContext("multi", 30L, "multi", "多角色",
                EnumSet.of(Role.STUDENT, Role.LIBRARIAN), Role.STUDENT);
        assertCode(ResultCodes.FORBIDDEN, new Runnable() { public void run() { service.saveBook(multi,
                new BookUpsertRequest(0L, "I-3", "网络", null, null, null,
                        Integer.valueOf(1), Integer.valueOf(1), null, null, "ON_SHELF")); } });
        SessionContext librarianRole = multi.withActiveRole(Role.LIBRARIAN);
        assertEquals("网络", service.saveBook(librarianRole, new BookUpsertRequest(0L,
                "I-3", "网络", null, null, null, Integer.valueOf(1), Integer.valueOf(1),
                null, null, "ON_SHELF")).getTitle());
    }

    @Test
    public void accessUsesSessionIdentityAndRejectsInactiveForStudent() {
        OnlineResourceView active = service.accessResource(student,
                new OnlineResourceAccessRequest(1L));
        assertEquals("开放资源", active.getTitle());
        assertEquals(1, accessLogs.size());
        assertCode(ResultCodes.FORBIDDEN, new Runnable() { public void run() { service.accessResource(student,
                new OnlineResourceAccessRequest(2L)); } });
        assertEquals(1, accessLogs.size());
    }

    @Test
    public void librarianMayAccessInactiveAndOnlyLibrarianReadsLogs() {
        service.accessResource(librarian, new OnlineResourceAccessRequest(2L));
        OnlineResourceAccessLogPage page = service.searchResourceAccessLogs(librarian,
                new OnlineResourceAccessLogQuery(null, Long.valueOf(20L), null, null, 1, 20));
        assertEquals(1L, page.getTotal());
        assertEquals("停用资源", page.getItems().get(0).getResourceTitle());
        assertEquals("用户20", page.getItems().get(0).getDisplayName());
        assertCode(ResultCodes.FORBIDDEN, new Runnable() { public void run() { service.searchResourceAccessLogs(student,
                new OnlineResourceAccessLogQuery()); } });
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id, id, "u" + id, "用户" + id,
                Collections.singleton(role), role);
    }

    private static void assertCode(String code, Runnable action) {
        try {
            action.run();
            fail("expected " + code);
        } catch (LibraryServiceException ex) {
            assertEquals(code, ex.getResultCode());
        }
    }
}
