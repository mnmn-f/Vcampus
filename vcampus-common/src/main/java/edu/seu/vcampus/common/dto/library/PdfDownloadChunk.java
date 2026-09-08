package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfDownloadChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long downloadId;
    private final long offset;
    private final int length;

    public PdfDownloadChunk(long downloadId, long offset, int length) {
        this.downloadId = downloadId;
        this.offset = offset;
        this.length = length;
    }
    public long getDownloadId() { return downloadId; }
    public long getOffset() { return offset; }
    public int getLength() { return length; }
}
