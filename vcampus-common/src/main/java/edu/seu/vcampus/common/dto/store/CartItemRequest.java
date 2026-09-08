package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 加入或修改购物车商品数量的请求。 */
public final class CartItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final int quantity;

    public CartItemRequest(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
