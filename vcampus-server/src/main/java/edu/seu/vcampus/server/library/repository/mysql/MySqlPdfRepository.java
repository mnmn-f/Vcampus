package edu.seu.vcampus.server.library.repository.mysql;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.server.library.repository.PdfRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** PDF 元数据与下载记录的参数化 MySQL 实现。 */
public final class MySqlPdfRepository implements PdfRepository {
    @Override public PageResult<PdfResourceView> list(Connection c, PdfQuery q, long owner) throws Exception {
        StringBuilder where = new StringBuilder(" WHERE 1=1"); List<Object> parameters = new ArrayList<Object>();
        if (owner > 0) { where.append(" AND uploader_id=?"); parameters.add(owner); }
        if (q.getStatus() != null) { where.append(" AND status=?"); parameters.add(q.getStatus()); }
        if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) {
            where.append(" AND (title LIKE ? OR description LIKE ? OR file_name LIKE ?)");
            for (int i = 0; i < 3; i++) parameters.add("%" + q.getKeyword().trim() + "%");
        }
        return JdbcLibrarySupport.page(c, "SELECT COUNT(*) FROM library_pdf_resources" + where,
                "SELECT * FROM library_pdf_resources" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                parameters, q.getPage(), q.getPageSize(), MySqlPdfRepository::resource);
    }
    @Override public PdfResourceView find(Connection c, long id, boolean lock) throws Exception {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM library_pdf_resources WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? resource(r) : null; }
        }
    }
    @Override public PdfResourceView save(Connection c, PdfResourceView r) throws Exception {
        long id = r.getId();
        if (id == 0) {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO library_pdf_resources"
                    + " (title,description,file_name,file_size,sha256,uploader_id,uploader_name,status,uploaded_at,reviewer_id,reviewer_name,reviewed_at,rejection_reason)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                bind(s, r.getTitle(), r.getDescription(), r.getFileName(), r.getFileSize(), r.getSha256(), r.getUploaderId(),
                        r.getUploaderName(), r.getStatus(), r.getUploadedAt(), r.getReviewerId(), r.getReviewerName(), r.getReviewedAt(), r.getRejectionReason());
                s.executeUpdate(); id = generatedId(s);
            }
        } else {
            try (PreparedStatement s = c.prepareStatement("UPDATE library_pdf_resources SET status=?,reviewer_id=?,reviewer_name=?,reviewed_at=?,rejection_reason=? WHERE id=?")) {
                bind(s, r.getStatus(), r.getReviewerId(), r.getReviewerName(), r.getReviewedAt(), r.getRejectionReason(), id); s.executeUpdate();
            }
        }
        return find(c, id, false);
    }
    @Override public PdfDownloadRecord download(Connection c, long id, boolean lock) throws Exception {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM library_pdf_downloads WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? record(r) : null; }
        }
    }
    @Override public PdfDownloadRecord saveDownload(Connection c, PdfDownloadRecord r) throws Exception {
        long id = r.getId();
        if (id == 0) {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO library_pdf_downloads"
                    + " (user_id,resource_id,title,file_name,file_size,sha256,transferred,status,started_at,finished_at,failure_reason)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                bind(s, r.getUserId(), r.getResourceId(), r.getTitle(), r.getFileName(), r.getFileSize(), r.getSha256(), r.getTransferred(),
                        r.getStatus(), r.getStartedAt(), r.getFinishedAt(), r.getFailureReason()); s.executeUpdate(); id = generatedId(s);
            }
        } else {
            try (PreparedStatement s = c.prepareStatement("UPDATE library_pdf_downloads SET transferred=?,status=?,finished_at=?,failure_reason=? WHERE id=?")) {
                bind(s, r.getTransferred(), r.getStatus(), r.getFinishedAt(), r.getFailureReason(), id); s.executeUpdate();
            }
        }
        return download(c, id, false);
    }
    @Override public PageResult<PdfDownloadRecord> history(Connection c, long owner, PdfQuery q) throws Exception {
        List<Object> parameters = new ArrayList<Object>(); parameters.add(owner);
        return JdbcLibrarySupport.page(c, "SELECT COUNT(*) FROM library_pdf_downloads WHERE user_id=?",
                "SELECT * FROM library_pdf_downloads WHERE user_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                parameters, q.getPage(), q.getPageSize(), MySqlPdfRepository::record);
    }
    private static PdfResourceView resource(ResultSet r) throws java.sql.SQLException {
        return new PdfResourceView(r.getLong("id"), r.getString("title"), r.getString("description"), r.getString("file_name"),
                r.getLong("file_size"), r.getString("sha256"), r.getLong("uploader_id"), r.getString("uploader_name"), r.getString("status"),
                r.getLong("uploaded_at"), r.getLong("reviewer_id"), r.getString("reviewer_name"), r.getLong("reviewed_at"), r.getString("rejection_reason"));
    }
    private static PdfDownloadRecord record(ResultSet r) throws java.sql.SQLException {
        return new PdfDownloadRecord(r.getLong("id"), r.getLong("user_id"), r.getLong("resource_id"), r.getString("title"),
                r.getString("file_name"), r.getLong("file_size"), r.getString("sha256"), r.getLong("transferred"), r.getString("status"),
                r.getLong("started_at"), r.getLong("finished_at"), r.getString("failure_reason"));
    }
    private static void bind(PreparedStatement s, Object... values) throws java.sql.SQLException {
        for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
    }
    private static long generatedId(PreparedStatement s) throws java.sql.SQLException {
        try (ResultSet keys = s.getGeneratedKeys()) {
            if (!keys.next()) throw new java.sql.SQLException("No generated PDF row id"); return keys.getLong(1);
        }
    }
}
