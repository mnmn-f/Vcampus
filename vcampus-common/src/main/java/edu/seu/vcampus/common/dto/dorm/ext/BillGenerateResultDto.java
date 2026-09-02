package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 账单生成结果汇总；notes 逐条说明每个房间的处理情况，便于宿管核对。 */
public final class BillGenerateResultDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int billCount;
    private final int allocationCount;
    private final int skippedCount;
    private final BigDecimal totalAmount;
    private final List<String> notes;

    public BillGenerateResultDto(int billCount, int allocationCount, int skippedCount,
                                 BigDecimal totalAmount, List<String> notes) {
        this.billCount = billCount;
        this.allocationCount = allocationCount;
        this.skippedCount = skippedCount;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        this.notes = notes == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(notes));
    }

    /** 本次新生成的房间账单数。 */
    public int getBillCount() { return billCount; }
    /** 本次新生成的学生分摊记录数。 */
    public int getAllocationCount() { return allocationCount; }
    /** 因无人在住、金额过小或已出账而跳过的读数条数。 */
    public int getSkippedCount() { return skippedCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<String> getNotes() { return notes; }
}
