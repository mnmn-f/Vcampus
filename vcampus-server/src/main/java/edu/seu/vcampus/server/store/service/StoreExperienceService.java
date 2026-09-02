package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
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
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreIdRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.StoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;
import edu.seu.vcampus.server.store.repository.LedgerRecord;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.math.BigDecimal;
import java.sql.Connection;

/** 商店体验增强业务：分类、优惠券、评价、代付和趋势。 */
final class StoreExperienceService {
    private final StoreRecordRepository core;
    private final StoreExperienceRepository repo;
    private final StoreTransactionRunner transactions;
    private final StoreCheckoutService checkout;

    StoreExperienceService(StoreRecordRepository core, StoreExperienceRepository repo,
                           StoreTransactionRunner transactions) {
        this.core = core; this.repo = repo; this.transactions = transactions;
        this.checkout = new StoreCheckoutService(core, repo, transactions);
    }
    CheckoutAccess checkout() { return new CheckoutAccess(); }
    StoreCategoryPage categories(final SessionContext s) throws StoreServiceException { read(s); return tx(new TransactionWork<StoreCategoryPage>() { public StoreCategoryPage execute(Connection c){return repo.listCategories(c,s.getActiveRole()==edu.seu.vcampus.common.security.Role.STORE_MANAGER);}}); }
    StoreCategoryDto saveCategory(final SessionContext s, final StoreCategoryWriteRequest r) throws StoreServiceException { manage(s); validateCategory(r); return tx(new TransactionWork<StoreCategoryDto>() { public StoreCategoryDto execute(Connection c){return repo.saveCategory(c,r);}}); }
    PromotionPage promotions(final SessionContext s) throws StoreServiceException { manage(s); return tx(new TransactionWork<PromotionPage>() { public PromotionPage execute(Connection c){return repo.listPromotions(c);}}); }
    PromotionDto savePromotion(final SessionContext s, final PromotionWriteRequest r) throws StoreServiceException { manage(s); validatePromotion(r); return tx(new TransactionWork<PromotionDto>() { public PromotionDto execute(Connection c){return repo.savePromotion(c,r);}}); }
    CouponPage coupons(final SessionContext s) throws StoreServiceException { purchase(s); return tx(new TransactionWork<CouponPage>() { public CouponPage execute(Connection c){return repo.listCoupons(c,s.getUserId());}}); }
    CouponDto claim(final SessionContext s, final CouponClaimRequest r) throws StoreServiceException { purchase(s); StoreServiceSupport.required(r==null?null:r.getCode(),"优惠券编码"); return tx(new TransactionWork<CouponDto>() { public CouponDto execute(Connection c)throws StoreServiceException { try{return repo.claimCoupon(c,s.getUserId(),r);}catch(StoreRepositoryException ex){throw new StoreServiceException(ResultCodes.CONFLICT,"优惠券不可领取",ex);}}}); }
    ProductReviewPage reviews(final SessionContext s, final ProductReviewQuery q) throws StoreServiceException { read(s); return tx(new TransactionWork<ProductReviewPage>() { public ProductReviewPage execute(Connection c){return repo.listReviews(c,q);}}); }
    ProductReviewDto addReview(final SessionContext s, final ProductReviewWriteRequest r) throws StoreServiceException { purchase(s); validateReview(r); return tx(new TransactionWork<ProductReviewDto>() { public ProductReviewDto execute(Connection c) throws StoreServiceException { OrderDto o=core.findOrder(c,r.getOrderId(),false); if(o==null||o.getBuyerId()!=s.getUserId()||!"COMPLETED".equalsIgnoreCase(o.getStatus())) throw new StoreServiceException(ResultCodes.CONFLICT,"仅可评价本人已完成订单"); boolean line=false; for(edu.seu.vcampus.common.dto.store.OrderItemDto i:o.getItems())if(i.getProductId()==r.getProductId())line=true; if(!line)throw new StoreServiceException(ResultCodes.NOT_FOUND,"订单中没有该商品"); try{return repo.addReview(c,s.getUserId(),r,"匿名用户");}catch(StoreRepositoryException ex){throw new StoreServiceException(ResultCodes.CONFLICT,"该商品已评价或暂时不能评价",ex);}}}); }
    FriendPaymentDto createFriend(final SessionContext s, final FriendPaymentRequest r) throws StoreServiceException { purchase(s); if(r==null||r.getOrderId()<=0)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"代付参数不正确"); StoreServiceSupport.required(r.getFriendAccount(),"好友账号"); StoreServiceSupport.maxLength(r.getFriendAccount(),128,"好友账号"); StoreServiceSupport.maxLength(r.getMessage(),500,"留言"); return tx(new TransactionWork<FriendPaymentDto>() { public FriendPaymentDto execute(Connection c) throws StoreServiceException { OrderDto o=core.findOrder(c,r.getOrderId(),false); if(o==null||o.getBuyerId()!=s.getUserId())throw new StoreServiceException(ResultCodes.NOT_FOUND,"订单不存在"); if(!"CREATED".equalsIgnoreCase(o.getStatus()))throw new StoreServiceException(ResultCodes.CONFLICT,"仅可为待支付订单请求代付"); try {FriendPaymentDto v=repo.createFriendPayment(c,s.getUserId(),r); if(v.getPayerId()==s.getUserId())throw new StoreServiceException(ResultCodes.CONFLICT,"不能请求自己代付"); return v;}catch(StoreRepositoryException ex){throw new StoreServiceException(ResultCodes.CONFLICT,"该订单已有待处理代付或好友账号不可用",ex);}}}); }
    FriendPaymentPage friendList(final SessionContext s, final FriendPaymentQuery q) throws StoreServiceException { purchase(s); return tx(new TransactionWork<FriendPaymentPage>() { public FriendPaymentPage execute(Connection c){return repo.listFriendPayments(c,s.getUserId(),q);}}); }
    FriendPaymentDto withdrawFriend(final SessionContext s, final StoreIdRequest r) throws StoreServiceException { purchase(s); StoreServiceSupport.positiveId(r==null?0:r.getId(),"代付编号"); return tx(new TransactionWork<FriendPaymentDto>() { public FriendPaymentDto execute(Connection c)throws StoreServiceException { FriendPaymentDto v=repo.findFriendPayment(c,r.getId(),true); if(v==null||v.getBuyerId()!=s.getUserId())throw new StoreServiceException(ResultCodes.NOT_FOUND,"代付请求不存在"); if(!"PENDING".equalsIgnoreCase(v.getStatus())||!repo.updateFriendPaymentStatus(c,v.getId(),"WITHDRAWN"))throw new StoreServiceException(ResultCodes.CONFLICT,"代付请求不可撤回"); return repo.findFriendPayment(c,v.getId(),false); }}); }
    FriendPaymentDto decideFriend(final SessionContext s, final FriendPaymentDecisionRequest r) throws StoreServiceException { purchase(s); if(r==null||r.getRequestId()<=0)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"代付参数不正确"); StoreAccountService.validateKey(r.getIdempotencyKey()); if(!"ACCEPT".equalsIgnoreCase(r.getDecision())&&!"REJECT".equalsIgnoreCase(r.getDecision()))throw new StoreServiceException(ResultCodes.INVALID_INPUT,"代付决定不正确"); return tx(new TransactionWork<FriendPaymentDto>() { public FriendPaymentDto execute(Connection c)throws StoreServiceException { FriendPaymentDto v=repo.findFriendPayment(c,r.getRequestId(),true); if(v==null||v.getPayerId()!=s.getUserId())throw new StoreServiceException(ResultCodes.NOT_FOUND,"代付请求不存在"); if(!"PENDING".equalsIgnoreCase(v.getStatus()))throw new StoreServiceException(ResultCodes.CONFLICT,"代付请求已处理"); if(v.getExpiresAt()!=null&&!org.threeten.bp.LocalDateTime.now().isBefore(v.getExpiresAt())){repo.updateFriendPaymentStatus(c,v.getId(),"EXPIRED");throw new StoreServiceException(ResultCodes.CONFLICT,"代付请求已过期");} OrderDto o=core.findOrder(c,v.getOrderId(),true); if(o==null||!"CREATED".equalsIgnoreCase(o.getStatus())||v.getAmount()==null||v.getAmount().compareTo(o.getTotalAmount())!=0)throw new StoreServiceException(ResultCodes.CONFLICT,"订单金额已变化，不能代付"); if("REJECT".equalsIgnoreCase(r.getDecision())){if(!repo.updateFriendPaymentStatus(c,v.getId(),"REJECTED"))throw new StoreServiceException(ResultCodes.CONFLICT,"代付状态已变化");return repo.findFriendPayment(c,v.getId(),false);} edu.seu.vcampus.common.dto.store.AccountDto a=core.findAccount(c,s.getUserId(),true); if(a==null||a.getBalance().compareTo(v.getAmount())<0)throw new StoreServiceException(ResultCodes.CONFLICT,"付款账户余额不足"); LedgerRecord old=core.findTransactionByKey(c,r.getIdempotencyKey()); if(old!=null)throw new StoreServiceException(ResultCodes.CONFLICT,"幂等键已使用"); for(edu.seu.vcampus.common.dto.store.OrderItemDto item:o.getItems())if(!core.decrementStock(c,item.getProductId(),item.getQuantity()))throw new StoreServiceException(ResultCodes.CONFLICT,"商品库存不足"); BigDecimal after=a.getBalance().subtract(v.getAmount()); if(!core.updateAccountBalance(c,a.getId(),a.getBalance(),after))throw new StoreServiceException(ResultCodes.CONFLICT,"余额已变化，请重试"); core.insertTransaction(c,a.getId(),"PURCHASE",v.getAmount().negate(),a.getBalance(),after,"STORE_ORDER",o.getId(),r.getIdempotencyKey(),s.getUserId(),"好友代付"); core.updateOrderPricing(c,o.getId(),o.getOriginalAmount(),o.getDiscountAmount(),o.getPromotionCode(),o.getCouponCode(),"FRIEND"); if(!core.updateOrderStatus(c,o.getId(),"PAID")||!repo.updateFriendPaymentStatus(c,v.getId(),"ACCEPTED"))throw new StoreServiceException(ResultCodes.CONFLICT,"代付状态已变化"); return repo.findFriendPayment(c,v.getId(),false); }}); }
    StoreSalesTrendPage trend(final SessionContext s, final StoreSalesTrendQuery q) throws StoreServiceException { manageRead(s); final StoreSalesTrendQuery safe=q==null?new StoreSalesTrendQuery():q; return tx(new TransactionWork<StoreSalesTrendPage>() { public StoreSalesTrendPage execute(Connection c){return new StoreSalesTrendPage(repo.findTrend(c,safe));}}); }
    private <T> T tx(TransactionWork<T> w)throws StoreServiceException{return StoreServiceSupport.inTransaction(transactions,w);}
    private static void read(SessionContext s)throws StoreServiceException{StoreServiceSupport.requirePermission(s,Permission.STORE_READ);}
    private static void purchase(SessionContext s)throws StoreServiceException{StoreServiceSupport.requirePermission(s,Permission.STORE_PURCHASE);}
    private static void manage(SessionContext s)throws StoreServiceException{StoreServiceSupport.requirePermission(s,Permission.STORE_MANAGE);}
    private static void manageRead(SessionContext s)throws StoreServiceException{StoreServiceSupport.requirePermission(s,Permission.STORE_SALES_READ);}
    private static void validateCategory(StoreCategoryWriteRequest r)throws StoreServiceException{if(r==null)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"分类参数不正确");StoreServiceSupport.required(r.getCode(),"分类编码");StoreServiceSupport.required(r.getName(),"分类名称");StoreServiceSupport.maxLength(r.getCode(),40,"分类编码");StoreServiceSupport.maxLength(r.getName(),80,"分类名称");}
    private static void validatePromotion(PromotionWriteRequest r)throws StoreServiceException{if(r==null)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"促销参数不正确");StoreServiceSupport.required(r.getCode(),"促销编码");StoreServiceSupport.required(r.getName(),"促销名称");String type=StoreServiceSupport.required(r.getType(),"促销类型");String scope=StoreServiceSupport.required(r.getProductScope(),"适用范围");if(!"THRESHOLD".equalsIgnoreCase(type)&&!"PERCENT".equalsIgnoreCase(type)&&!"FIXED".equalsIgnoreCase(type))throw new StoreServiceException(ResultCodes.INVALID_INPUT,"促销类型不正确");if(!"ALL".equalsIgnoreCase(scope)&&!"PRODUCT".equalsIgnoreCase(scope)&&!"CATEGORY".equalsIgnoreCase(scope))throw new StoreServiceException(ResultCodes.INVALID_INPUT,"适用范围不正确");if(("ALL".equalsIgnoreCase(scope)&& (r.getProductId()!=null||r.getCategoryCode()!=null))||("PRODUCT".equalsIgnoreCase(scope)&&(r.getProductId()==null||r.getCategoryCode()!=null))||("CATEGORY".equalsIgnoreCase(scope)&&(r.getProductId()!=null||r.getCategoryCode()==null)))throw new StoreServiceException(ResultCodes.INVALID_INPUT,"促销适用范围参数不一致");if(r.getProductId()!=null&&r.getProductId().longValue()<=0L)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"商品编号不正确");if(r.getThreshold()!=null)StoreServiceSupport.money(r.getThreshold(),"促销门槛",false);StoreServiceSupport.money(r.getValue(),"优惠值",true);if("PERCENT".equalsIgnoreCase(type)&&r.getValue().compareTo(BigDecimal.valueOf(100L))>0)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"百分比优惠不能超过100");if(r.getStartsAt()==null||(r.getEndsAt()!=null&&!r.getEndsAt().isAfter(r.getStartsAt())))throw new StoreServiceException(ResultCodes.INVALID_INPUT,"促销时间范围不正确");StoreServiceSupport.maxLength(r.getCode(),64,"促销编码");StoreServiceSupport.maxLength(r.getName(),120,"促销名称");StoreServiceSupport.maxLength(r.getCategoryCode(),40,"分类编码");}
    private static void validateReview(ProductReviewWriteRequest r)throws StoreServiceException{if(r==null||r.getOrderId()<=0||r.getProductId()<=0||r.getScore()<1||r.getScore()>5)throw new StoreServiceException(ResultCodes.INVALID_INPUT,"评价参数不正确");StoreServiceSupport.maxLength(r.getContent(),1000,"评价内容");}
    final class CheckoutAccess { CheckoutPreviewDto preview(SessionContext s,String c)throws StoreServiceException{return checkout.preview(s,c);} OrderDto confirm(SessionContext s,CheckoutConfirmRequest r)throws StoreServiceException{return checkout.confirm(s,r);} }
}
