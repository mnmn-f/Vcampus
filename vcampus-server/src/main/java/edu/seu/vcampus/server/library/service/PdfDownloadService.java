package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;
import edu.seu.vcampus.common.library.PdfFileStore;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.PdfRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 每个下载分块重新检查可见性与记录归属，记录真实已传输字节。 */
public final class PdfDownloadService extends LibraryServiceSupport {
    private final PdfRepository repository; private final PdfFileStore files;
    public PdfDownloadService(PdfRepository repository, PdfFileStore files, TransactionManager transactions) {
        super(transactions); this.repository = repository; this.files = files;
    }
    public PdfDownloadRecord begin(SessionContext session, long id) {
        require(session, Permission.LIBRARY_READ);
        return execute(c -> {
            PdfResourceView r = PdfLibraryService.visible(session, repository.find(c, id, true));
            if (!java.nio.file.Files.isRegularFile(files.file(r.getSha256())))
                throw new LibraryServiceException(ResultCodes.NOT_FOUND, "PDF 文件暂不可用，请联系图书管理员");
            return repository.saveDownload(c, new PdfDownloadRecord(0, session.getUserId(), r.getId(), r.getTitle(), r.getFileName(),
                    r.getFileSize(), r.getSha256(), 0, "DOWNLOADING", System.currentTimeMillis(), 0, null));
        });
    }
    public synchronized byte[] chunk(SessionContext session, PdfDownloadChunk request) {
        require(session, Permission.LIBRARY_READ);
        if (request == null || request.getLength() < 1 || request.getLength() > PdfCommands.CHUNK_BYTES || request.getOffset() < 0)
            throw PdfLibraryService.invalid("下载分块参数无效");
        return execute(c -> {
            PdfDownloadRecord r = owned(session, repository.download(c, request.getDownloadId(), true));
            PdfLibraryService.visible(session, repository.find(c, r.getResourceId(), true));
            if (!"DOWNLOADING".equals(r.getStatus()) || request.getOffset() > r.getTransferred() || request.getOffset() >= r.getFileSize())
                throw PdfLibraryService.invalid("下载状态或分块顺序不正确");
            byte[] bytes = files.read(r.getSha256(), request.getOffset(), request.getLength());
            repository.saveDownload(c, progress(r, Math.max(r.getTransferred(), request.getOffset() + bytes.length), "DOWNLOADING", 0, null));
            return bytes;
        });
    }
    public synchronized PdfDownloadRecord finish(SessionContext session, PdfDownloadFinish request) {
        require(session, Permission.LIBRARY_READ);
        if (request == null || request.getReason() != null && request.getReason().length() > 1000) throw PdfLibraryService.invalid("下载结果参数无效");
        return execute(c -> {
            PdfDownloadRecord r = owned(session, repository.download(c, request.getDownloadId(), true));
            if (!"DOWNLOADING".equals(r.getStatus())) return r;
            if (request.getSuccess() && r.getTransferred() != r.getFileSize()) throw PdfLibraryService.invalid("文件尚未完整下载");
            return repository.saveDownload(c, progress(r, r.getTransferred(), request.getSuccess() ? "COMPLETED" : "FAILED",
                    System.currentTimeMillis(), request.getSuccess() ? null : request.getReason()));
        });
    }
    public PageResult<PdfDownloadRecord> history(SessionContext session, PdfQuery query) {
        require(session, Permission.LIBRARY_READ); PdfLibraryService.checkQuery(query);
        return execute(c -> repository.history(c, session.getUserId(), query));
    }
    private static PdfDownloadRecord owned(SessionContext session, PdfDownloadRecord r) {
        if (r == null || r.getUserId() != session.getUserId()) throw new LibraryServiceException(ResultCodes.FORBIDDEN, "无权访问该下载记录");
        return r;
    }
    private static PdfDownloadRecord progress(PdfDownloadRecord r, long bytes, String state, long finished, String reason) {
        return new PdfDownloadRecord(r.getId(), r.getUserId(), r.getResourceId(), r.getTitle(), r.getFileName(), r.getFileSize(),
                r.getSha256(), bytes, state, r.getStartedAt(), finished, reason);
    }
}
