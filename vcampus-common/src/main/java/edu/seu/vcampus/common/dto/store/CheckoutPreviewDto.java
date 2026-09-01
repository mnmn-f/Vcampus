package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 服务端计算的购物车结算预览，客户端不得自行覆盖金额。 */
public final class CheckoutPreviewDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<CheckoutLineDto> lines;
    private final BigDecimal subtotal;
    private final BigDecimal promotionDiscount;
    private final BigDecimal couponDiscount;
    private final BigDecimal payable;
    private final String appliedPromotion;
    private final String appliedCoupon;
    private final boolean stockAvailable;

    public CheckoutPreviewDto(List<CheckoutLineDto> lines, BigDecimal subtotal,
                              BigDecimal promotionDiscount, BigDecimal couponDiscount,
                              BigDecimal payable, String appliedPromotion,
                              String appliedCoupon, boolean stockAvailable) {
        this.lines = Collections.unmodifiableList(new ArrayList<CheckoutLineDto>(
                lines == null ? Collections.<CheckoutLineDto>emptyList() : lines));
        this.subtotal = subtotal;
        this.promotionDiscount = promotionDiscount;
        this.couponDiscount = couponDiscount;
        this.payable = payable;
        this.appliedPromotion = appliedPromotion;
        this.appliedCoupon = appliedCoupon;
        this.stockAvailable = stockAvailable;
    }
    public List<CheckoutLineDto> getLines() { return lines; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getPromotionDiscount() { return promotionDiscount; }
    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public BigDecimal getPayable() { return payable; }
    public BigDecimal getTotalAmount() { return payable; }
    public String getAppliedPromotion() { return appliedPromotion; }
    public String getAppliedCoupon() { return appliedCoupon; }
    public boolean isStockAvailable() { return stockAvailable; }
}
