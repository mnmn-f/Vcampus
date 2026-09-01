package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutLineDto;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.CartLine;
import edu.seu.vcampus.server.store.repository.StoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** 服务端结算预览和确认；最终金额始终从锁定的购物车重算。 */
final class StoreCheckoutService {
    private final StoreRecordRepository core;
    private final StoreExperienceRepository experience;
    private final StoreTransactionRunner transactions;

    StoreCheckoutService(StoreRecordRepository core, StoreExperienceRepository experience,
                         StoreTransactionRunner transactions) {
        this.core = core; this.experience = experience; this.transactions = transactions;
    }

    CheckoutPreviewDto preview(final SessionContext session, final String couponCode)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
        return StoreServiceSupport.inTransaction(transactions, new TransactionWork<CheckoutPreviewDto>() {
            @Override public CheckoutPreviewDto execute(Connection c) throws StoreServiceException {
                return calculate(c, session.getUserId(), couponCode);
            }
        });
    }

    OrderDto confirm(final SessionContext session, final CheckoutConfirmRequest request)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
        final CheckoutConfirmRequest safe = request == null
                ? new CheckoutConfirmRequest(null) : request;
        if (!"SELF".equalsIgnoreCase(safe.getPaymentMode())
                && !"FRIEND".equalsIgnoreCase(safe.getPaymentMode())) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "付款方式不正确");
        }
        return StoreServiceSupport.inTransaction(transactions, new TransactionWork<OrderDto>() {
            @Override public OrderDto execute(Connection c) throws StoreServiceException {
                List<CartLine> lines = core.findCartLines(c, session.getUserId(), true);
                CheckoutPreviewDto pricing = calculate(c, session.getUserId(), safe.getCouponCode());
                if (lines.isEmpty() || !pricing.isStockAvailable()) {
                    throw new StoreServiceException(ResultCodes.CONFLICT, "购物车为空或库存不足");
                }
                BigDecimal total = pricing.getPayable();
                long id = core.insertOrder(c, session.getUserId(), orderNo(), total);
                // 明细保留原价，折扣以订单价格快照记录，避免把促销结果伪装成商品单价。
                core.insertOrderItems(c, id, lines);
                core.updateOrderPricing(c, id, pricing.getSubtotal(), discount(pricing),
                        pricing.getAppliedPromotion(), pricing.getAppliedCoupon(), safe.getPaymentMode());
                core.clearCart(c, session.getUserId());
                if (safe.getCouponCode() != null && !safe.getCouponCode().trim().isEmpty()
                        && !experience.markCouponUsed(c, session.getUserId(), safe.getCouponCode().trim(), id)) {
                    throw new StoreServiceException(ResultCodes.CONFLICT, "优惠券已被使用");
                }
                return core.findOrder(c, id, false);
            }
        });
    }

    private CheckoutPreviewDto calculate(Connection c, long user, String coupon)
            throws StoreServiceException {
        List<CartLine> lines = core.findCartLines(c, user, true);
        BigDecimal subtotal = BigDecimal.ZERO; boolean stock = true;
        List<CheckoutLineDto> result = new ArrayList<CheckoutLineDto>();
        for (CartLine line : lines) {
            StoreOrderService.validateLine(line);
            subtotal = subtotal.add(line.lineAmount());
            stock = stock && line.getStockQty() >= line.getQuantity();
            result.add(new CheckoutLineDto(line.getProductId(), line.getProductName(), line.getQuantity(),
                    line.getUnitPrice(), line.lineAmount(), line.getStockQty()));
        }
        Discount promotion = promotions(c, subtotal, lines);
        BigDecimal couponDiscount = BigDecimal.ZERO; String usedCoupon = null;
        if (coupon != null && !coupon.trim().isEmpty()) {
            CouponDto value = experience.findCoupon(c, user, coupon.trim(), true);
            if (value == null || value.isUsed()) throw new StoreServiceException(ResultCodes.CONFLICT, "优惠券不可用");
            if (value.getExpiresAt() != null && !org.threeten.bp.LocalDateTime.now().isBefore(value.getExpiresAt())) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "优惠券已过期");
            }
            if (value.getThreshold() == null || subtotal.compareTo(value.getThreshold()) >= 0) {
                couponDiscount = value.getDiscountAmount() == null ? BigDecimal.ZERO : value.getDiscountAmount();
                usedCoupon = value.getCode();
            }
        }
        BigDecimal totalDiscount = promotion.amount.add(couponDiscount);
        if (totalDiscount.compareTo(subtotal) > 0) totalDiscount = subtotal;
        return new CheckoutPreviewDto(result, subtotal, promotion.amount, couponDiscount,
                subtotal.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP), promotion.code,
                usedCoupon, stock);
    }

    private Discount promotions(Connection c, BigDecimal subtotal, List<CartLine> lines)
            throws StoreServiceException {
        BigDecimal best = BigDecimal.ZERO; String code = null; BigDecimal stack = BigDecimal.ZERO;
        String stackCodes = null;
        for (PromotionDto p : experience.activePromotions(c)) {
            BigDecimal eligible = eligibleAmount(c, p, lines);
            if (eligible.signum() <= 0 || p.getThreshold() != null && eligible.compareTo(p.getThreshold()) < 0) continue;
            BigDecimal value = p.getValue() == null ? BigDecimal.ZERO : p.getValue();
            if ("PERCENT".equalsIgnoreCase(p.getType())) value = eligible.multiply(value).divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
            if ("THRESHOLD".equalsIgnoreCase(p.getType()) || "FIXED".equalsIgnoreCase(p.getType())) { }
            if (value.compareTo(eligible) > 0) value = eligible;
            if (p.isStackable()) { stack = stack.add(value); stackCodes = join(stackCodes, p.getCode()); }
            else if (value.compareTo(best) > 0) { best = value; code = p.getCode(); }
        }
        if (stack.compareTo(best) > 0) return new Discount(stack.min(subtotal), stackCodes);
        return new Discount(best.min(subtotal), code);
    }

    private BigDecimal eligibleAmount(Connection c, PromotionDto p, List<CartLine> lines) {
        if (p.getProductId() == null && p.getCategoryCode() == null) return total(lines);
        BigDecimal result = BigDecimal.ZERO;
        for (CartLine line : lines) {
            boolean applies = p.getProductId() != null && p.getProductId().longValue() == line.getProductId();
            ProductDto product = core.findProduct(c, line.getProductId(), false);
            applies = applies || p.getCategoryCode() != null && product != null
                    && p.getCategoryCode().equals(product.getCategory());
            if (applies) result = result.add(line.lineAmount());
        }
        return result;
    }

    private static BigDecimal discount(CheckoutPreviewDto v) {
        return v.getPromotionDiscount().add(v.getCouponDiscount());
    }
    private static BigDecimal total(List<CartLine> lines) { BigDecimal value=BigDecimal.ZERO; for(CartLine l:lines)value=value.add(l.lineAmount()); return value.setScale(2,RoundingMode.HALF_UP); }
    private static String join(String previous, String next) { return previous == null ? next : previous + "," + next; }
    private static String orderNo() { return "VC-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8); }
    private static final class Discount { final BigDecimal amount; final String code; Discount(BigDecimal a,String c){amount=a;code=c;} }
}
