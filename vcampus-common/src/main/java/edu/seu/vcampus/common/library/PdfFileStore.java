package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.protocol.command.PdfCommands;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 有界临时上传与不可变 PDF 文件存储；路径只由随机标识/校验和生成。 */
public final class PdfFileStore {
    private final Path root;
    private final Map<String, Upload> uploads = new HashMap<String, Upload>();
    private static final long EXPIRY = 24L * 60 * 60 * 1000;
    public PdfFileStore(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public static void validate(PdfUploadRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()
                || request.getTitle().length() > 240) throw new IllegalArgumentException("资源名称不能为空且不超过 240 字");
        if (request.getDescription() != null && request.getDescription().length() > 4000)
            throw new IllegalArgumentException("资源简介不能超过 4000 字");
        String name = request.getFileName();
        if (name == null || name.length() > 200 || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") || name.startsWith("."))
            throw new IllegalArgumentException("请选择文件名有效的 PDF 文件");
        if (request.getFileSize() < 8 || request.getFileSize() > PdfCommands.MAX_FILE_BYTES)
            throw new IllegalArgumentException("PDF 大小须在 8 字节至 50 MB 之间");
        if (request.getSha256() == null || !request.getSha256().matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("文件校验信息无效");
    }
    public synchronized String begin(long user, PdfUploadRequest request) throws Exception {
        validate(request); Files.createDirectories(root); cleanup();
        long count = uploads.values().stream().filter(value -> value.user == user).count();
        if (count >= 2 || uploads.size() >= 32) throw new IllegalArgumentException("上传任务较多，请完成或取消已有任务后重试");
        String id = UUID.randomUUID().toString(); Path file = root.resolve(id + ".part");
        Files.createFile(file); uploads.put(id, new Upload(user, request, file)); return id;
    }
    public synchronized long append(long user, PdfUploadChunk chunk) throws Exception {
        if (chunk == null) throw new IllegalArgumentException("上传数据不能为空");
        Upload upload = require(user, chunk.getUploadId()); byte[] bytes = chunk.getData();
        if (upload.sealed || bytes == null || bytes.length == 0 || bytes.length > PdfCommands.CHUNK_BYTES
                || chunk.getOffset() < 0 || chunk.getOffset() + bytes.length > upload.request.getFileSize())
            throw new IllegalArgumentException("上传分块信息无效");
        try (RandomAccessFile file = new RandomAccessFile(upload.file.toFile(), "rw")) {
            long length = file.length();
            if (chunk.getOffset() > length) throw new IllegalArgumentException("上传分块顺序错误");
            if (chunk.getOffset() < length) {
                if (chunk.getOffset() + bytes.length > length) throw new IllegalArgumentException("上传分块重叠");
                byte[] existing = new byte[bytes.length]; file.seek(chunk.getOffset()); file.readFully(existing);
                if (!java.util.Arrays.equals(existing, bytes)) throw new IllegalArgumentException("重复上传的分块内容不一致");
                return length;
            }
            file.seek(length); file.write(bytes); return file.length();
        }
    }
    public synchronized PdfUploadRequest seal(long user, String id) throws Exception {
        Upload upload = require(user, id);
        if (!upload.sealed) {
            if (Files.size(upload.file) != upload.request.getFileSize()) throw new IllegalArgumentException("文件尚未上传完整");
            byte[] header = new byte[5];
            try (InputStream input = Files.newInputStream(upload.file)) {
                if (input.read(header) != 5 || !java.util.Arrays.equals(header, new byte[]{37, 80, 68, 70, 45}))
                    throw new IllegalArgumentException("文件内容不是 PDF");
            }
            if (!sha256(upload.file).equals(upload.request.getSha256())) throw new IllegalArgumentException("PDF 校验失败，请重新上传");
            Path target = file(upload.request.getSha256());
            if (Files.exists(target)) Files.delete(upload.file);
            else Files.move(upload.file, target, StandardCopyOption.ATOMIC_MOVE);
            upload.file = target; upload.sealed = true;
        }
        return upload.request;
    }
    public synchronized void complete(long user, String id) { require(user, id); uploads.remove(id); }
    public synchronized void abort(long user, String id) throws Exception {
        Upload upload = require(user, id);
        if (!upload.sealed) Files.deleteIfExists(upload.file); uploads.remove(id);
    }
    public byte[] read(String sha, long offset, int length) throws Exception {
        if (offset < 0 || length < 1 || length > PdfCommands.CHUNK_BYTES) throw new IllegalArgumentException("下载分块信息无效");
        try (RandomAccessFile file = new RandomAccessFile(file(sha).toFile(), "r")) {
            if (offset > file.length()) throw new IllegalArgumentException("下载位置超出文件范围");
            byte[] bytes = new byte[(int) Math.min(length, file.length() - offset)];
            file.seek(offset); file.readFully(bytes); return bytes;
        }
    }
    public Path file(String sha) {
        if (sha == null || !sha.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("文件标识无效");
        return root.resolve(sha + ".pdf");
    }
    public static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] bytes = new byte[65536];
        try (InputStream input = Files.newInputStream(path)) { int count; while ((count = input.read(bytes)) >= 0) digest.update(bytes, 0, count); }
        StringBuilder result = new StringBuilder(); for (byte value : digest.digest()) result.append(String.format("%02x", value & 255)); return result.toString();
    }
    private Upload require(long user, String id) {
        Upload value = uploads.get(id);
        if (value == null || value.user != user || System.currentTimeMillis() - value.created > EXPIRY)
            throw new IllegalArgumentException("上传任务不存在、已过期或无权访问");
        return value;
    }
    private void cleanup() throws Exception {
        long cutoff = System.currentTimeMillis() - EXPIRY;
        uploads.entrySet().removeIf(entry -> entry.getValue().created < cutoff);
        try (java.nio.file.DirectoryStream<Path> files = Files.newDirectoryStream(root, "*.part")) {
            for (Path file : files) if (Files.getLastModifiedTime(file).toMillis() < cutoff) Files.deleteIfExists(file);
        }
    }
    private static final class Upload {
        private final long user; private final PdfUploadRequest request; private Path file;
        private final long created = System.currentTimeMillis(); private boolean sealed;
        private Upload(long user, PdfUploadRequest request, Path file) { this.user = user; this.request = request; this.file = file; }
    }
}
