package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.repository.InMemoryBorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomReservationRepository;
import edu.seu.vcampus.server.library.registry.LibraryCommandRegistry;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 管理员借阅台账只允许图书管理员并支持身份、状态和关键字筛选。 */
public final class LibraryBorrowLedgerCommandTest {
    private InMemoryBorrowRepository borrows;
    private CommandRouter router;
    private SessionContext student;
    private SessionContext librarian;

    @Before public void setUp() {
        borrows = new InMemoryBorrowRepository();
        LocalDateTime now = LocalDateTime.now();
        borrows.seed(new BorrowRecordView(1L, 1L, "Java 编程", 7L, "张三",
                now.minusDays(1), now.plusDays(20), null, "BORROWED", 0, null));
        borrows.seed(new BorrowRecordView(2L, 2L, "数据库原理", 8L, "李四",
                now.minusDays(2), now.plusDays(19), null, "BORROWED", 0, null));
        LibraryService service = new LibraryService(new InMemoryBookRepository(), borrows,
                new InMemoryStudyRoomRepository(), new InMemoryStudyRoomReservationRepository(),
                new InMemoryOnlineResourceRepository(), null);
        SessionManager sessions = new SessionManager();
        student = sessions.createSession(7L, "student", "张三", Collections.singleton(Role.STUDENT), Role.STUDENT);
        librarian = sessions.createSession(9L, "librarian", "图书管理员", Collections.singleton(Role.LIBRARIAN), Role.LIBRARIAN);
        router = LibraryCommandRegistry.registerAll(new CommandRouter(sessions), service);
    }

    @Test public void librarianCanFilterLedgerByStudentAndKeyword() {
        PageResult<BorrowRecordView> result = route(librarian,
                new BorrowAdminSearchRequest("BORROWED", 7L, "Java", 1, 20));
        assertEquals(1L, result.getTotal());
        assertEquals(7L, result.getItems().get(0).getBorrowerUserId());
        assertEquals("Java 编程", result.getItems().get(0).getBookTitle());
    }

    @Test public void studentCannotUseAdminLedgerCommand() {
        Message response = router.route(Message.request(LibraryCommands.BORROW_ADMIN_LIST,
                student.getSessionToken(), new BorrowAdminSearchRequest()));
        assertEquals(ResultCodes.FORBIDDEN, response.getResultCode());
    }

    private PageResult<BorrowRecordView> route(SessionContext session,
                                               BorrowAdminSearchRequest request) {
        Message response = router.route(Message.request(LibraryCommands.BORROW_ADMIN_LIST,
                session.getSessionToken(), request));
        assertEquals(true, response.isSuccess());
        return (PageResult<BorrowRecordView>) response.getPayload();
    }
}
