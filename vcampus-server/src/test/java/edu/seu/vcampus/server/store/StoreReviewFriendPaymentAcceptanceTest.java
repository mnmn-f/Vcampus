package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDecisionRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 评价和好友代付的对象归属、一次性约束、扣款及退款账户验收。 */
public final class StoreReviewFriendPaymentAcceptanceTest {
    private InMemoryStoreRecordRepository core;
    private InMemoryStoreExperienceRepository experience;
    private StoreService service;
    private SessionContext buyer;
    private SessionContext other;
    private SessionContext payer;
    private SessionContext manager;

    @Before
    public void setUp() {
        core = new InMemoryStoreRecordRepository();
        experience = new InMemoryStoreExperienceRepository(core);
        core.addProduct(new ProductDto(1L, "SKU-1", "校园杯", "CULTURE", null,
                new BigDecimal("12.00"), 10, "ON_SALE"));
        core.addAccount(new AccountDto(1L, 10L, new BigDecimal("100.00"), "ACTIVE"));
        core.addAccount(new AccountDto(2L, 11L, new BigDecimal("50.00"), "ACTIVE"));
        core.addAccount(new AccountDto(3L, 12L, new BigDecimal("50.00"), "ACTIVE"));
        experience.addFriendAccount("payer", 11L);
        service = new StoreService(core, new InMemoryStoreTransactionRunner(core), experience);
        buyer = session(10L, Role.STUDENT);
        payer = session(11L, Role.STUDENT);
        other = session(12L, Role.STUDENT);
        manager = session(99L, Role.STORE_MANAGER);
    }

    @Test
    public void completedBuyerCanReviewEachOrderProductOnlyOnce() throws Exception {
        core.addOrder(order(1L, 10L, "COMPLETED"));
        assertEquals(5, service.addReview(buyer,
                new ProductReviewWriteRequest(1L, 1L, 5, "很好")).getScore());
        try {
            service.addReview(buyer, new ProductReviewWriteRequest(1L, 1L, 4, "再次评价"));
            fail("the same order product must be reviewable once");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
        assertEquals(1L, service.listReviews(buyer,
                new edu.seu.vcampus.common.dto.store.ProductReviewQuery(1L)).getTotal());
    }

    @Test
    public void reviewsRequireCompletedOrderOwnedByCaller() throws Exception {
        core.addOrder(order(2L, 10L, "CREATED"));
        core.addOrder(order(3L, 12L, "COMPLETED"));
        assertReviewRejected(new ProductReviewWriteRequest(2L, 1L, 5, "未完成"));
        assertReviewRejected(new ProductReviewWriteRequest(3L, 1L, 5, "他人订单"));
    }

    @Test
    public void friendPaymentRequiresBuyerRequestAndPayerDecision() throws Exception {
        core.addOrder(order(4L, 10L, "CREATED"));
        try {
            service.createFriendPayment(other, new FriendPaymentRequest(4L, "payer"));
            fail("a non-buyer must not request payment for the order");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
        }
        FriendPaymentDto request = service.createFriendPayment(buyer,
                new FriendPaymentRequest(4L, " payer ", "请帮忙"));
        assertEquals("PENDING", request.getStatus());
        try {
            service.decideFriendPayment(buyer,
                    new FriendPaymentDecisionRequest(request.getId(), "ACCEPT", "bad-buyer"));
            fail("the buyer must not decide the payment request");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
        }
        try {
            service.decideFriendPayment(other,
                    new FriendPaymentDecisionRequest(request.getId(), "ACCEPT", "bad-other"));
            fail("an unrelated user must not decide the payment request");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
        }
    }

    @Test
    public void acceptingFriendPaymentDebitsPayerAndRejectsRepeatCharge() throws Exception {
        core.addOrder(order(5L, 10L, "CREATED"));
        FriendPaymentDto request = service.createFriendPayment(buyer,
                new FriendPaymentRequest(5L, "payer"));
        assertEquals("ACCEPTED", service.decideFriendPayment(payer,
                new FriendPaymentDecisionRequest(request.getId(), "ACCEPT", "friend-pay-5"))
                .getStatus());
        assertEquals("PAID", core.getOrder(5L).getStatus());
        assertEquals(new BigDecimal("100.00"), core.getAccount(10L).getBalance());
        assertEquals(new BigDecimal("38.00"), core.getAccount(11L).getBalance());
        assertEquals(9, core.getStock(1L));
        assertEquals(1L, service.getAccountLedger(payer, null).getTotal());
        try {
            service.decideFriendPayment(payer,
                    new FriendPaymentDecisionRequest(request.getId(), "ACCEPT", "friend-pay-5"));
            fail("an accepted request must not charge the payer again");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
        assertEquals(new BigDecimal("38.00"), core.getAccount(11L).getBalance());
        assertEquals(1L, service.getAccountLedger(payer, null).getTotal());
    }

    @Test
    public void refundAfterFriendPaymentCreditsPayerInsteadOfBuyer() throws Exception {
        core.addOrder(order(6L, 10L, "CREATED"));
        FriendPaymentDto request = service.createFriendPayment(buyer,
                new FriendPaymentRequest(6L, "payer"));
        service.decideFriendPayment(payer,
                new FriendPaymentDecisionRequest(request.getId(), "ACCEPT", "friend-pay-6"));
        service.updateOrderStatus(manager, new OrderStatusUpdateRequest(6L, "REFUNDED"));
        assertEquals("REFUNDED", core.getOrder(6L).getStatus());
        assertEquals(new BigDecimal("100.00"), core.getAccount(10L).getBalance());
        assertEquals(new BigDecimal("50.00"), core.getAccount(11L).getBalance());
        assertEquals(10, core.getStock(1L));
        AccountLedgerPage payerLedger = service.getAccountLedger(payer, null);
        assertEquals(2L, payerLedger.getTotal());
        assertEquals("REFUND", payerLedger.getItems().get(0).getTransactionType());
        assertEquals(0L, service.getAccountLedger(buyer, null).getTotal());
    }

    private void assertReviewRejected(ProductReviewWriteRequest request) throws Exception {
        try {
            service.addReview(buyer, request);
            fail("review must be rejected");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
    }

    private static OrderDto order(long id, long user, String status) {
        LocalDateTime time = LocalDateTime.of(2026, 8, 29, 12, 0);
        LocalDateTime paid = "CREATED".equals(status) ? null : time;
        LocalDateTime done = "COMPLETED".equals(status) ? time : null;
        return new OrderDto(id, "ORDER-" + id, user, new BigDecimal("12.00"), status,
                time, paid, null, done, Collections.singletonList(new OrderItemDto(1L,
                        "校园杯", new BigDecimal("12.00"), 1)));
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("review-friend-" + id, id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
