package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.PdfRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** PDF 上传、可见性和审核；上传者身份始终来自当前服务端会话。 */
public final class PdfLibraryService extends LibraryServiceSupport {
    private final PdfRepository repository;
    private final PdfFileStore files;
    private final PdfDownloadService downloads;
    public PdfLibraryService(PdfRepository repository, PdfFileStore files, TransactionManager transactions) {
        super(transactions); this.repository = repository; this.files = files;
        this.downloads = new PdfDownloadService(repository, files, transactions);
    }
    public PdfDownloadService downloads() { return downloads; }
    public PageResult<PdfResourceView> list(SessionContext session, PdfQuery q) {
        require(session, Permission.LIBRARY_READ); checkQuery(q);
        String status = clean(q.getStatus()); long owner = 0;
        if ("PUBLIC".equals(q.getScope())) status = "APPROVED";
        else if ("MINE".equals(q.getScope())) owner = session.getUserId();
        else if ("REVIEW".equals(q.getScope())) { require(session, Permission.LIBRARY_MANAGE); status = "PENDING"; }
        else if ("ADMIN".equals(q.getScope())) require(session, Permission.LIBRARY_MANAGE);
        else throw invalid("资源查询范围无效");
        if (status != null && !java.util.Arrays.asList("PENDING", "APPROVED", "REJECTED", "INACTIVE").contains(status)) throw invalid("审核状态无效");
        final long selectedOwner = owner;
        PdfQuery normalized = new PdfQuery(q.getScope(), q.getKeyword(), status, q.getPage(), q.getPageSize());
        return execute(c -> repository.list(c, normalized, selectedOwner));
    }
    public PdfResourceView detail(SessionContext session, long id) {
        require(session, Permission.LIBRARY_READ); id(id, "资源编号无效");
        return execute(c -> visible(session, repository.find(c, id, false)));
    }
    public String beginUpload(SessionContext session, PdfUploadRequest request) {
        uploader(session); return io(() -> files.begin(session.getUserId(), request));
    }
    public long uploadChunk(SessionContext session, PdfUploadChunk chunk) {
        uploader(session); return io(() -> files.append(session.getUserId(), chunk));
    }
    public synchronized PdfResourceView commitUpload(SessionContext session, String id) {
        uploader(session); PdfUploadRequest request = io(() -> files.seal(session.getUserId(), id));
        PdfResourceView saved = execute(c -> repository.save(c, new PdfResourceView(0, request.getTitle().trim(), request.getDescription(),
                request.getFileName(), request.getFileSize(), request.getSha256(), session.getUserId(), session.getDisplayName(),
                "PENDING", System.currentTimeMillis(), 0, null, 0, null)));
        files.complete(session.getUserId(), id); return saved;
    }
    public Boolean abortUpload(SessionContext session, String id) {
        uploader(session); return io(() -> { files.abort(session.getUserId(), id); return Boolean.TRUE; });
    }
    public synchronized PdfResourceView review(SessionContext session, PdfReviewRequest request) {
        require(session, Permission.LIBRARY_MANAGE);
        if (request == null) throw invalid("审核请求不能为空");
        String decision = request.getDecision(); String reason = clean(request.getReason());
        if (!java.util.Arrays.asList("APPROVED", "REJECTED", "INACTIVE").contains(decision)) throw invalid("审核操作无效");
        if ("REJECTED".equals(decision) && (reason == null || reason.length() > 1000)) throw invalid("拒绝时必须填写 1 至 1000 字的理由");
        return execute(c -> {
            PdfResourceView r = repository.find(c, request.getResourceId(), true);
            if (r == null) throw missing();
            if ("INACTIVE".equals(decision) ? !"APPROVED".equals(r.getStatus()) : !"PENDING".equals(r.getStatus()))
                throw new LibraryServiceException(ResultCodes.CONFLICT, "资源状态已变化，请刷新后操作");
            return repository.save(c, new PdfResourceView(r.getId(), r.getTitle(), r.getDescription(), r.getFileName(), r.getFileSize(),
                    r.getSha256(), r.getUploaderId(), r.getUploaderName(), decision, r.getUploadedAt(), session.getUserId(),
                    session.getDisplayName(), System.currentTimeMillis(), "REJECTED".equals(decision) ? reason : null));
        });
    }
    static PdfResourceView visible(SessionContext session, PdfResourceView resource) {
        if (resource == null) throw missing();
        if (!"APPROVED".equals(resource.getStatus()) && session.getUserId() != resource.getUploaderId()
                && !session.allows(Permission.LIBRARY_MANAGE)) throw new LibraryServiceException(ResultCodes.FORBIDDEN, "资源尚未公开或已停用");
        return resource;
    }
    static void checkQuery(PdfQuery q) {
        if (q == null) throw invalid("查询不能为空"); page(q.getPage(), q.getPageSize());
        if (q.getKeyword() != null && q.getKeyword().length() > 240) throw invalid("搜索内容过长");
    }
    private static void uploader(SessionContext session) {
        require(session, Permission.LIBRARY_READ);
        if (session.getActiveRole() != Role.STUDENT && !session.allows(Permission.LIBRARY_MANAGE))
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "当前职责不能上传资源");
    }
    private <T> T io(IoWork<T> work) {
        try { return work.run(); }
        catch (IllegalArgumentException ex) { throw invalid(ex.getMessage()); }
        catch (Exception ex) { throw new LibraryServiceException(ResultCodes.INTERNAL_ERROR, "PDF 文件操作失败，请重试", ex); }
    }
    static LibraryServiceException invalid(String message) { return new LibraryServiceException(ResultCodes.INVALID_INPUT, message); }
    private static LibraryServiceException missing() { return new LibraryServiceException(ResultCodes.NOT_FOUND, "PDF 资源不存在"); }
    private static String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private interface IoWork<T> { T run() throws Exception; }
}
