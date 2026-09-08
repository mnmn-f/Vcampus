package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 一次未归扫描的结果汇总。 */
public final class WarningScanResultDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate scanDate;
    private final int residentsScanned;
    private final int normalCount;
    private final int severeCount;
    private final int exemptCount;

    public WarningScanResultDto(LocalDate scanDate, int residentsScanned, int normalCount,
                                int severeCount, int exemptCount) {
        this.scanDate = scanDate;
        this.residentsScanned = residentsScanned;
        this.normalCount = normalCount;
        this.severeCount = severeCount;
        this.exemptCount = exemptCount;
    }

    public LocalDate getScanDate() { return scanDate; }
    /** 参与扫描的在住学生数。 */
    public int getResidentsScanned() { return residentsScanned; }
    public int getNormalCount() { return normalCount; }
    public int getSevereCount() { return severeCount; }
    public int getExemptCount() { return exemptCount; }
    /** 本次写入的预警条数。 */
    public int getWarningCount() { return normalCount + severeCount + exemptCount; }
}
