package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import java.sql.Connection;
import java.util.List;

/** 商店体验增强数据的独立持久化边界。 */
public interface StoreExperienceRepository {
    StoreCategoryPage listCategories(Connection c, boolean includeInactive);
    StoreCategoryDto saveCategory(Connection c, StoreCategoryWriteRequest request);
    PromotionPage listPromotions(Connection c);
    PromotionDto savePromotion(Connection c, PromotionWriteRequest request);
    List<PromotionDto> activePromotions(Connection c);
    CouponPage listCoupons(Connection c, long userId);
    CouponDto claimCoupon(Connection c, long userId, CouponClaimRequest request);
    CouponDto findCoupon(Connection c, long userId, String code, boolean lock);
    boolean markCouponUsed(Connection c, long userId, String code, long orderId);
    ProductReviewPage listReviews(Connection c, ProductReviewQuery query);
    ProductReviewDto addReview(Connection c, long userId, ProductReviewWriteRequest request,
                               String reviewerName);
    FriendPaymentDto createFriendPayment(Connection c, long buyerId, FriendPaymentRequest request);
    FriendPaymentPage listFriendPayments(Connection c, long userId, FriendPaymentQuery query);
    FriendPaymentDto findFriendPayment(Connection c, long requestId, boolean lock);
    boolean updateFriendPaymentStatus(Connection c, long requestId, String status);
    List<StoreSalesTrendDto> findTrend(Connection c, StoreSalesTrendQuery query);
}
