package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.server.store.repository.StoreExperienceRepository;
import java.sql.Connection;
import java.util.List;

/** 商店体验增强 DAO 门面，按数据职责委托给小型 PreparedStatement DAO。 */
public final class MySqlStoreExperienceRepository implements StoreExperienceRepository {
    private final MySqlStoreCatalogRepository catalog = new MySqlStoreCatalogRepository();
    private final MySqlStoreCouponReviewRepository reviews = new MySqlStoreCouponReviewRepository();
    private final MySqlStoreFriendPaymentRepository friends = new MySqlStoreFriendPaymentRepository();

    @Override public StoreCategoryPage listCategories(Connection c, boolean all) { return catalog.categories(c, all); }
    @Override public StoreCategoryDto saveCategory(Connection c, StoreCategoryWriteRequest r) { return catalog.saveCategory(c, r); }
    @Override public PromotionPage listPromotions(Connection c) { return catalog.promotions(c); }
    @Override public PromotionDto savePromotion(Connection c, PromotionWriteRequest r) { return catalog.savePromotion(c, r); }
    @Override public List<PromotionDto> activePromotions(Connection c) { return catalog.activePromotions(c); }
    @Override public CouponPage listCoupons(Connection c, long u) { return reviews.coupons(c, u); }
    @Override public CouponDto claimCoupon(Connection c, long u, CouponClaimRequest r) { return reviews.claim(c, u, r); }
    @Override public CouponDto findCoupon(Connection c, long u, String code, boolean lock) { return reviews.findCoupon(c, u, code, lock); }
    @Override public boolean markCouponUsed(Connection c, long u, String code, long order) { return reviews.markCouponUsed(c, u, code, order); }
    @Override public ProductReviewPage listReviews(Connection c, ProductReviewQuery q) { return reviews.reviews(c, q); }
    @Override public ProductReviewDto addReview(Connection c, long u, ProductReviewWriteRequest r, String name) { return reviews.addReview(c, u, r, name); }
    @Override public FriendPaymentDto createFriendPayment(Connection c, long u, FriendPaymentRequest r) { return friends.create(c, u, r); }
    @Override public FriendPaymentPage listFriendPayments(Connection c, long u, FriendPaymentQuery q) { return friends.list(c, u, q); }
    @Override public FriendPaymentDto findFriendPayment(Connection c, long id, boolean lock) { return friends.find(c, id, lock); }
    @Override public boolean updateFriendPaymentStatus(Connection c, long id, String status) { return friends.updateStatus(c, id, status); }
    @Override public List<StoreSalesTrendDto> findTrend(Connection c, StoreSalesTrendQuery q) { return friends.trend(c, q); }
}
