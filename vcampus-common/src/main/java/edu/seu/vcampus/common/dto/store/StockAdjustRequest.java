package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商店管理员增减商品库存的请求；库存不足时服务端原子拒绝。 */
public final class StockAdjustRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final int delta;
    private final String remark;

    public StockAdjustRequest(long productId, int delta, String remark) {
        this.productId = productId;
        this.delta = delta;
        this.remark = remark;
    }

    public StockAdjustRequest(long productId, int delta) {
        this(productId, delta, null);
    }

    public long getProductId() { return productId; }
    public long getId() { return productId; }
    public int getDelta() { return delta; }
    public String getRemark() { return remark; }
}
