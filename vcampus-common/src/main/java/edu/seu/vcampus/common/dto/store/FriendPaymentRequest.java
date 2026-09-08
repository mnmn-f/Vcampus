package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 买家发起的一次性好友代付请求。 */
public final class FriendPaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final String friendAccount;
    private final String message;
    public FriendPaymentRequest(long orderId, String friendAccount, String message) {
        this.orderId = orderId; this.friendAccount = friendAccount; this.message = message;
    }
    public FriendPaymentRequest(long orderId, String friendAccount) {
        this(orderId, friendAccount, null);
    }
    public long getOrderId() { return orderId; }
    public String getFriendAccount() { return friendAccount; }
    public String getMessage() { return message; }
}
