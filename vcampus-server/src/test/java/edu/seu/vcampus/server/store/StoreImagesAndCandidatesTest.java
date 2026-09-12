package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.*;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.*;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

public class StoreImagesAndCandidatesTest {
    private final InMemoryStoreRecordRepository core = new InMemoryStoreRecordRepository();
    private final StoreService service = new StoreService(core, new InMemoryStoreTransactionRunner(core));
    private final SessionContext manager = session(90, Role.STORE_MANAGER), student = session(10, Role.STUDENT);

    @Test public void imageSurvivesEditsReplacesAndRemovesWithRoleVisibility() throws Exception {
        byte[] image = image();
        ProductDto product = service.saveProduct(manager, write(0, "ON_SALE", null, image));
        assertArrayEquals(image, service.getProductImage(student, product.getImageUrl()));
        byte[] copy = service.getProductImage(student, product.getImageUrl()); copy[0] = 0;
        assertArrayEquals(image, service.getProductImage(student, product.getImageUrl()));
        service.saveProduct(manager, write(product.getId(), "ON_SALE", product.getImageUrl(), null));
        assertArrayEquals(image, service.getProductImage(student, product.getImageUrl()));
        ProductDto replacement = service.saveProduct(manager, write(product.getId(), "DRAFT", null, image));
        assertNotEquals(product.getImageUrl(), replacement.getImageUrl());
        rejected(() -> service.getProductImage(student, replacement.getImageUrl()));
        assertArrayEquals(image, service.getProductImage(manager, replacement.getImageUrl()));
        service.saveProduct(manager, write(product.getId(), "ON_SALE", null, null));
        rejected(() -> service.getProductImage(manager, replacement.getImageUrl()));
        rejected(() -> service.saveProduct(student, write(0, "ON_SALE", null, image)));
    }
    @Test public void invalidUploadsAndForgedReferencesAreRejectedAndRollbackRestoresImage() throws Exception {
        rejected(() -> service.saveProduct(manager, write(0, "ON_SALE", null, new byte[]{1,2,3})));
        rejected(() -> service.saveProduct(manager, write(0, "ON_SALE", "file:///secret.jpg", null)));
        rejected(() -> service.saveProduct(manager, write(0, "ON_SALE", "store-image:" + java.util.UUID.randomUUID(), null)));
        ProductDto product = service.saveProduct(manager, write(0, "ON_SALE", null, image()));
        try { new InMemoryStoreTransactionRunner(core).execute(c -> { core.updateProduct(c, write(product.getId(), "ON_SALE", null, null), 90); throw new IllegalStateException(); }); }
        catch (IllegalStateException expected) { }
        assertArrayEquals(image(), service.getProductImage(student, product.getImageUrl()));
    }
    @Test public void moreThanHundredOrdersArePagedOwnedAndCompletedOnlyAndReviewedRowsDisappear() throws Exception {
        core.addProduct(new ProductDto(1, "P", "校园杯", "CULTURE", "", BigDecimal.TEN, 300, "ON_SALE"));
        for (int id=1; id<=125; id++) core.addOrder(order(id, 10, "COMPLETED"));
        core.addOrder(order(126, 11, "COMPLETED")); core.addOrder(order(127, 10, "CREATED"));
        ReviewCandidatePage page = service.reviewCandidates(student, new ProductReviewQuery(0, 7, 20));
        assertEquals(125, page.getTotal()); assertEquals(5, page.getItems().size());
        assertEquals(1, service.reviewCandidates(session(11, Role.STUDENT), new ProductReviewQuery(0)).getTotal());
        assertEquals(1, service.reviewCandidates(student, new ProductReviewQuery(0, 1, 20, "ORDER-125")).getTotal());
        service.addReview(student, new ProductReviewWriteRequest(125, 1, 5, "实用"));
        assertEquals(124, service.reviewCandidates(student, new ProductReviewQuery(0)).getTotal());
        assertEquals(0, service.reviewCandidates(student, new ProductReviewQuery(0, 1, 20, "ORDER-125")).getTotal());
        assertEquals(1, service.listReviews(session(11, Role.STUDENT), new ProductReviewQuery(1, 1, 20, "实用")).getTotal());
        assertEquals(0, service.listReviews(student, new ProductReviewQuery(1, 1, 20, "不好")).getTotal());
        rejected(() -> service.reviewCandidates(manager, new ProductReviewQuery(0)));
    }
    public static byte[] image() throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 6, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes); return bytes.toByteArray();
    }
    private static ProductWriteRequest write(long id, String status, String ref, byte[] bytes) {
        return new ProductWriteRequest(id, "UPLOAD-TEST", "校园杯", "CULTURE", "完整说明", BigDecimal.TEN, 50, status, ref, bytes);
    }
    private static OrderDto order(long id, long user, String status) {
        LocalDateTime now = LocalDateTime.now();
        return new OrderDto(id, "ORDER-" + id, user, BigDecimal.TEN, status, now, now, null, now,
                Collections.singletonList(new OrderItemDto(1, "校园杯", BigDecimal.TEN, 1)));
    }
    private static SessionContext session(long id, Role role) { return new SessionContext("test-" + id, id, "user" + id, "用户", Collections.singleton(role), role); }
    private interface Work { void run() throws Exception; }
    private static void rejected(Work work) throws Exception { try { work.run(); fail("must reject"); } catch (StoreServiceException expected) { } }
}
