package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;

/** 真实网络客户端实现；页面只依赖 LibraryClientService。 */
public final class NetworkLibraryClientService implements LibraryClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkLibraryClientService(NetworkClientService network) {
        this(network, null);
    }

    public NetworkLibraryClientService(NetworkClientService network, ClientSession session) {
        if (network == null) throw new IllegalArgumentException("network is required");
        this.network = network;
        this.session = session;
    }

    @Override
    public PdfClientService pdf(ClientSession value) {
        return new NetworkPdfClientService(network, value == null ? session : value);
    }

    @Override public PageResult<BookDetail> searchBooks(BookSearchRequest r) throws NetworkClientException {
        return page(request(LibraryCommands.BOOK_SEARCH, r), BookDetail.class);
    }
    @Override public BookDetail bookDetail(long id) throws NetworkClientException {
        return value(request(LibraryCommands.BOOK_DETAIL, new LibraryIdRequest(id)), BookDetail.class);
    }
    @Override public BorrowRecordView borrow(BorrowRequest r) throws NetworkClientException {
        return value(request(LibraryCommands.BOOK_BORROW, r), BorrowRecordView.class);
    }
    @Override public BorrowRecordView returnBook(long id) throws NetworkClientException {
        return value(request(LibraryCommands.BOOK_RETURN,
                new ReturnBorrowRequest(id)), BorrowRecordView.class);
    }
    @Override public PageResult<BorrowRecordView> myBorrowings(BorrowSearchRequest r)
            throws NetworkClientException {
        return page(request(LibraryCommands.BORROW_MINE, r), BorrowRecordView.class);
    }
    @Override public PageResult<BorrowRecordView> adminBorrowings(BorrowAdminSearchRequest r)
            throws NetworkClientException {
        BorrowAdminSearchRequest query = r == null ? new BorrowAdminSearchRequest() : r;
        return page(request(LibraryCommands.BORROW_ADMIN_LIST, query), BorrowRecordView.class);
    }
    @Override public BookDetail saveBook(BookUpsertRequest r) throws NetworkClientException {
        return value(request(LibraryCommands.BOOK_SAVE, r), BookDetail.class);
    }
    @Override public PageResult<StudyRoomView> searchStudyRooms(StudyRoomSearchRequest r)
            throws NetworkClientException {
        return page(request(LibraryCommands.STUDY_ROOM_SEARCH, r), StudyRoomView.class);
    }
    @Override public StudyRoomReservationView reserveStudyRoom(StudyRoomReservationRequest r)
            throws NetworkClientException {
        return value(request(LibraryCommands.STUDY_ROOM_RESERVE, r), StudyRoomReservationView.class);
    }
    @Override public StudyRoomReservationView cancelReservation(long id) throws NetworkClientException {
        return value(request(LibraryCommands.STUDY_ROOM_CANCEL,
                new LibraryIdRequest(id)), StudyRoomReservationView.class);
    }
    @Override public PageResult<StudyRoomReservationView> reservations(
            StudyRoomReservationSearchRequest r) throws NetworkClientException {
        return page(request(LibraryCommands.STUDY_ROOM_RESERVATIONS, r),
                StudyRoomReservationView.class);
    }
    @Override public StudyRoomView saveStudyRoom(StudyRoomUpsertRequest r) throws NetworkClientException {
        return value(request(LibraryCommands.STUDY_ROOM_SAVE, r), StudyRoomView.class);
    }
    @Override public PageResult<OnlineResourceView> searchResources(OnlineResourceSearchRequest r)
            throws NetworkClientException {
        return page(request(LibraryCommands.RESOURCE_SEARCH, r), OnlineResourceView.class);
    }
    @Override public OnlineResourceView saveResource(OnlineResourceUpsertRequest r)
            throws NetworkClientException {
        return value(request(LibraryCommands.RESOURCE_SAVE, r), OnlineResourceView.class);
    }
    @Override public OnlineResourceView accessResource(long resourceId)
            throws NetworkClientException {
        return value(request(LibraryCommands.RESOURCE_ACCESS,
                new OnlineResourceAccessRequest(resourceId)), OnlineResourceView.class);
    }
    @Override public OnlineResourceAccessLogPage searchResourceAccessLogs(
            OnlineResourceAccessLogQuery r) throws NetworkClientException {
        OnlineResourceAccessLogQuery query = r == null ? new OnlineResourceAccessLogQuery() : r;
        return value(request(LibraryCommands.RESOURCE_ACCESS_LOGS, query),
                OnlineResourceAccessLogPage.class);
    }

    private Message request(String command, java.io.Serializable body)
            throws NetworkClientException {
        if (session != null) {
            if (!session.isAuthenticated()) {
                throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
            }
            network.setSessionToken(session.getSessionToken());
        }
        return network.request(command, body);
    }

    @SuppressWarnings("unchecked")
    private static <T> PageResult<T> page(Message message, Class<T> itemType)
            throws NetworkClientException {
        if (!(message.getPayload() instanceof PageResult)) {
            throw new NetworkClientException("COMMON.INTERNAL_ERROR", "图书馆分页响应格式不正确");
        }
        PageResult<?> result = (PageResult<?>) message.getPayload();
        for (Object item : result.getItems()) {
            if (!itemType.isInstance(item)) {
                throw new NetworkClientException("COMMON.INTERNAL_ERROR", "图书馆列表响应格式不正确");
            }
        }
        return (PageResult<T>) result;
    }

    private static <T> T value(Message message, Class<T> type) throws NetworkClientException {
        if (!type.isInstance(message.getPayload())) {
            throw new NetworkClientException("COMMON.INTERNAL_ERROR", "图书馆响应格式不正确");
        }
        return type.cast(message.getPayload());
    }
}
