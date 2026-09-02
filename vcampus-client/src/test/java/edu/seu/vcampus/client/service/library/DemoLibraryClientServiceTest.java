package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Demo 图书馆的界面操作必须真正改变本地业务数据。 */
public final class DemoLibraryClientServiceTest {
    @Test
    public void borrowAndReturnUpdateInventory() throws Exception {
        DemoLibraryClientService service = new DemoLibraryClientService();
        BookDetail before = service.bookDetail(101L);
        BorrowRecordView borrowed = service.borrow(new BorrowRequest(101L));
        assertEquals(before.getAvailableCopies() - 1,
                service.bookDetail(101L).getAvailableCopies());
        service.returnBook(borrowed.getId());
        assertEquals(before.getAvailableCopies(),
                service.bookDetail(101L).getAvailableCopies());
    }

    @Test
    public void roomAndResourceChangesAreSearchable() throws Exception {
        DemoLibraryClientService service = new DemoLibraryClientService();
        StudyRoomView room = service.saveStudyRoom(new StudyRoomUpsertRequest(0L,
                "李文正图书馆四楼", "D401", 16, LocalTime.of(8, 0),
                LocalTime.of(22, 0), "OPEN", "Demo 新增自习室"));
        assertTrue(room.getId() > 0L);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        StudyRoomReservationView reservation = service.reserveStudyRoom(
                new StudyRoomReservationRequest(room.getId(), start, start.plusHours(1)));
        assertEquals("RESERVED", reservation.getStatus());
        assertEquals("CANCELLED", service.cancelReservation(reservation.getId()).getStatus());

        OnlineResourceView resource = service.saveResource(new OnlineResourceUpsertRequest(
                0L, "Demo 文献库", "学术数据库", "https://example.com/library",
                "用于测试线上资源维护", "ACTIVE"));
        service.accessResource(resource.getId());
        assertEquals(1L, service.searchResourceAccessLogs(
                new OnlineResourceAccessLogQuery()).getTotal());
    }

    @Test
    public void categoryIsIncludedInBookKeywordSearch() {
        DemoLibraryClientService service = new DemoLibraryClientService();
        assertTrue(service.searchBooks(new BookSearchRequest("数学", null, null, 1, 20))
                .getTotal() > 0L);
    }
}
