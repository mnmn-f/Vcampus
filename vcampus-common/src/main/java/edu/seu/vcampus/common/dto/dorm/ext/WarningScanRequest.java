package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 触发一次未归扫描；日期留空表示按服务端当天。 */
public final class WarningScanRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate scanDate;

    public WarningScanRequest(LocalDate scanDate) { this.scanDate = scanDate; }

    public LocalDate getScanDate() { return scanDate; }
}
