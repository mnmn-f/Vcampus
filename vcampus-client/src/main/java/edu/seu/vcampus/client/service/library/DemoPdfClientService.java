package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.security.Role;
import java.util.ArrayList;
import java.util.Locale;

/** 与网络模式相同的演示流程，按当前登录用户隔离私有记录。 */
public final class DemoPdfClientService implements PdfClientService {
    private static final DemoPdfStore DEFAULT = new DemoPdfStore(java.nio.file.Paths.get(System.getProperty("user.home"), ".vcampus", "library-pdf-demo"));
    private final ClientSession session; private final DemoPdfStore store; private final String token;
    public DemoPdfClientService(ClientSession session) { this(session, DEFAULT); }
    public DemoPdfClientService(ClientSession session, DemoPdfStore store) {
        this.session = session; this.store = store; this.token = session.getSessionToken();
    }
    private long user() {
        if (!session.isAuthenticated() || !token.equals(session.getSessionToken())) throw new IllegalStateException("登录会话已结束");
        return session.getUserId();
    }
    private boolean admin() { user(); return session.getActiveRole() == Role.LIBRARIAN; }
    @Override public PageResult<PdfResourceView> list(PdfQuery q) throws Exception {
        synchronized (store) {
            store.load(); long owner = user();
            if (q == null || !java.util.Arrays.asList("PUBLIC", "MINE", "ADMIN", "REVIEW").contains(q.getScope())
                    || q.getPage() < 1 || q.getPageSize() < 1 || q.getPageSize() > 100) throw new IllegalArgumentException("查询参数无效");
            if (("ADMIN".equals(q.getScope()) || "REVIEW".equals(q.getScope())) && !admin()) throw denied();
            ArrayList<PdfResourceView> rows = new ArrayList<PdfResourceView>();
            for (PdfResourceView r : store.resources) {
                if ("PUBLIC".equals(q.getScope()) && !"APPROVED".equals(r.getStatus())) continue;
                if ("MINE".equals(q.getScope()) && r.getUploaderId() != owner) continue;
                if ("REVIEW".equals(q.getScope()) && !"PENDING".equals(r.getStatus())) continue;
                if (q.getStatus() != null && !q.getStatus().equals(r.getStatus())) continue;
                if (q.getKeyword() != null && !(r.getTitle() + " " + r.getDescription() + " " + r.getFileName()).toLowerCase(Locale.ROOT).contains(q.getKeyword().toLowerCase(Locale.ROOT))) continue;
                rows.add(0, r);
            }
            return DemoLibrarySupport.page(rows, q.getPage(), q.getPageSize());
        }
    }
    @Override public PdfResourceView detail(long id) throws Exception {
        synchronized (store) {
            store.load(); long owner = user();
            for (PdfResourceView r : store.resources) if (r.getId() == id) {
                if (!"APPROVED".equals(r.getStatus()) && r.getUploaderId() != owner && !admin()) throw denied(); return r;
            }
            throw new IllegalArgumentException("资源不存在");
        }
    }
    @Override public String beginUpload(PdfUploadRequest request) throws Exception {
        if (session.getActiveRole() != Role.STUDENT && !admin()) throw denied();
        return store.files.begin(user(), request);
    }
    @Override public long uploadChunk(PdfUploadChunk chunk) throws Exception { return store.files.append(user(), chunk); }
    @Override public PdfResourceView commitUpload(String id) throws Exception {
        synchronized (store) {
            store.load(); PdfUploadRequest r = store.files.seal(user(), id);
            long next = store.resources.stream().mapToLong(PdfResourceView::getId).max().orElse(0) + 1;
            PdfResourceView value = new PdfResourceView(next, r.getTitle(), r.getDescription(), r.getFileName(), r.getFileSize(), r.getSha256(),
                    user(), session.getDisplayName(), "PENDING", System.currentTimeMillis(), 0, null, 0, null);
            store.resources.add(value); store.save(); store.files.complete(user(), id); return value;
        }
    }
    @Override public void abortUpload(String id) throws Exception { store.files.abort(user(), id); }
    @Override public PdfResourceView review(PdfReviewRequest request) throws Exception {
        synchronized (store) {
            if (!admin()) throw denied(); PdfResourceView r = detail(request.getResourceId()); String status = request.getDecision();
            if (!java.util.Arrays.asList("APPROVED", "REJECTED", "INACTIVE").contains(status)) throw new IllegalArgumentException("审核操作无效");
            if ("INACTIVE".equals(status) ? !"APPROVED".equals(r.getStatus()) : !"PENDING".equals(r.getStatus())) throw new IllegalArgumentException("资源状态已变化，请刷新");
            if ("REJECTED".equals(status) && (request.getReason() == null || request.getReason().trim().isEmpty() || request.getReason().length() > 1000)) throw new IllegalArgumentException("拒绝时必须填写 1 至 1000 字的理由");
            PdfResourceView value = new PdfResourceView(r.getId(), r.getTitle(), r.getDescription(), r.getFileName(), r.getFileSize(), r.getSha256(),
                    r.getUploaderId(), r.getUploaderName(), status, r.getUploadedAt(), user(), session.getDisplayName(), System.currentTimeMillis(),
                    "REJECTED".equals(status) ? request.getReason().trim() : null);
            store.resources.set(store.resources.indexOf(r), value); store.save(); return value;
        }
    }
    @Override public PdfDownloadRecord beginDownload(long id) throws Exception {
        synchronized (store) {
            PdfResourceView r = detail(id); long next = store.downloads.stream().mapToLong(PdfDownloadRecord::getId).max().orElse(0) + 1;
            PdfDownloadRecord value = new PdfDownloadRecord(next, user(), id, r.getTitle(), r.getFileName(), r.getFileSize(), r.getSha256(), 0,
                    "DOWNLOADING", System.currentTimeMillis(), 0, null); store.downloads.add(value); store.save(); return value;
        }
    }
    @Override public byte[] downloadChunk(PdfDownloadChunk request) throws Exception {
        synchronized (store) {
            PdfDownloadRecord r = download(request.getDownloadId()); detail(r.getResourceId());
            if (!"DOWNLOADING".equals(r.getStatus()) || request.getOffset() < 0 || request.getOffset() > r.getTransferred() || request.getOffset() >= r.getFileSize()) throw new IllegalArgumentException("下载状态无效");
            byte[] bytes = store.files.read(r.getSha256(), request.getOffset(), request.getLength());
            store.downloads.set(store.downloads.indexOf(r), DemoPdfStore.progress(r, Math.max(r.getTransferred(), request.getOffset() + bytes.length), "DOWNLOADING", null)); return bytes;
        }
    }
    @Override public PdfDownloadRecord finishDownload(PdfDownloadFinish request) throws Exception {
        synchronized (store) {
            PdfDownloadRecord r = download(request.getDownloadId());
            if (!"DOWNLOADING".equals(r.getStatus())) return r;
            if (request.getSuccess() && r.getTransferred() != r.getFileSize()) throw new IllegalArgumentException("文件尚未完整下载");
            PdfDownloadRecord result = DemoPdfStore.progress(r, r.getTransferred(), request.getSuccess() ? "COMPLETED" : "FAILED", request.getReason());
            store.downloads.set(store.downloads.indexOf(r), result); store.save(); return result;
        }
    }
    @Override public PageResult<PdfDownloadRecord> history(PdfQuery q) throws Exception {
        synchronized (store) {
            store.load(); long owner = user(); ArrayList<PdfDownloadRecord> rows = new ArrayList<PdfDownloadRecord>();
            for (PdfDownloadRecord r : store.downloads) if (r.getUserId() == owner) rows.add(0, r);
            return DemoLibrarySupport.page(rows, q.getPage(), q.getPageSize());
        }
    }
    private PdfDownloadRecord download(long id) throws Exception {
        store.load(); long owner = user();
        for (PdfDownloadRecord r : store.downloads) if (r.getId() == id && r.getUserId() == owner) return r;
        throw denied();
    }
    private static IllegalArgumentException denied() { return new IllegalArgumentException("无权访问该资源或记录"); }
}
