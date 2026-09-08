package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfUploadChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String uploadId;
    private final long offset;
    private final byte[] data;

    public PdfUploadChunk(String uploadId, long offset, byte[] data) {
        this.uploadId = uploadId;
        this.offset = offset;
        this.data = data == null ? null : data.clone();
    }
    public String getUploadId() { return uploadId; }
    public long getOffset() { return offset; }
    public byte[] getData() { return data == null ? null : data.clone(); }
}
