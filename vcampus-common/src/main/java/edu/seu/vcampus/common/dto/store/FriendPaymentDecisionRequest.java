package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 好友接受或拒绝代付请求。 */
public final class FriendPaymentDecisionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long requestId;
    private final String decision;
    private final String idempotencyKey;
    public FriendPaymentDecisionRequest(long requestId, String idempotencyKey) {
        this(requestId, "ACCEPT", idempotencyKey);
    }
    public FriendPaymentDecisionRequest(long requestId, String decision, String idempotencyKey) {
        this.requestId = requestId; this.decision = decision == null ? "ACCEPT" : decision;
        this.idempotencyKey = idempotencyKey;
    }
    public long getRequestId() { return requestId; }
    public String getDecision() { return decision; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
