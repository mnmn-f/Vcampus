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

/** 客户端图书馆服务边界；不暴露 Socket 或服务端仓储。 */
public interface LibraryClientService {
    default PdfClientService pdf(edu.seu.vcampus.client.session.ClientSession session) { return null; }
    PageResult<BookDetail> searchBooks(BookSearchRequest request) throws NetworkClientException;
    BookDetail bookDetail(long bookId) throws NetworkClientException;
    BorrowRecordView borrow(BorrowRequest request) throws NetworkClientException;
    BorrowRecordView returnBook(long borrowRecordId) throws NetworkClientException;
    PageResult<BorrowRecordView> myBorrowings(BorrowSearchRequest request) throws NetworkClientException;
    PageResult<BorrowRecordView> adminBorrowings(BorrowAdminSearchRequest request)
            throws NetworkClientException;
    BookDetail saveBook(BookUpsertRequest request) throws NetworkClientException;
    PageResult<StudyRoomView> searchStudyRooms(StudyRoomSearchRequest request) throws NetworkClientException;
    StudyRoomReservationView reserveStudyRoom(StudyRoomReservationRequest request) throws NetworkClientException;
    StudyRoomReservationView cancelReservation(long reservationId) throws NetworkClientException;
    PageResult<StudyRoomReservationView> reservations(StudyRoomReservationSearchRequest request)
            throws NetworkClientException;
    StudyRoomView saveStudyRoom(StudyRoomUpsertRequest request) throws NetworkClientException;
    PageResult<OnlineResourceView> searchResources(OnlineResourceSearchRequest request)
            throws NetworkClientException;
    OnlineResourceView saveResource(OnlineResourceUpsertRequest request) throws NetworkClientException;
    OnlineResourceView accessResource(long resourceId) throws NetworkClientException;
    OnlineResourceAccessLogPage searchResourceAccessLogs(OnlineResourceAccessLogQuery request)
            throws NetworkClientException;
}
