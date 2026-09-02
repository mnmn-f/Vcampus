package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 用户对待执行操作的确认结果。 */
public final class AiActionConfirmation implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long actionId;
    private final boolean agreed;

    public AiActionConfirmation(long actionId, boolean agreed) {
        this.actionId = actionId;
        this.agreed = agreed;
    }

    public long getActionId() { return actionId; }
    public boolean isAgreed() { return agreed; }
}
