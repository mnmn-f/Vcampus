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
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.threeten.bp.LocalDateTime;

/** 无数据库测试用的商店体验仓储；状态由调用方在同一线程安全事务中保护。 */
public final class InMemoryStoreExperienceRepository implements StoreExperienceRepository {
    private final StoreRecordRepository core;
    private final Map<Long, StoreCategoryDto> categories = new LinkedHashMap<Long, StoreCategoryDto>();
    private final Map<Long, PromotionDto> promotions = new LinkedHashMap<Long, PromotionDto>();
    private final Map<String, CouponDto> coupons = new LinkedHashMap<String, CouponDto>();
    private final Map<Long, List<String>> claimed = new LinkedHashMap<Long, List<String>>();
    private final Map<Long, List<String>> used = new LinkedHashMap<Long, List<String>>();
    private final Map<Long, ProductReviewDto> reviews = new LinkedHashMap<Long, ProductReviewDto>();
    private final Map<Long, FriendPaymentDto> friends = new LinkedHashMap<Long, FriendPaymentDto>();
    private final Map<String, Long> friendAccounts = new LinkedHashMap<String, Long>();
    private long sequence = 1L;

    public InMemoryStoreExperienceRepository(StoreRecordRepository core) { this.core = core; }
    public InMemoryStoreExperienceRepository() { this(null); }
    public void addCoupon(CouponDto coupon) { coupons.put(coupon.getCode(), coupon); }
    public void addPromotion(PromotionDto promotion) { promotions.put(promotion.getId(), promotion); }
    public void addCategory(StoreCategoryDto category) { categories.put(category.getId(), category); }
    /** 为内存测试提供账号到用户 id 的最小映射；生产 MySQL 由 users 表解析。 */
    public void addFriendAccount(String account, long userId) { friendAccounts.put(normalize(account), userId); }

