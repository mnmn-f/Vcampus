package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** 演示模式的本机持久化；不与生产服务端的数据目录混用。 */
public final class DemoPdfStore {
    final PdfFileStore files;
    final List<PdfResourceView> resources = new ArrayList<PdfResourceView>();
    final List<PdfDownloadRecord> downloads = new ArrayList<PdfDownloadRecord>();
    private final Path root;
    private boolean loaded;
    public DemoPdfStore(Path root) { this.root = root; files = new PdfFileStore(root.resolve("files")); }
    synchronized void load() throws Exception {
        if (loaded) return;
        Path source = root.resolve("records.bin");
        if (Files.exists(source)) {
            try (SafeObjectInputStream input = new SafeObjectInputStream(Files.newInputStream(source), 64L * 1024 * 1024)) {
                Object first = input.readObject(), second = input.readObject();
                if (!(first instanceof List) || !(second instanceof List)) throw new java.io.IOException("演示记录格式错误");
                List<PdfResourceView> savedResources = new ArrayList<PdfResourceView>();
                List<PdfDownloadRecord> savedDownloads = new ArrayList<PdfDownloadRecord>();
                for (Object value : (List<?>) first) {
                    if (!(value instanceof PdfResourceView)) throw new java.io.IOException("演示资源记录格式错误");
                    savedResources.add((PdfResourceView) value);
                }
                for (Object value : (List<?>) second) {
                    if (!(value instanceof PdfDownloadRecord)) throw new java.io.IOException("演示下载记录格式错误");
                    PdfDownloadRecord r = (PdfDownloadRecord) value;
                    if ("DOWNLOADING".equals(r.getStatus())) r = progress(r, r.getTransferred(), "FAILED", "上次程序退出时下载尚未完成");
                    savedDownloads.add(r);
                }
                resources.addAll(savedResources); downloads.addAll(savedDownloads);
            }
        }
        loaded = true;
    }
    synchronized void save() throws Exception {
        Files.createDirectories(root); Path temp = Files.createTempFile(root, "records-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temp))) {
                output.writeObject(resources); output.writeObject(downloads);
            }
            Files.move(temp, root.resolve("records.bin"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temp); }
    }
    static PdfDownloadRecord progress(PdfDownloadRecord r, long bytes, String status, String reason) {
        return new PdfDownloadRecord(r.getId(), r.getUserId(), r.getResourceId(), r.getTitle(), r.getFileName(), r.getFileSize(), r.getSha256(),
                bytes, status, r.getStartedAt(), "DOWNLOADING".equals(status) ? 0 : System.currentTimeMillis(), reason);
    }
}
