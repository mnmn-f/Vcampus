package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 借书请求只携带图书编号，借阅人始终来自服务端会话。 */
public final class BorrowRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long bookId;

    public BorrowRequest(long bookId) { this.bookId = bookId; }
    public long getBookId() { return bookId; }
}
