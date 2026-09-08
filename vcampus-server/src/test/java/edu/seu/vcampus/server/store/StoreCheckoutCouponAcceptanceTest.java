package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 结算确认只接受服务端重算结果，优惠券确认后只能消费一次。 */
public final class StoreCheckoutCouponAcceptanceTest {
    private InMemoryStoreRecordRepository core;
    private InMemoryStoreExperienceRepository experience;
    private StoreService service;
    private SessionContext buyer;
    private SessionContext manager;

    @Before
    public void setUp() {
        core = new InMemoryStoreRecordRepository();
        experience = new InMemoryStoreExperienceRepository(core);
        core.addProduct(new ProductDto(1L, "SKU-1", "校园杯", "CULTURE", null,
                new BigDecimal("20.00"), 5, "ON_SALE"));
        core.addAccount(new AccountDto(1L, 10L, new BigDecimal("100.00"), "ACTIVE"));
        experience.addCoupon(new CouponDto(1L, "C-2", "满减券", new BigDecimal("10.00"),
                new BigDecimal("2.00"), null, false, false));
        service = new StoreService(core, new InMemoryStoreTransactionRunner(core), experience);
        buyer = session(10L, Role.STUDENT);
        manager = session(99L, Role.STORE_MANAGER);
    }

    @Test
    public void confirmationRecalculatesCartAfterPreviewPriceChanges() throws Exception {
        service.addCartItem(buyer, new CartItemRequest(1L, 1));
        CheckoutPreviewDto preview = service.checkoutPreview(buyer, null);
        assertEquals(new BigDecimal("20.00"), preview.getPayable());

        service.updateProduct(manager, new ProductWriteRequest(1L, "SKU-1", "校园杯",
                "CULTURE", null, new BigDecimal("30.00"), 5, "ON_SALE"));
        edu.seu.vcampus.common.dto.store.OrderDto order = service.confirmCheckout(buyer,
                new CheckoutConfirmRequest(null, "SELF", "client-supplied-key"));
        assertEquals(new BigDecimal("30.00"), order.getOriginalAmount());
        assertEquals(new BigDecimal("30.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("30.00"), order.getItems().get(0).getUnitPrice());
        assertEquals(0L, service.getCart(buyer).getItems().size());
    }

    @Test
    public void confirmedCouponIsMarkedUsedAndCannotBeAppliedAgain() throws Exception {
        service.claimCoupon(buyer, new CouponClaimRequest("C-2"));
        service.addCartItem(buyer, new CartItemRequest(1L, 1));
        CheckoutPreviewDto preview = service.checkoutPreview(buyer, "C-2");
        assertEquals("C-2", preview.getAppliedCoupon());
        assertEquals(new BigDecimal("18.00"), preview.getPayable());

        edu.seu.vcampus.common.dto.store.OrderDto first = service.confirmCheckout(buyer,
                new CheckoutConfirmRequest("C-2", "SELF", "checkout-1"));
        assertEquals(new BigDecimal("18.00"), first.getTotalAmount());
        assertTrue(service.listCoupons(buyer).getItems().get(0).isUsed());

        service.addCartItem(buyer, new CartItemRequest(1L, 1));
        try {
            service.checkoutPreview(buyer, "C-2");
            fail("a used coupon must not be previewed as available");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("checkout-" + id, id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
