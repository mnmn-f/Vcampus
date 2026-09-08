package edu.seu.vcampus.server.library.registry;

import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.library.handler.LibraryCommandHandler;
import edu.seu.vcampus.server.library.repository.mysql.MySqlBookRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlBorrowRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlOnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlOnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlStudyRoomRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlStudyRoomReservationRepository;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;

import java.io.Serializable;

/** 图书馆命令接线点；ServerMain 只需创建服务并调用 register。 */
public final class LibraryCommandRegistry {
    private LibraryCommandRegistry() {
    }

    /** 生产接线的唯一默认组装点；数据库连接仍由调用方提供。 */
    public static LibraryService createMySqlService(TransactionManager transactions) {
        if (transactions == null) throw new IllegalArgumentException("transactions is required");
        return new LibraryService(new MySqlBookRepository(), new MySqlBorrowRepository(),
                new MySqlStudyRoomRepository(), new MySqlStudyRoomReservationRepository(),
                new MySqlOnlineResourceRepository(), new MySqlOnlineResourceAccessLogRepository(),
                transactions);
    }

    public static CommandRouter register(CommandRouter router, final LibraryService service) {
        if (router == null || service == null) throw new IllegalArgumentException("library dependencies required");
        return router
                .register(LibraryCommands.BOOK_SEARCH, handler(Permission.LIBRARY_READ,
                        new ServiceAction(service, LibraryCommands.BOOK_SEARCH)))
                .register(LibraryCommands.BOOK_DETAIL, handler(Permission.LIBRARY_READ,
                        new ServiceAction(service, LibraryCommands.BOOK_DETAIL)))
                .register(LibraryCommands.BOOK_BORROW, handler(Permission.LIBRARY_BORROW,
                        new ServiceAction(service, LibraryCommands.BOOK_BORROW)))
                .register(LibraryCommands.BOOK_RETURN, handler(Permission.LIBRARY_BORROW,
                        new ServiceAction(service, LibraryCommands.BOOK_RETURN)))
                .register(LibraryCommands.BORROW_MINE, handler(Permission.LIBRARY_BORROW,
                        new ServiceAction(service, LibraryCommands.BORROW_MINE)))
                .register(LibraryCommands.BORROW_ADMIN_LIST, handler(Permission.LIBRARY_MANAGE,
                        new ServiceAction(service, LibraryCommands.BORROW_ADMIN_LIST)))
                .register(LibraryCommands.BOOK_SAVE, handler(Permission.LIBRARY_MANAGE,
                        new ServiceAction(service, LibraryCommands.BOOK_SAVE)))
                .register(LibraryCommands.STUDY_ROOM_SEARCH, handler(Permission.LIBRARY_READ,
                        new ServiceAction(service, LibraryCommands.STUDY_ROOM_SEARCH)))
                .register(LibraryCommands.STUDY_ROOM_RESERVE, handler(Permission.STUDY_ROOM_RESERVE,
                        new ServiceAction(service, LibraryCommands.STUDY_ROOM_RESERVE)))
                .register(LibraryCommands.STUDY_ROOM_CANCEL, handler(null,
                        new ServiceAction(service, LibraryCommands.STUDY_ROOM_CANCEL)))
                .register(LibraryCommands.STUDY_ROOM_RESERVATIONS, handler(null,
                        new ServiceAction(service, LibraryCommands.STUDY_ROOM_RESERVATIONS)))
                .register(LibraryCommands.STUDY_ROOM_SAVE, handler(Permission.STUDY_ROOM_MANAGE,
                        new ServiceAction(service, LibraryCommands.STUDY_ROOM_SAVE)))
                .register(LibraryCommands.RESOURCE_SEARCH, handler(Permission.LIBRARY_READ,
                        new ServiceAction(service, LibraryCommands.RESOURCE_SEARCH)))
                .register(LibraryCommands.RESOURCE_SAVE, handler(Permission.LIBRARY_MANAGE,
                        new ServiceAction(service, LibraryCommands.RESOURCE_SAVE)))
                .register(LibraryCommands.RESOURCE_ACCESS, handler(Permission.LIBRARY_READ,
                        new ServiceAction(service, LibraryCommands.RESOURCE_ACCESS)))
                .register(LibraryCommands.RESOURCE_ACCESS_LOGS, handler(Permission.LIBRARY_MANAGE,
                        new ServiceAction(service, LibraryCommands.RESOURCE_ACCESS_LOGS)));
    }

