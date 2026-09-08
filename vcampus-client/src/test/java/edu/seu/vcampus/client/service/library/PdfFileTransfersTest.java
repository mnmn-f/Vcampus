package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.security.Role;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

/** 大文件传输、并发同名下载、记录重开和账号隔离。 */
public final class PdfFileTransfersTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void roundTripConcurrentDownloadsAndRestartKeepResults() throws Exception {
        Path root = temporary.newFolder("demo").toPath(); DemoPdfStore store = new DemoPdfStore(root);
        ClientSession a = session(10, Role.STUDENT), b = session(11, Role.STUDENT), admin = session(12, Role.LIBRARIAN);
        PdfClientService uploader = new DemoPdfClientService(a, store), downloader = new DemoPdfClientService(b, store), reviewer = new DemoPdfClientService(admin, store);
        Path source = temporary.newFile("test.pdf").toPath(); byte[] bytes = new byte[9 * 1024 * 1024 + 33];
        new java.util.Random(14).nextBytes(bytes); System.arraycopy("%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, bytes, 0, 8); Files.write(source, bytes);
        AtomicLong uploaded = new AtomicLong(); PdfResourceView resource = new PdfFileTransfers(uploader).upload(source, "资源标题", "简介", (n, total) -> uploaded.set(n));
        assertEquals(bytes.length, uploaded.get()); assertEquals("PENDING", resource.getStatus());
        assertEquals(0, downloader.list(new PdfQuery("PUBLIC", null, null, 1, 20)).getTotal());
        reviewer.review(new PdfReviewRequest(resource.getId(), "APPROVED", null));
        Path directory = temporary.newFolder("downloads").toPath();
        CompletableFuture<Path> first = download(downloader, resource.getId(), directory);
        CompletableFuture<Path> second = download(downloader, resource.getId(), directory);
        Path file1 = first.get(), file2 = second.get(); assertNotEquals(file1, file2);
        assertEquals(PdfFileStore.sha256(source), PdfFileStore.sha256(file1)); assertEquals(PdfFileStore.sha256(source), PdfFileStore.sha256(file2));
        PdfClientService restarted = new DemoPdfClientService(b, new DemoPdfStore(root));
        assertEquals(2, restarted.history(new PdfQuery("MINE", null, null, 1, 20)).getTotal());
        assertTrue(restarted.history(new PdfQuery("MINE", null, null, 1, 20)).getItems().stream().allMatch(r -> "COMPLETED".equals(r.getStatus())));
        assertEquals(0, uploader.history(new PdfQuery("MINE", null, null, 1, 20)).getTotal());
        assertEquals(1, uploader.list(new PdfQuery("MINE", null, null, 1, 20)).getTotal());
    }
    @Test public void failedSaveRecordsFailureAndOldSessionCannotContinue() throws Exception {
        DemoPdfStore store = new DemoPdfStore(temporary.newFolder().toPath()); ClientSession student = session(1, Role.STUDENT);
        PdfClientService service = new DemoPdfClientService(student, store); Path source = temporary.newFile("sample.pdf").toPath(); Files.write(source, "%PDF-1.7 test file".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        PdfResourceView r = new PdfFileTransfers(service).upload(source, "标题", "", (n, t) -> { });
        try { new PdfFileTransfers(service).download(r.getId(), temporary.getRoot().toPath().resolve("missing"), (n, t) -> { }); org.junit.Assert.fail(); }
        catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("目录")); }
        assertEquals("FAILED", service.history(new PdfQuery("MINE", null, null, 1, 20)).getItems().get(0).getStatus());
        student.close(); student.open(new LoginResult(2, "other", "其他学生", Role.STUDENT, "other-token"));
        try { service.detail(r.getId()); org.junit.Assert.fail(); } catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("会话")); }
    }
    private static CompletableFuture<Path> download(PdfClientService service, long id, Path directory) {
        return CompletableFuture.supplyAsync(() -> {
            try { return new PdfFileTransfers(service).download(id, directory, (n, t) -> { }); }
            catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        });
    }
    private static ClientSession session(long id, Role role) {
        ClientSession value = new ClientSession(); value.open(new LoginResult(id, "user" + id, "用户" + id, role, "token" + id)); return value;
    }
}