    @Override public StoreCategoryPage listCategories(Connection c, boolean all) {
        List<StoreCategoryDto> rows = new ArrayList<StoreCategoryDto>();
        for (StoreCategoryDto v : categories.values()) if (all || v.isActive()) rows.add(v);
        return new StoreCategoryPage(rows, rows.size());
    }
    @Override public StoreCategoryDto saveCategory(Connection c, StoreCategoryWriteRequest r) {
        long id = r.getId() > 0L ? r.getId() : next();
        StoreCategoryDto value = new StoreCategoryDto(id, r.getCode(), r.getName(), r.isActive()); categories.put(id, value); return value;
    }
    @Override public PromotionPage listPromotions(Connection c) { return new PromotionPage(new ArrayList<PromotionDto>(promotions.values()), promotions.size()); }
    @Override public PromotionDto savePromotion(Connection c, PromotionWriteRequest r) {
        long id = r.getId() > 0L ? r.getId() : next();
        PromotionDto v = new PromotionDto(id,r.getCode(),r.getName(),r.getType(),r.getThreshold(),r.getValue(),r.getProductScope(),r.getProductId(),r.getCategoryCode(),r.getStartsAt(),r.getEndsAt(),r.isStackable(),r.isActive()); promotions.put(id,v); return v;
    }
    @Override public List<PromotionDto> activePromotions(Connection c) { List<PromotionDto> r=new ArrayList<PromotionDto>(); LocalDateTime now=LocalDateTime.now(); for(PromotionDto p:promotions.values()) if(p.isActive()&&(p.getStartsAt()==null||!now.isBefore(p.getStartsAt()))&&(p.getEndsAt()==null||now.isBefore(p.getEndsAt()))) r.add(p); return r; }
    @Override public CouponPage listCoupons(Connection c,long user) { List<CouponDto> r=new ArrayList<CouponDto>(); List<String> have=claimed.get(user); List<String> spent=used.get(user); for(CouponDto v:coupons.values()) r.add(new CouponDto(v.getId(),v.getCode(),v.getName(),v.getThreshold(),v.getDiscountAmount(),v.getExpiresAt(),have!=null&&have.contains(v.getCode()),spent!=null&&spent.contains(v.getCode()))); return new CouponPage(r,r.size()); }
    @Override public CouponDto claimCoupon(Connection c,long user,CouponClaimRequest r) { String code=normalize(r==null?null:r.getCode()); CouponDto v=findTemplate(code); if(v==null)throw new StoreRepositoryException("优惠券不存在"); List<String> have=claimed.get(user); if(have==null){have=new ArrayList<String>();claimed.put(user,have);} if(have.contains(v.getCode()))throw new StoreRepositoryException("优惠券已领取");have.add(v.getCode());return coupon(v,user); }
    @Override public CouponDto findCoupon(Connection c,long user,String code,boolean lock) { List<String> have=claimed.get(user); String normalized=normalize(code); CouponDto v=findTemplate(normalized); return v==null||have==null||!have.contains(v.getCode())?null:coupon(v,user); }
    @Override public boolean markCouponUsed(Connection c,long user,String code,long order) { CouponDto v=findCoupon(c,user,code,true); if(v==null||v.isUsed())return false; List<String> spent=used.get(user); if(spent==null){spent=new ArrayList<String>();used.put(user,spent);} if(spent.contains(v.getCode()))return false; spent.add(v.getCode()); return true; }
    @Override public ProductReviewPage listReviews(Connection c,ProductReviewQuery q) { List<ProductReviewDto> r=new ArrayList<ProductReviewDto>(); for(ProductReviewDto v:reviews.values()) if(q==null||q.getProductId()<=0L||v.getProductId()==q.getProductId())r.add(v);return new ProductReviewPage(r,r.size()); }
    @Override public ProductReviewDto addReview(Connection c,long user,ProductReviewWriteRequest r,String name) { for(ProductReviewDto old:reviews.values()) if(old.getOrderId()==r.getOrderId()&&old.getProductId()==r.getProductId()) throw new StoreRepositoryException("该商品已评价"); ProductReviewDto v=new ProductReviewDto(next(),r.getProductId(),r.getOrderId(),"商品",r.getScore(),r.getContent(),name,LocalDateTime.now());reviews.put(v.getId(),v);refreshRating(c,r.getProductId());return v; }
    @Override public FriendPaymentDto createFriendPayment(Connection c,long user,FriendPaymentRequest r) { if(core==null)throw new StoreRepositoryException("内存订单仓储未配置"); edu.seu.vcampus.common.dto.store.OrderDto o=core.findOrder(c,r.getOrderId(),false);if(o==null||!"CREATED".equalsIgnoreCase(o.getStatus()))throw new StoreRepositoryException("订单不存在或不可代付"); String account=normalize(r.getFriendAccount()); Long payer=friendAccounts.get(account); if(payer==null)throw new StoreRepositoryException("好友账号不存在"); if(payer.longValue()==user)throw new StoreRepositoryException("不能请求自己代付"); for(FriendPaymentDto old:friends.values())if(old.getOrderId()==o.getId()&&"PENDING".equalsIgnoreCase(old.getStatus()))throw new StoreRepositoryException("订单已有待处理代付"); FriendPaymentDto v=new FriendPaymentDto(next(),o.getId(),o.getOrderNo(),o.getBuyerId(),"买家",payer.longValue(),account,o.getTotalAmount(),"PENDING",r.getMessage(),LocalDateTime.now().plusHours(48L),LocalDateTime.now());friends.put(v.getId(),v);return v; }
    @Override public FriendPaymentPage listFriendPayments(Connection c,long user,FriendPaymentQuery q) { List<FriendPaymentDto> r=new ArrayList<FriendPaymentDto>(); boolean mine=q!=null&&"MINE".equalsIgnoreCase(q.getScope());for(FriendPaymentDto v:friends.values())if(mine?v.getBuyerId()==user:v.getPayerId()==user)r.add(expire(v));return new FriendPaymentPage(r,r.size()); }
    @Override public FriendPaymentDto findFriendPayment(Connection c,long id,boolean lock){FriendPaymentDto v=friends.get(id);return v==null?null:expire(v);}
    @Override public boolean updateFriendPaymentStatus(Connection c,long id,String status){FriendPaymentDto old=friends.get(id);if(old==null||!"PENDING".equals(old.getStatus()))return false;friends.put(id,new FriendPaymentDto(old.getId(),old.getOrderId(),old.getOrderNo(),old.getBuyerId(),old.getBuyerName(),old.getPayerId(),old.getPayerName(),old.getAmount(),status,old.getMessage(),old.getExpiresAt(),old.getCreatedAt()));return true;}
    @Override public List<StoreSalesTrendDto> findTrend(Connection c,StoreSalesTrendQuery q){return core instanceof InMemoryStoreRecordRepository?((InMemoryStoreRecordRepository)core).findTrend(q):new ArrayList<StoreSalesTrendDto>();}
    private CouponDto findTemplate(String code){return code==null?null:coupons.get(code);}
    private CouponDto coupon(CouponDto v,long user){List<String> spent=used.get(user);return new CouponDto(v.getId(),v.getCode(),v.getName(),v.getThreshold(),v.getDiscountAmount(),v.getExpiresAt(),true,spent!=null&&spent.contains(v.getCode()));}
    private void refreshRating(Connection c,long productId){long sum=0,count=0;for(ProductReviewDto v:reviews.values())if(v.getProductId()==productId){sum+=v.getScore();count++;}if(core!=null&&count>0)core.updateRating(c,productId,BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count),2,java.math.RoundingMode.HALF_UP),count);}
    private FriendPaymentDto expire(FriendPaymentDto value){if("PENDING".equalsIgnoreCase(value.getStatus())&&value.getExpiresAt()!=null&&!LocalDateTime.now().isBefore(value.getExpiresAt()))return new FriendPaymentDto(value.getId(),value.getOrderId(),value.getOrderNo(),value.getBuyerId(),value.getBuyerName(),value.getPayerId(),value.getPayerName(),value.getAmount(),"EXPIRED",value.getMessage(),value.getExpiresAt(),value.getCreatedAt());return value;}
    private static String normalize(String code){return code==null?null:code.trim();}
    private long next(){return sequence++;}
}