    public static CommandRouter registerAll(CommandRouter router, LibraryService service) {
        return register(router, service);
    }

    private static LibraryCommandHandler handler(Permission permission,
                                                  LibraryCommandHandler.Action action) {
        return new LibraryCommandHandler(permission, action);
    }

    private static final class ServiceAction implements LibraryCommandHandler.Action {
        private final LibraryService service;
        private final String command;

        ServiceAction(LibraryService service, String command) {
            this.service = service;
            this.command = command;
        }

        @Override
        public Serializable execute(Serializable payload, SessionContext session) {
            if (LibraryCommands.BOOK_SEARCH.equals(command)) return service.searchBooks(session,
                    payload == null ? new BookSearchRequest() : LibraryCommandHandler.payload(payload, BookSearchRequest.class));
            if (LibraryCommands.BOOK_DETAIL.equals(command)) return service.bookDetail(session,
                    LibraryCommandHandler.payload(payload, LibraryIdRequest.class).getId());
            if (LibraryCommands.BOOK_BORROW.equals(command)) return service.borrow(session,
                    LibraryCommandHandler.payload(payload, BorrowRequest.class));
            if (LibraryCommands.BOOK_RETURN.equals(command)) return service.returnBook(session,
                    LibraryCommandHandler.payload(payload, ReturnBorrowRequest.class).getBorrowRecordId());
            if (LibraryCommands.BORROW_MINE.equals(command)) return service.myBorrowings(session,
                    payload == null ? new BorrowSearchRequest() : LibraryCommandHandler.payload(payload, BorrowSearchRequest.class));
            if (LibraryCommands.BORROW_ADMIN_LIST.equals(command)) return service.adminBorrowings(session,
                    payload == null ? new BorrowAdminSearchRequest() : LibraryCommandHandler.payload(payload, BorrowAdminSearchRequest.class));
            if (LibraryCommands.BOOK_SAVE.equals(command)) return service.saveBook(session,
                    LibraryCommandHandler.payload(payload, BookUpsertRequest.class));
            if (LibraryCommands.STUDY_ROOM_SEARCH.equals(command)) return service.searchStudyRooms(session,
                    payload == null ? new StudyRoomSearchRequest() : LibraryCommandHandler.payload(payload, StudyRoomSearchRequest.class));
            if (LibraryCommands.STUDY_ROOM_RESERVE.equals(command)) return service.reserveStudyRoom(session,
                    LibraryCommandHandler.payload(payload, StudyRoomReservationRequest.class));
            if (LibraryCommands.STUDY_ROOM_CANCEL.equals(command)) return service.cancelReservation(session,
                    LibraryCommandHandler.payload(payload, LibraryIdRequest.class).getId());
            if (LibraryCommands.STUDY_ROOM_RESERVATIONS.equals(command)) return service.listReservations(session,
                    payload == null ? new StudyRoomReservationSearchRequest() : LibraryCommandHandler.payload(payload, StudyRoomReservationSearchRequest.class));
            if (LibraryCommands.STUDY_ROOM_SAVE.equals(command)) return service.saveStudyRoom(session,
                    LibraryCommandHandler.payload(payload, StudyRoomUpsertRequest.class));
            if (LibraryCommands.RESOURCE_SEARCH.equals(command)) return service.searchResources(session,
                    payload == null ? new OnlineResourceSearchRequest() : LibraryCommandHandler.payload(payload, OnlineResourceSearchRequest.class));
            if (LibraryCommands.RESOURCE_SAVE.equals(command)) return service.saveResource(session,
                    LibraryCommandHandler.payload(payload, OnlineResourceUpsertRequest.class));
            if (LibraryCommands.RESOURCE_ACCESS.equals(command)) return service.accessResource(session,
                    LibraryCommandHandler.payload(payload, OnlineResourceAccessRequest.class));
            if (LibraryCommands.RESOURCE_ACCESS_LOGS.equals(command)) return service.searchResourceAccessLogs(session,
                    payload == null ? new OnlineResourceAccessLogQuery() : LibraryCommandHandler.payload(payload, OnlineResourceAccessLogQuery.class));
            throw new IllegalArgumentException("unsupported library command: " + command);
        }
    }
}
