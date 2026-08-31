package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商品、订单等按主键读取或移除时使用的通用请求。 */
public final class StoreIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;

    public StoreIdRequest(long id) { this.id = id; }
    public long getId() { return id; }
    public long getProductId() { return id; }
    public long getOrderId() { return id; }
}
