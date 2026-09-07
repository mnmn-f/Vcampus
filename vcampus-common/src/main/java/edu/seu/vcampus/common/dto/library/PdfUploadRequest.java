package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String title;
    private final String description;
    private final String fileName;
    private final long fileSize;
    private final String sha256;

    public PdfUploadRequest(String title, String description, String fileName, long fileSize, String sha256) {
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
    }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSha256() { return sha256; }
}
