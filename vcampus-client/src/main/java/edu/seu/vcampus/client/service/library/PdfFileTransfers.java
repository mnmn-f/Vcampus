package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** 在工作线程执行真实文件传输；完整校验后才将临时文件移为最终 PDF。 */
public final class PdfFileTransfers {
    public interface Progress { void update(long transferred, long total); }
    private final PdfClientService service;
    public PdfFileTransfers(PdfClientService service) { this.service = service; }
    public PdfResourceView upload(Path file, String title, String description, Progress progress) throws Exception {
        long size = Files.size(file);
        if (size < 8 || size > PdfCommands.MAX_FILE_BYTES) throw new IllegalArgumentException("PDF 文件须不超过 50 MB");
        PdfUploadRequest request = new PdfUploadRequest(title, description, file.getFileName().toString(), size, PdfFileStore.sha256(file));
        PdfFileStore.validate(request); String id = service.beginUpload(request);
        try {
            byte[] buffer = new byte[PdfCommands.CHUNK_BYTES]; long offset = 0; progress.update(0, size);
            try (InputStream input = Files.newInputStream(file)) {
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (count == 0) continue;
                    long next = service.uploadChunk(new PdfUploadChunk(id, offset, Arrays.copyOf(buffer, count)));
                    if (next != offset + count) throw new java.io.IOException("上传进度不一致，请重试");
                    offset = next; progress.update(offset, size);
                }
            }
            return service.commitUpload(id);
        } catch (Exception ex) {
            try { service.abortUpload(id); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }
    public Path download(long resourceId, Path directory, Progress progress) throws Exception {
        PdfDownloadRecord record = service.beginDownload(resourceId); Path temp = null;
        try {
            if (!Files.isDirectory(directory)) throw new java.io.IOException("下载目录不存在");
            temp = Files.createTempFile(directory, ".vcampus-", ".part"); long offset = 0; progress.update(0, record.getFileSize());
            try (OutputStream output = Files.newOutputStream(temp)) {
                while (offset < record.getFileSize()) {
                    byte[] bytes = service.downloadChunk(new PdfDownloadChunk(record.getId(), offset, PdfCommands.CHUNK_BYTES));
                    if (bytes == null || bytes.length == 0 || bytes.length > PdfCommands.CHUNK_BYTES || offset + bytes.length > record.getFileSize())
                        throw new java.io.IOException("下载分块不完整，请重试");
                    output.write(bytes); offset += bytes.length; progress.update(offset, record.getFileSize());
                }
            }
            if (!PdfFileStore.sha256(temp).equals(record.getSha256())) throw new java.io.IOException("下载文件校验失败，请重试");
            Path destination = publish(temp, directory, record.getFileName()); temp = null;
            try { service.finishDownload(new PdfDownloadFinish(record.getId(), true, null)); }
            catch (Exception ex) { throw new java.io.IOException("文件已保存到 " + destination + "，下载记录同步失败，请刷新记录", ex); }
            return destination;
        } catch (Exception ex) {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            String reason = ex.getMessage() == null ? "下载中断" : ex.getMessage();
            if (reason.length() > 1000) reason = reason.substring(0, 1000);
            try { service.finishDownload(new PdfDownloadFinish(record.getId(), false, reason)); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }
    private static synchronized Path publish(Path temp, Path directory, String name) throws Exception {
        String safe = name == null ? "resource.pdf" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (safe.startsWith(".") || !safe.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) safe = "resource.pdf";
        if (safe.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])\\..*")) safe = "resource-" + safe;
        Path target = directory.resolve(safe); int index = 1;
        while (Files.exists(target)) target = directory.resolve(safe.substring(0, safe.length() - 4) + " (" + index++ + ").pdf");
        // 不覆盖已有文件；同名资源自动编号。
        Files.move(temp, target); return target;
    }
}
