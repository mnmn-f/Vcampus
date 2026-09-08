package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;

/** 测试仓储，生产通过 MySQL 持久化。 */
public final class InMemoryPdfRepository implements PdfRepository {
    private final Map<Long, PdfResourceView> resources = new LinkedHashMap<Long, PdfResourceView>();
    private final Map<Long, PdfDownloadRecord> downloads = new LinkedHashMap<Long, PdfDownloadRecord>();
    private long nextResource = 1, nextDownload = 1;
    @Override public synchronized PageResult<PdfResourceView> list(Connection c, PdfQuery q, long owner) {
        ArrayList<PdfResourceView> rows = new ArrayList<PdfResourceView>();
        for (PdfResourceView r : resources.values()) {
            if (owner > 0 && owner != r.getUploaderId()) continue;
            if (q.getStatus() != null && !q.getStatus().equals(r.getStatus())) continue;
            String text = (r.getTitle() + " " + r.getDescription() + " " + r.getFileName()).toLowerCase(Locale.ROOT);
            if (q.getKeyword() != null && !text.contains(q.getKeyword().toLowerCase(Locale.ROOT))) continue;
            rows.add(0, r);
        }
        return InMemoryLibrarySupport.page(rows, q.getPage(), q.getPageSize());
    }
    @Override public synchronized PdfResourceView find(Connection c, long id, boolean lock) { return resources.get(id); }
    @Override public synchronized PdfResourceView save(Connection c, PdfResourceView r) {
        long id = r.getId() > 0 ? r.getId() : nextResource++;
        PdfResourceView value = new PdfResourceView(id, r.getTitle(), r.getDescription(), r.getFileName(), r.getFileSize(), r.getSha256(),
                r.getUploaderId(), r.getUploaderName(), r.getStatus(), r.getUploadedAt(), r.getReviewerId(), r.getReviewerName(), r.getReviewedAt(), r.getRejectionReason());
        resources.put(id, value); return value;
    }
    @Override public synchronized PdfDownloadRecord download(Connection c, long id, boolean lock) { return downloads.get(id); }
    @Override public synchronized PdfDownloadRecord saveDownload(Connection c, PdfDownloadRecord r) {
        long id = r.getId() > 0 ? r.getId() : nextDownload++;
        PdfDownloadRecord value = new PdfDownloadRecord(id, r.getUserId(), r.getResourceId(), r.getTitle(), r.getFileName(), r.getFileSize(), r.getSha256(),
                r.getTransferred(), r.getStatus(), r.getStartedAt(), r.getFinishedAt(), r.getFailureReason());
        downloads.put(id, value); return value;
    }
    @Override public synchronized PageResult<PdfDownloadRecord> history(Connection c, long owner, PdfQuery q) {
        ArrayList<PdfDownloadRecord> rows = new ArrayList<PdfDownloadRecord>();
        for (PdfDownloadRecord row : downloads.values()) if (row.getUserId() == owner) rows.add(0, row);
        return InMemoryLibrarySupport.page(rows, q.getPage(), q.getPageSize());
    }
}
