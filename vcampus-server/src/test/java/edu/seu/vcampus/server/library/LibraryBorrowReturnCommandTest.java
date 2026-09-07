package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.registry.LibraryCommandRegistry;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.repository.InMemoryBorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomReservationRepository;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 借书和还书命令经真实路由后必须同步改变库存。 */
public final class LibraryBorrowReturnCommandTest {
    @Test public void studentBorrowsAndReturnsThroughRegisteredCommands() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        books.seed(new BookDetail(8L, "TEST-8", "测试图书", "作者", "出版社",
                "计算机", 2, 2, "二楼", "测试", "ON_SHELF",
                LocalDateTime.now(), LocalDateTime.now()));
        LibraryService service = new LibraryService(books, new InMemoryBorrowRepository(),
                new InMemoryStudyRoomRepository(), new InMemoryStudyRoomReservationRepository(),
                new InMemoryOnlineResourceRepository(), null);
        SessionManager sessions = new SessionManager();
        SessionContext student = sessions.createSession(21L, "student", "学生",
                Collections.singleton(Role.STUDENT), Role.STUDENT);
        CommandRouter router = LibraryCommandRegistry.registerAll(
                new CommandRouter(sessions), service);

        Message borrowedResponse = router.route(Message.request(LibraryCommands.BOOK_BORROW,
                student.getSessionToken(), new BorrowRequest(8L)));
        assertTrue(borrowedResponse.getUserMessage(), borrowedResponse.isSuccess());
        BorrowRecordView borrowed = (BorrowRecordView) borrowedResponse.getPayload();
        assertEquals(1, books.findById(null, 8L).getAvailableCopies());

        Message returnedResponse = router.route(Message.request(LibraryCommands.BOOK_RETURN,
                student.getSessionToken(), new ReturnBorrowRequest(borrowed.getId())));
        assertTrue(returnedResponse.getUserMessage(), returnedResponse.isSuccess());
        assertEquals("RETURNED", ((BorrowRecordView) returnedResponse.getPayload()).getStatus());
        assertEquals(2, books.findById(null, 8L).getAvailableCopies());
    }
}
