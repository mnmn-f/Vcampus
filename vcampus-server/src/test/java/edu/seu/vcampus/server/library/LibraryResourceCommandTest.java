package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.repository.InMemoryBorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceAccessLogRepository;
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

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 线上资源访问命令按当前职责鉴权并记录服务端会话身份。 */
public final class LibraryResourceCommandTest {
    private InMemoryOnlineResourceRepository resources;
    private InMemoryOnlineResourceAccessLogRepository logs;
    private CommandRouter router;
    private SessionContext student;
    private SessionContext librarian;

    @Before public void setUp() {
        resources = new InMemoryOnlineResourceRepository();
        resources.seed(new OnlineResourceView(1L, "启用资源", "文档",
                "https://example.com/open", null, 9L, "ACTIVE", null));
        resources.seed(new OnlineResourceView(2L, "停用资源", "文档",
                "https://example.com/closed", null, 9L, "INACTIVE", null));
        logs = new InMemoryOnlineResourceAccessLogRepository();
        LibraryService service = new LibraryService(new InMemoryBookRepository(),
                new InMemoryBorrowRepository(), new InMemoryStudyRoomRepository(),
                new InMemoryStudyRoomReservationRepository(), resources, logs, null);
        SessionManager sessions = new SessionManager();
        student = sessions.createSession(7L, "student", "学生", Collections.singleton(Role.STUDENT), Role.STUDENT);
        librarian = sessions.createSession(9L, "librarian", "图书管理员", Collections.singleton(Role.LIBRARIAN), Role.LIBRARIAN);
        router = LibraryCommandRegistry.registerAll(new CommandRouter(sessions), service);
    }

    @Test public void inactiveStudentIsRejectedWithoutLog() {
        Message response = route(LibraryCommands.RESOURCE_ACCESS, student,
                new OnlineResourceAccessRequest(2L));
        assertEquals(ResultCodes.FORBIDDEN, response.getResultCode());
        assertEquals(0, logs.size());
    }

    @Test public void librarianCanAccessAndReadLogs() {
        Message access = route(LibraryCommands.RESOURCE_ACCESS, librarian,
                new OnlineResourceAccessRequest(2L));
        assertEquals(true, access.isSuccess());
        Message query = route(LibraryCommands.RESOURCE_ACCESS_LOGS, librarian,
                new OnlineResourceAccessLogQuery());
        assertEquals(true, query.isSuccess());
        Message studentQuery = route(LibraryCommands.RESOURCE_ACCESS_LOGS, student,
                new OnlineResourceAccessLogQuery());
        assertEquals(ResultCodes.FORBIDDEN, studentQuery.getResultCode());
    }

    private Message route(String command, SessionContext session, java.io.Serializable payload) {
        return router.route(Message.request(command, session.getSessionToken(), payload));
    }
}
