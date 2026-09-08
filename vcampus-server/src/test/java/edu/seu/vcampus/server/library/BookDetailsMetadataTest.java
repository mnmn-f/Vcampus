package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.service.BookService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/** 封面/年份在列表、库存改变和软删除后都保持可用。 */
public final class BookDetailsMetadataTest {
    @Test public void saveInventoryAndArchivePreserveBookMetadata() throws Exception {
        InMemoryBookRepository repository = new InMemoryBookRepository(); BookService service = new BookService(repository, null);
        SessionContext librarian = new SessionContext("token", 3, "admin", "图书管理员", java.util.Collections.singleton(Role.LIBRARIAN), Role.LIBRARIAN);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "png", out); byte[] cover = out.toByteArray();
        BookDetail saved = service.save(librarian, new BookUpsertRequest(0, "123", "测试图书", "作者", "出版社", "分类", 3, 3, "二楼", "简介", "ON_SHELF", 2024, cover));
        repository.decrementAvailable(null, saved.getId());
        BookDetail detail = service.detail(librarian, saved.getId()); assertEquals(Integer.valueOf(2024), detail.getPublicationYear()); assertArrayEquals(cover, detail.getCoverImage());
        assertNull("列表查询不应携带封面 BLOB", service.search(librarian, new BookSearchRequest()).getItems().get(0).getCoverImage());
        BookDetail archived = service.save(librarian, new BookUpsertRequest(saved.getId(), null, null, null, null, null, null, null, null, null, "ARCHIVED"));
        assertArrayEquals(cover, archived.getCoverImage()); assertEquals(Integer.valueOf(2024), archived.getPublicationYear()); assertEquals(2, archived.getAvailableCopies());
    }
}
