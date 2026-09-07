package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfDownloadFinish implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long downloadId;
    private final boolean success;
    private final String reason;

    public PdfDownloadFinish(long downloadId, boolean success, String reason) {
        this.downloadId = downloadId;
        this.success = success;
        this.reason = reason;
    }
    public long getDownloadId() { return downloadId; }
    public boolean getSuccess() { return success; }
    public String getReason() { return reason; }
}
