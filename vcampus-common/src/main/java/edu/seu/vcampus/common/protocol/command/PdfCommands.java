package edu.seu.vcampus.common.protocol.command;

/** PDF 文件协议；字节始终分块传输，继续使用原会话鉴权。 */
public final class PdfCommands {
    private PdfCommands() { }
    public static final String LIST = "library.pdf.list";
    public static final String DETAIL = "library.pdf.detail";
    public static final String UPLOAD_BEGIN = "library.pdf.upload_begin";
    public static final String UPLOAD_CHUNK = "library.pdf.upload_chunk";
    public static final String UPLOAD_COMMIT = "library.pdf.upload_commit";
    public static final String UPLOAD_ABORT = "library.pdf.upload_abort";
    public static final String REVIEW = "library.pdf.review";
    public static final String DOWNLOAD_BEGIN = "library.pdf.download_begin";
    public static final String DOWNLOAD_CHUNK = "library.pdf.download_chunk";
    public static final String DOWNLOAD_FINISH = "library.pdf.download_finish";
    public static final String DOWNLOAD_HISTORY = "library.pdf.download_history";

    public static final int CHUNK_BYTES = 128 * 1024;
    public static final long MAX_FILE_BYTES = 50L * 1024 * 1024;
}
