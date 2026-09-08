package edu.seu.vcampus.server.db;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** V15 是当前 V14 主线之后的显式图书馆扩展，不启动时隐式建表。 */
public final class LibraryPdfMigrationCompatibilityTest {
    @Test public void usesNextVersionAndIsAdditive() throws Exception {
        String sql = read("/db/migration/V15__library_pdf_and_book_details.sql");
        assertFalse(sql.contains("USE `vcampus`"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("ALTER TABLE books ADD COLUMN publication_year"));
        assertTrue(sql.contains("ALTER TABLE books ADD COLUMN cover_image"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `library_pdf_resources`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `library_pdf_downloads`"));
        assertFalse(sql.contains("DROP TABLE"));
        assertFalse(sql.contains("TRUNCATE TABLE"));
    }

    private static String read(String path) throws Exception {
        try (InputStream input = LibraryPdfMigrationCompatibilityTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing migration");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
