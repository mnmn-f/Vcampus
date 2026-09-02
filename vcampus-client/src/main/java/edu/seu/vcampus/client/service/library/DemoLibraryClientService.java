package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
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

/** Demo 模式图书馆服务门面；数据仅保存在当前客户端进程。 */
public final class DemoLibraryClientService implements LibraryClientService {
    private final DemoBookService books = new DemoBookService();
    private final DemoStudyRoomService rooms = new DemoStudyRoomService();
    private final DemoOnlineResourceService resources = new DemoOnlineResourceService();

    @Override
    public synchronized PageResult<BookDetail> searchBooks(BookSearchRequest request) {
        return books.search(request);
    }

    @Override
    public synchronized BookDetail bookDetail(long bookId) throws NetworkClientException {
        return books.detail(bookId);
    }

    @Override
    public synchronized BorrowRecordView borrow(BorrowRequest request)
            throws NetworkClientException {
        return books.borrow(request);
    }

    @Override
    public synchronized BorrowRecordView returnBook(long borrowRecordId)
            throws NetworkClientException {
        return books.returnBook(borrowRecordId);
    }

    @Override
    public synchronized PageResult<BorrowRecordView> myBorrowings(BorrowSearchRequest request) {
        return books.myBorrowings(request);
    }

    @Override
    public synchronized PageResult<BorrowRecordView> adminBorrowings(
            BorrowAdminSearchRequest request) {
        return books.adminBorrowings(request);
    }

    @Override
    public synchronized BookDetail saveBook(BookUpsertRequest request)
            throws NetworkClientException {
        return books.save(request);
    }

    @Override
    public synchronized PageResult<StudyRoomView> searchStudyRooms(
            StudyRoomSearchRequest request) {
        return rooms.search(request);
    }

    @Override
    public synchronized StudyRoomReservationView reserveStudyRoom(
            StudyRoomReservationRequest request) throws NetworkClientException {
        return rooms.reserve(request);
    }

    @Override
    public synchronized StudyRoomReservationView cancelReservation(long reservationId)
            throws NetworkClientException {
        return rooms.cancel(reservationId);
    }

    @Override
    public synchronized PageResult<StudyRoomReservationView> reservations(
            StudyRoomReservationSearchRequest request) {
        return rooms.reservations(request);
    }

    @Override
    public synchronized StudyRoomView saveStudyRoom(StudyRoomUpsertRequest request)
            throws NetworkClientException {
        return rooms.save(request);
    }

    @Override
    public synchronized PageResult<OnlineResourceView> searchResources(
            OnlineResourceSearchRequest request) {
        return resources.search(request);
    }

    @Override
    public synchronized OnlineResourceView saveResource(OnlineResourceUpsertRequest request)
            throws NetworkClientException {
        return resources.save(request);
    }

    @Override
    public synchronized OnlineResourceView accessResource(long resourceId)
            throws NetworkClientException {
        return resources.access(resourceId);
    }

    @Override
    public synchronized OnlineResourceAccessLogPage searchResourceAccessLogs(
            OnlineResourceAccessLogQuery request) {
        return resources.accessLogs(request);
    }
}
