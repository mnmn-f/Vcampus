package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfUploadId;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.library.registry.PdfCommandRegistry;
import edu.seu.vcampus.server.library.repository.InMemoryPdfRepository;
import edu.seu.vcampus.server.library.service.PdfLibraryService;
import edu.seu.vcampus.server.network.TcpServer;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

/** 真实 TCP + 服务端鉴权：两名学生、管理员、审核与超 8 MB 文件往返。 */
public final class PdfWorkflowTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private TcpServer server; private SessionContext a, b, admin; private CommandRouter router;
    @Before public void setUp() throws Exception {
        SessionManager sessions = new SessionManager();
        a = sessions.createSession(1, "student_a", "学生甲", Collections.singleton(Role.STUDENT), Role.STUDENT);
        b = sessions.createSession(2, "student_b", "学生乙", Collections.singleton(Role.STUDENT), Role.STUDENT);
        admin = sessions.createSession(3, "librarian", "管理员", Collections.singleton(Role.LIBRARIAN), Role.LIBRARIAN);
        PdfLibraryService service = new PdfLibraryService(new InMemoryPdfRepository(), new PdfFileStore(temporary.newFolder("files").toPath()), null);
        router = PdfCommandRegistry.register(new CommandRouter(sessions), service);
        server = new TcpServer(0, 4, router); server.startAsync();
    }
    @After public void stop() { if (server != null) server.stop(); }
    @Test public void approvalAndLargeTcpDownloadPreserveExactBytesAndHistory() throws Exception {
        byte[] bytes = pdf(9 * 1024 * 1024 + 31); PdfResourceView r = upload(bytes);
        assertEquals("PENDING", r.getStatus());
        assertEquals(ResultCodes.FORBIDDEN, call(b, PdfCommands.DETAIL, new LibraryIdRequest(r.getId())).getResultCode());
        assertEquals(ResultCodes.FORBIDDEN, call(b, PdfCommands.DOWNLOAD_BEGIN, new LibraryIdRequest(r.getId())).getResultCode());
        PageResult<?> publicBefore = (PageResult<?>) ok(b, PdfCommands.LIST, new PdfQuery("PUBLIC", null, "PENDING", 1, 20));
        assertEquals(0, publicBefore.getTotal());
        assertEquals(ResultCodes.FORBIDDEN, call(a, PdfCommands.REVIEW, new PdfReviewRequest(r.getId(), "APPROVED", null)).getResultCode());
        ok(admin, PdfCommands.REVIEW, new PdfReviewRequest(r.getId(), "APPROVED", null));
        assertEquals(1, ((PageResult<?>) ok(b, PdfCommands.LIST, new PdfQuery("PUBLIC", null, null, 1, 20))).getTotal());
        PdfDownloadRecord record = (PdfDownloadRecord) ok(b, PdfCommands.DOWNLOAD_BEGIN, new LibraryIdRequest(r.getId()));
        assertEquals(ResultCodes.FORBIDDEN, call(a, PdfCommands.DOWNLOAD_CHUNK, new PdfDownloadChunk(record.getId(), 0, 10)).getResultCode());
        assertEquals(ResultCodes.INVALID_INPUT, call(b, PdfCommands.DOWNLOAD_FINISH, new PdfDownloadFinish(record.getId(), true, null)).getResultCode());
        java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
        for (int offset = 0; offset < bytes.length;) {
            byte[] chunk = (byte[]) ok(b, PdfCommands.DOWNLOAD_CHUNK, new PdfDownloadChunk(record.getId(), offset, PdfCommands.CHUNK_BYTES));
            result.write(chunk); offset += chunk.length;
        }
        assertArrayEquals(bytes, result.toByteArray());
        PdfDownloadRecord finished = (PdfDownloadRecord) ok(b, PdfCommands.DOWNLOAD_FINISH, new PdfDownloadFinish(record.getId(), true, null));
        assertEquals("COMPLETED", finished.getStatus()); assertEquals(bytes.length, finished.getTransferred());
        assertEquals(1, ((PageResult<?>) ok(b, PdfCommands.DOWNLOAD_HISTORY, new PdfQuery("MINE", null, null, 1, 20))).getTotal());
        assertEquals(0, ((PageResult<?>) ok(a, PdfCommands.DOWNLOAD_HISTORY, new PdfQuery("MINE", null, null, 1, 20))).getTotal());
        PdfDownloadRecord started = (PdfDownloadRecord) ok(b, PdfCommands.DOWNLOAD_BEGIN, new LibraryIdRequest(r.getId()));
        ok(admin, PdfCommands.REVIEW, new PdfReviewRequest(r.getId(), "INACTIVE", null));
        assertEquals(ResultCodes.FORBIDDEN, call(b, PdfCommands.DOWNLOAD_CHUNK, new PdfDownloadChunk(started.getId(), 0, 10)).getResultCode());
    }
    @Test public void rejectionRequiresReasonAndRemainsPrivate() throws Exception {
        PdfResourceView r = upload(pdf(1024));
        assertEquals(ResultCodes.INVALID_INPUT, call(admin, PdfCommands.REVIEW, new PdfReviewRequest(r.getId(), "REJECTED", " ")).getResultCode());
        ok(admin, PdfCommands.REVIEW, new PdfReviewRequest(r.getId(), "REJECTED", "请补充完整目录"));
        PdfResourceView mine = (PdfResourceView) ok(a, PdfCommands.DETAIL, new LibraryIdRequest(r.getId()));
        assertEquals("请补充完整目录", mine.getRejectionReason()); assertEquals(admin.getUserId(), mine.getReviewerId());
        assertEquals(ResultCodes.FORBIDDEN, call(b, PdfCommands.DETAIL, new LibraryIdRequest(r.getId())).getResultCode());
        assertEquals(ResultCodes.UNAUTHORIZED, call(null, PdfCommands.LIST, new PdfQuery("PUBLIC", null, null, 1, 20)).getResultCode());
        assertEquals(ResultCodes.FORBIDDEN, call(a, PdfCommands.LIST, new PdfQuery("ADMIN", null, null, 1, 20)).getResultCode());
    }
    @Test public void invalidUploadsAndCrossUserChunksAreRejected() throws Exception {
        byte[] data = pdf(100); Path file = temporary.newFile().toPath(); Files.write(file, data); String sha = PdfFileStore.sha256(file);
        assertEquals(ResultCodes.INVALID_INPUT, call(a, PdfCommands.UPLOAD_BEGIN, new PdfUploadRequest("标题", "", "../a.pdf", data.length, sha)).getResultCode());
        String id = (String) ok(a, PdfCommands.UPLOAD_BEGIN, new PdfUploadRequest("标题", "", "a.pdf", data.length, sha));
        assertEquals(ResultCodes.INVALID_INPUT, call(b, PdfCommands.UPLOAD_CHUNK, new PdfUploadChunk(id, 0, data)).getResultCode());
        assertEquals(ResultCodes.INVALID_INPUT, call(a, PdfCommands.UPLOAD_COMMIT, new PdfUploadId(id)).getResultCode());
        ok(a, PdfCommands.UPLOAD_CHUNK, new PdfUploadChunk(id, 0, data));
        ok(a, PdfCommands.UPLOAD_CHUNK, new PdfUploadChunk(id, 0, data)); // 重复分块幂等。
        PdfResourceView committed = (PdfResourceView) ok(a, PdfCommands.UPLOAD_COMMIT, new PdfUploadId(id)); assertEquals("PENDING", committed.getStatus());
        byte[] invalid = "not a PDF file".getBytes(java.nio.charset.StandardCharsets.UTF_8); Files.write(file, invalid);
        String bad = (String) ok(a, PdfCommands.UPLOAD_BEGIN, new PdfUploadRequest("假文件", "", "bad.pdf", invalid.length, PdfFileStore.sha256(file)));
        ok(a, PdfCommands.UPLOAD_CHUNK, new PdfUploadChunk(bad, 0, invalid));
        assertEquals(ResultCodes.INVALID_INPUT, call(a, PdfCommands.UPLOAD_COMMIT, new PdfUploadId(bad)).getResultCode());
        ok(a, PdfCommands.UPLOAD_ABORT, new PdfUploadId(bad));
    }
    private PdfResourceView upload(byte[] bytes) throws Exception {
        Path path = temporary.newFile().toPath(); Files.write(path, bytes);
        String id = (String) ok(a, PdfCommands.UPLOAD_BEGIN, new PdfUploadRequest("测试 PDF", "资料说明", "资料.pdf", bytes.length, PdfFileStore.sha256(path)));
        for (int i = 0; i < bytes.length; i += PdfCommands.CHUNK_BYTES)
            ok(a, PdfCommands.UPLOAD_CHUNK, new PdfUploadChunk(id, i, Arrays.copyOfRange(bytes, i, Math.min(bytes.length, i + PdfCommands.CHUNK_BYTES))));
        return (PdfResourceView) ok(a, PdfCommands.UPLOAD_COMMIT, new PdfUploadId(id));
    }
    private Object ok(SessionContext session, String command, Serializable payload) throws Exception {
        Message result = call(session, command, payload); assertTrue(result.getResultCode() + ": " + result.getUserMessage(), result.isSuccess()); return result.getPayload();
    }
    private Message call(SessionContext session, String command, Serializable payload) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(10000); ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream()); output.flush();
            SafeObjectInputStream input = new SafeObjectInputStream(socket.getInputStream());
            output.writeObject(Message.request(command, session == null ? null : session.getSessionToken(), payload)); output.flush();
            return (Message) input.readObject();
        }
    }
    private static byte[] pdf(int size) {
        byte[] bytes = new byte[size]; new java.util.Random(42).nextBytes(bytes);
        System.arraycopy("%PDF-1.7\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, bytes, 0, 9); return bytes;
    }
}
