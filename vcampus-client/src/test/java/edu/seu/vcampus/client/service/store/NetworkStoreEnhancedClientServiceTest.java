package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDecisionRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 增强商店客户端逐命令检查载荷类型、响应强类型和会话令牌。 */
public final class NetworkStoreEnhancedClientServiceTest {
    @Test
    public void enhancedCommandsUseTypedBodiesAndCurrentToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        NetworkStoreClientService service = new NetworkStoreClientService(
                new NetworkClientService(gateway), session);
        gateway.put(StoreCommands.CATEGORY_LIST, new StoreCategoryPage(null, 0L));
        gateway.put(StoreCommands.CHECKOUT_PREVIEW, new CheckoutPreviewDto(
                null, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, null, true));
        gateway.put(StoreCommands.CHECKOUT_CONFIRM, order());
        gateway.put(StoreCommands.PROMOTION_LIST, new PromotionPage(null, 0L));
        gateway.put(StoreCommands.COUPON_MINE, new CouponPage(null, 0L));
        gateway.put(StoreCommands.COUPON_CLAIM, new CouponDto(1L, "C1", "券", null,
                java.math.BigDecimal.ONE, null, true, false));
        gateway.put(StoreCommands.REVIEW_LIST, new ProductReviewPage(null, 0L));
        gateway.put(StoreCommands.REVIEW_CREATE, new ProductReviewDto(1L, 2L, 3L,
                "商品", 5, "好", "匿名用户", null));
        gateway.put(StoreCommands.FRIEND_PAY_MINE, new FriendPaymentPage(null, 0L));
        gateway.put(StoreCommands.FRIEND_PAY_CREATE, friend());
        gateway.put(StoreCommands.FRIEND_PAY_DECIDE, friend());
        gateway.put(StoreCommands.SALES_TREND, new StoreSalesTrendPage(null));

        assertSame(gateway.payloads.get(StoreCommands.CATEGORY_LIST), service.listCategories());
        assertBody(StoreCommands.CATEGORY_LIST, null, gateway);
        assertSame(gateway.payloads.get(StoreCommands.CHECKOUT_PREVIEW),
                service.checkoutPreview(" C1 "));
        assertBody(StoreCommands.CHECKOUT_PREVIEW, CheckoutPreviewRequest.class, gateway);
        assertEquals("C1", ((CheckoutPreviewRequest) gateway.last.getPayload()).getCouponCode());
        assertSame(gateway.payloads.get(StoreCommands.CHECKOUT_CONFIRM),
                service.confirmCheckout(new CheckoutConfirmRequest("C1", "SELF", "k1")));
        assertBody(StoreCommands.CHECKOUT_CONFIRM, CheckoutConfirmRequest.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.PROMOTION_LIST), service.listPromotions());
        assertBody(StoreCommands.PROMOTION_LIST, null, gateway);
        assertSame(gateway.payloads.get(StoreCommands.COUPON_MINE), service.listCoupons());
        assertBody(StoreCommands.COUPON_MINE, null, gateway);
        assertSame(gateway.payloads.get(StoreCommands.COUPON_CLAIM),
                service.claimCoupon(new CouponClaimRequest("C1")));
        assertBody(StoreCommands.COUPON_CLAIM, CouponClaimRequest.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.REVIEW_LIST),
                service.listReviews(new ProductReviewQuery(2L)));
        assertBody(StoreCommands.REVIEW_LIST, ProductReviewQuery.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.REVIEW_CREATE), service.addReview(
                new ProductReviewWriteRequest(3L, 2L, 5, "好")));
        assertBody(StoreCommands.REVIEW_CREATE, ProductReviewWriteRequest.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.FRIEND_PAY_MINE),
                service.listFriendPayments(new FriendPaymentQuery("INBOX")));
        assertBody(StoreCommands.FRIEND_PAY_MINE, FriendPaymentQuery.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.FRIEND_PAY_CREATE),
                service.createFriendPayment(new FriendPaymentRequest(3L, "friend")));
        assertBody(StoreCommands.FRIEND_PAY_CREATE, FriendPaymentRequest.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.FRIEND_PAY_DECIDE), service.decideFriendPayment(
                new FriendPaymentDecisionRequest(4L, "ACCEPT", "k2")));
        assertBody(StoreCommands.FRIEND_PAY_DECIDE, FriendPaymentDecisionRequest.class, gateway);
        assertSame(gateway.payloads.get(StoreCommands.SALES_TREND),
                service.salesTrend(new StoreSalesTrendQuery()));
        assertBody(StoreCommands.SALES_TREND, StoreSalesTrendQuery.class, gateway);
        assertTrue(gateway.allTokensAre("token-7"));
    }

    @Test
    public void unauthenticatedAndWrongResponseTypesAreRejected() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        NetworkStoreClientService service = new NetworkStoreClientService(
                new NetworkClientService(gateway), session);
        gateway.put(StoreCommands.SALES_TREND, new CouponPage(null, 0L));
        try {
            service.salesTrend(new StoreSalesTrendQuery());
            fail("wrong response DTO must be rejected");
        } catch (NetworkClientException ex) {
            assertEquals(ResultCodes.INTERNAL_ERROR, ex.getCode());
        }
        session.close();
        try {
            service.listCoupons();
            fail("closed session must be rejected");
        } catch (NetworkClientException ex) {
            assertEquals(ResultCodes.UNAUTHORIZED, ex.getCode());
        }
    }

    private static void assertBody(String command, Class<?> type, RecordingGateway gateway) {
        assertEquals(command, gateway.last.getCommand());
        if (type == null) assertEquals(null, gateway.last.getPayload());
        else assertTrue(type.isInstance(gateway.last.getPayload()));
        assertEquals("token-7", gateway.last.getSessionToken());
    }

    private static OrderDto order() {
        return new OrderDto(1L, "O1", 7L, java.math.BigDecimal.ONE,
                "CREATED", null, null, null, null,
                Collections.<edu.seu.vcampus.common.dto.store.OrderItemDto>emptyList());
    }

    private static FriendPaymentDto friend() {
        return new FriendPaymentDto(1L, 1L, "O1", 7L, "学生", 8L,
                "好友", java.math.BigDecimal.ONE, "PENDING", null, null, null);
    }

    private static final class RecordingGateway implements ClientGateway {
        private final Map<String, Serializable> payloads = new HashMap<String, Serializable>();
        private final java.util.List<Message> requests = new java.util.ArrayList<Message>();
        private Message last;

        private void put(String command, Serializable value) { payloads.put(command, value); }

        @Override public Message send(Message request) {
            last = request;
            requests.add(request);
            return Message.success(request, payloads.get(request.getCommand()));
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }

        private boolean allTokensAre(String token) {
            for (Message request : requests) if (!token.equals(request.getSessionToken())) return false;
            return true;
        }
    }
}
