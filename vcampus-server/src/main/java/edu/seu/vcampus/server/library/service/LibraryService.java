package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
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
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.BookRepository;
import edu.seu.vcampus.server.library.repository.BorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.OnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.OnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.StudyRoomRepository;
import edu.seu.vcampus.server.library.repository.StudyRoomReservationRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 图书馆纵向门面；具体规则分布在图书、借阅、自习室和资源服务中。 */
public final class LibraryService {
    private PdfLibraryService pdfService;
    public LibraryService withPdf(PdfLibraryService service) { this.pdfService = service; return this; }
    public PdfLibraryService pdf() {
        if (pdfService == null) {
            pdfService = new PdfLibraryService(new edu.seu.vcampus.server.library.repository.InMemoryPdfRepository(),
                    new edu.seu.vcampus.common.library.PdfFileStore(java.nio.file.Paths.get("data", "library-pdf-test")), null);
        }
        return pdfService;
    }
    private final BookService bookService;
    private final BorrowService borrowService;
    private final StudyRoomService studyRoomService;
    private final OnlineResourceService resourceService;

    public LibraryService(BookRepository books, BorrowRepository borrows,
                          StudyRoomRepository rooms,
                          StudyRoomReservationRepository reservations,
                          OnlineResourceRepository resources,
                          TransactionManager transactions) {
        this(books, borrows, rooms, reservations, resources,
                new InMemoryOnlineResourceAccessLogRepository(), transactions);
    }

    public LibraryService(BookRepository books, BorrowRepository borrows,
                          StudyRoomRepository rooms,
                          StudyRoomReservationRepository reservations,
                          OnlineResourceRepository resources,
                          OnlineResourceAccessLogRepository accessLogs,
                          TransactionManager transactions) {
        bookService = new BookService(books, transactions);
        borrowService = new BorrowService(books, borrows, transactions);
        studyRoomService = new StudyRoomService(rooms, reservations, transactions);
        resourceService = new OnlineResourceService(resources, accessLogs, transactions);
    }

    public PageResult<BookDetail> searchBooks(SessionContext s, BookSearchRequest r) {
        return bookService.search(s, r);
    }
    public BookDetail bookDetail(SessionContext s, long id) { return bookService.detail(s, id); }
    public BookDetail saveBook(SessionContext s, BookUpsertRequest r) { return bookService.save(s, r); }

    public BorrowRecordView borrow(SessionContext s, BorrowRequest r) { return borrowService.borrow(s, r); }
    public BorrowRecordView returnBook(SessionContext s, long id) { return borrowService.returnBook(s, id); }
    public PageResult<BorrowRecordView> myBorrowings(SessionContext s, BorrowSearchRequest r) {
        return borrowService.mine(s, r);
    }
    public PageResult<BorrowRecordView> adminBorrowings(SessionContext s, BorrowAdminSearchRequest r) {
        return borrowService.adminList(s, r);
    }

    public PageResult<StudyRoomView> searchStudyRooms(SessionContext s, StudyRoomSearchRequest r) {
        return studyRoomService.search(s, r);
    }
    public StudyRoomView saveStudyRoom(SessionContext s, StudyRoomUpsertRequest r) {
        return studyRoomService.save(s, r);
    }
    public StudyRoomReservationView reserveStudyRoom(SessionContext s, StudyRoomReservationRequest r) {
        return studyRoomService.reserve(s, r);
    }
    public StudyRoomReservationView cancelReservation(SessionContext s, long id) {
        return studyRoomService.cancel(s, id);
    }
    public PageResult<StudyRoomReservationView> listReservations(
            SessionContext s, StudyRoomReservationSearchRequest r) {
        return studyRoomService.listReservations(s, r);
    }

    public PageResult<OnlineResourceView> searchResources(SessionContext s,
                                                          OnlineResourceSearchRequest r) {
        return resourceService.search(s, r);
    }
    public OnlineResourceView saveResource(SessionContext s, OnlineResourceUpsertRequest r) {
        return resourceService.save(s, r);
    }
    public OnlineResourceView accessResource(SessionContext s, OnlineResourceAccessRequest r) {
        return resourceService.access(s, r);
    }
    public OnlineResourceAccessLogPage searchResourceAccessLogs(SessionContext s,
                                                                 OnlineResourceAccessLogQuery r) {
        return resourceService.searchAccessLogs(s, r);
    }
}
