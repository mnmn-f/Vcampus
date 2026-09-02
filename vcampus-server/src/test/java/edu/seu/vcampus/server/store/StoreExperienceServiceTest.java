package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import edu.seu.vcampus.common.protocol.ResultCodes;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.EnumSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 商店增强功能的结算、分类、优惠券和规则测试。 */
public final class StoreExperienceServiceTest {
    private InMemoryStoreRecordRepository core;
    private InMemoryStoreExperienceRepository experience;
    private StoreService service;
    private SessionContext student;

    @Before public void setUp() {
        core = new InMemoryStoreRecordRepository(); experience = new InMemoryStoreExperienceRepository(core);
        core.addProduct(new ProductDto(1L,"SKU-1","校园杯","CULTURE",null,new BigDecimal("20.00"),5,"ON_SALE"));
        core.addAccount(new AccountDto(1L,10L,new BigDecimal("100.00"),"ACTIVE"));
        experience.addCategory(new StoreCategoryDto(1L,"CULTURE","校园文创",true));
        experience.addPromotion(new PromotionDto(1L,"P-5","满减","THRESHOLD",new BigDecimal("10.00"),new BigDecimal("5.00"),"ALL",null,null,null,null,false,true));
        experience.addCoupon(new CouponDto(1L,"C-2","券",new BigDecimal("10.00"),new BigDecimal("2.00"),null,false,false));
        service = new StoreService(core,new InMemoryStoreTransactionRunner(core),experience);
        student = new SessionContext("s",10L,"student","学生",EnumSet.of(Role.STUDENT),Role.STUDENT);
    }

    @Test public void serverCalculatesCheckoutDiscountAndSnapshotsIt() throws Exception {
        service.claimCoupon(student,new edu.seu.vcampus.common.dto.store.CouponClaimRequest("C-2"));
        service.addCartItem(student,new CartItemRequest(1L,1));
        CheckoutPreviewDto preview=service.checkoutPreview(student,"C-2");
        assertEquals(new BigDecimal("20.00"),preview.getSubtotal());
        assertEquals(new BigDecimal("7.00"),preview.getPromotionDiscount().add(preview.getCouponDiscount()));
        assertEquals(new BigDecimal("13.00"),preview.getPayable());
        assertEquals(new BigDecimal("13.00"),service.confirmCheckout(student,new CheckoutConfirmRequest("C-2")).getTotalAmount());
    }

    @Test public void categoryListAndCouponCannotBeClaimedTwice() throws Exception {
        assertEquals(1L,service.listCategories(student).getTotal());
        service.claimCoupon(student,new edu.seu.vcampus.common.dto.store.CouponClaimRequest("C-2"));
        try { service.claimCoupon(student,new edu.seu.vcampus.common.dto.store.CouponClaimRequest("C-2")); assertTrue(false); }
        catch (StoreServiceException expected) { assertEquals(ResultCodes.CONFLICT, expected.getResultCode()); }
    }
}
