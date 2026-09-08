package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfUploadId implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String uploadId;

    public PdfUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
    public String getUploadId() { return uploadId; }
}
