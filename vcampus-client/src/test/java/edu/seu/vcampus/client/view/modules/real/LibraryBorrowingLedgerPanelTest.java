package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 借阅台账只向图书管理员组合，并覆盖空结果与覆盖确认边界。 */
public final class LibraryBorrowingLedgerPanelTest {
    @Test public void onlyLibrarianGetsBorrowingLedger() {
        assertTrue(RealLibraryPage.showsBorrowingLedger(Role.LIBRARIAN));
        assertFalse(RealLibraryPage.showsBorrowingLedger(Role.STUDENT));
        assertFalse(RealLibraryPage.showsBorrowingLedger(Role.TEACHER));
    }

    @Test public void emptyExportStillHasHeaderAndExistingFileNeedsConfirmation() throws Exception {
        List<String[]> rows = LibraryBorrowingLedgerPanel.csvRows(Collections.<edu.seu.vcampus.common.dto.library.BorrowRecordView>emptyList());
        assertEquals("记录编号", rows.get(0)[0]);
        assertEquals(1, rows.size());
        Path temp = Files.createTempFile("borrow-ledger", ".csv");
        try {
            assertTrue(LibraryBorrowingLedgerPanel.needsOverwriteConfirmation(temp));
            Files.delete(temp);
            assertFalse(LibraryBorrowingLedgerPanel.needsOverwriteConfirmation(temp));
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
