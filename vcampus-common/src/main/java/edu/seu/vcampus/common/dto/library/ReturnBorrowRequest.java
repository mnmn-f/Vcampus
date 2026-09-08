package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 还书请求只携带借阅记录编号。 */
public final class ReturnBorrowRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long borrowRecordId;

    public ReturnBorrowRequest(long borrowRecordId) {
        this.borrowRecordId = borrowRecordId;
    }

    public long getBorrowRecordId() { return borrowRecordId; }
}
