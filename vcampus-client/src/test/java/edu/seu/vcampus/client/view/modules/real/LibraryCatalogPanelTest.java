package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 学生借阅记录应即时识别逾期，即使服务端状态尚未批量刷新。 */
public final class LibraryCatalogPanelTest {
    @Test public void borrowedBookPastDueIsMarkedOverdue() {
        BorrowRecordView row = row("BORROWED", LocalDateTime.now().minusDays(2), null);
        assertTrue(LibraryCatalogPanel.isOverdue(row));
        assertEquals("已逾期", LibraryCatalogPanel.borrowingStatus(row));
    }

    @Test public void explicitOverdueStatusIsMarkedOverdue() {
        BorrowRecordView row = row("OVERDUE", LocalDateTime.now().plusDays(2), null);
        assertTrue(LibraryCatalogPanel.isOverdue(row));
    }

    @Test public void futureAndReturnedRecordsAreNotMarkedOverdue() {
        BorrowRecordView future = row("BORROWED", LocalDateTime.now().plusDays(2), null);
        BorrowRecordView returned = row("RETURNED", LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1));
        assertFalse(LibraryCatalogPanel.isOverdue(future));
        assertFalse(LibraryCatalogPanel.isOverdue(returned));
        assertEquals("借阅中", LibraryCatalogPanel.borrowingStatus(future));
        assertEquals("已归还", LibraryCatalogPanel.borrowingStatus(returned));
    }

    @Test public void borrowingColumnsFollowViewportWidth() {
        int[] compact = LibraryCatalogPanel.borrowingColumnWidths(700);
        int[] wide = LibraryCatalogPanel.borrowingColumnWidths(1200);
        assertEquals(700, sum(compact));
        assertEquals(1200, sum(wide));
        assertTrue(wide[0] > compact[0]);
        assertTrue(wide[1] > compact[1]);
    }

    private static BorrowRecordView row(String status, LocalDateTime dueAt,
                                        LocalDateTime returnedAt) {
        return new BorrowRecordView(1L, 2L, "Java 核心技术", 1L, "演示学生",
                LocalDateTime.now().minusDays(20), dueAt, returnedAt, status, 0, null);
    }

    private static int sum(int[] values) {
        int result = 0;
        for (int value : values) result += value;
        return result;
    }
}
