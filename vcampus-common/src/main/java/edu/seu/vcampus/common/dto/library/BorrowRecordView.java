package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 面向客户端的借阅记录，不暴露数据库连接或密码等内部对象。 */
public final class BorrowRecordView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long bookId;
    private final String bookTitle;
    private final long borrowerUserId;
    private final String borrowerName;
    private final LocalDateTime issuedAt;
    private final LocalDateTime dueAt;
    private final LocalDateTime returnedAt;
    private final String status;
    private final int renewCount;
    private final String remark;

    public BorrowRecordView(long id, long bookId, String bookTitle,
                            long borrowerUserId, String borrowerName,
                            LocalDateTime issuedAt, LocalDateTime dueAt,
                            LocalDateTime returnedAt, String status,
                            int renewCount, String remark) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowerUserId = borrowerUserId;
        this.borrowerName = borrowerName;
        this.issuedAt = issuedAt;
        this.dueAt = dueAt;
        this.returnedAt = returnedAt;
        this.status = status;
        this.renewCount = renewCount;
        this.remark = remark;
    }

    public long getId() { return id; }
    public long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public long getBorrowerUserId() { return borrowerUserId; }
    public String getBorrowerName() { return borrowerName; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public String getStatus() { return status; }
    public int getRenewCount() { return renewCount; }
    public String getRemark() { return remark; }
}
