package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.util.Locale;

/** SRTP 审核或撤销状态请求；审核人从 SessionContext 获取。 */
public final class SrtpStatusRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long recordId;
    private final String status;
    private final String remark;

    public SrtpStatusRequest(long recordId, String status, String remark) {
        this.recordId = recordId;
        this.status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
        this.remark = remark;
    }

    public long getRecordId() { return recordId; }
    public long getId() { return recordId; }
    public String getStatus() { return status; }
    public String getRemark() { return remark; }
}
