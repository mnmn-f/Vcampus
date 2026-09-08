package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** 账单生成请求；房间为空表示对该账期内全部待出账读数批量出账。 */
public final class BillGenerateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long roomId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDateTime dueAt;

    public BillGenerateRequest(Long roomId, LocalDate periodStart, LocalDate periodEnd,
                               LocalDateTime dueAt) {
        this.roomId = roomId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueAt = dueAt;
    }

    /** 只对单个房间出账时填房间编号，批量出账留空。 */
    public Long getRoomId() { return roomId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    /** 缴费截止时间；留空由服务端按账期结束后 15 天计算。 */
    public LocalDateTime getDueAt() { return dueAt; }
}
