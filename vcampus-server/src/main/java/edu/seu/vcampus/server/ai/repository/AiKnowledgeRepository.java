package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiKnowledgeQuery;
import edu.seu.vcampus.common.ai.AiKnowledgeSaveRequest;
import edu.seu.vcampus.common.ai.AiPage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 知识片段 MySQL DAO；候选集使用关键词与轻量文本向量混合排序。 */
public final class AiKnowledgeRepository {
    private final KnowledgeRanker ranker = new KnowledgeRanker();
    public AiPage<AiKnowledgeChunk> search(Connection c, AiKnowledgeQuery q) throws Exception {
        int page = Math.max(1, q == null ? 1 : q.getPage());
        int size = Math.min(100, Math.max(1, q == null ? 20 : q.getPageSize()));
        String keyword = text(q == null ? null : q.getKeyword());
        String status = text(q == null ? null : q.getStatus());
        String where = " WHERE (? IS NULL OR status=?) AND (? IS NULL OR title LIKE ? OR content LIKE ?)";
        long total = count(c, where, status, keyword);
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,source_type,title,content,status,updated_at FROM ai_knowledge_chunks"
                        + where + " ORDER BY updated_at DESC LIMIT ? OFFSET ?");
        try {
            bindFilter(ps, status, keyword); ps.setInt(6, size); ps.setInt(7, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            try { return new AiPage<AiKnowledgeChunk>(rows(rs), total, page, size); }
            finally { rs.close(); }
        } finally { ps.close(); }
    }

    public List<AiKnowledgeChunk> retrieve(Connection c, String query, int topK) throws Exception {
        if (query == null || query.trim().isEmpty()) return new ArrayList<AiKnowledgeChunk>();
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,source_type,title,content,status,updated_at "
                        + "FROM ai_knowledge_chunks WHERE status='ACTIVE' "
                        + "ORDER BY updated_at DESC LIMIT 500");
        try {
            ResultSet rs = ps.executeQuery();
            try { return ranker.rank(query, rows(rs), Math.min(20, Math.max(1, topK))); }
            finally { rs.close(); }
        } finally { ps.close(); }
    }

    public AiKnowledgeChunk save(Connection c, AiKnowledgeSaveRequest r, long userId) throws Exception {
        if (r.getChunkId() == null) {
            AiKnowledgeChunk inserted = insert(c, r, userId);
            addVersion(c, inserted, userId); return inserted;
        }
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_knowledge_chunks SET source_type=?,title=?,content=?,status=?,updated_by=? WHERE id=?");
        try {
            bindSave(ps, r, userId); ps.setLong(6, r.getChunkId().longValue());
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("knowledge chunk not found");
        } finally { ps.close(); }
        AiKnowledgeChunk updated = get(c, r.getChunkId().longValue());
        addVersion(c, updated, userId); return updated;
    }

    public List<edu.seu.vcampus.common.ai.AiKnowledgeVersion> versions(
            Connection c, long chunkId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,chunk_id,version_no,source_type,title,content,status,created_at "
                        + "FROM ai_knowledge_versions WHERE chunk_id=? ORDER BY version_no DESC");
        try {
            ps.setLong(1, chunkId); ResultSet rs = ps.executeQuery();
            try {
                List<edu.seu.vcampus.common.ai.AiKnowledgeVersion> out =
                        new ArrayList<edu.seu.vcampus.common.ai.AiKnowledgeVersion>();
                while (rs.next()) out.add(new edu.seu.vcampus.common.ai.AiKnowledgeVersion(
                        rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getTimestamp(8).getTime()));
                return out;
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public AiKnowledgeChunk rollback(Connection c, long chunkId, long versionId,
            long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT source_type,title,content,status FROM ai_knowledge_versions "
                        + "WHERE id=? AND chunk_id=?");
        try {
            ps.setLong(1, versionId); ps.setLong(2, chunkId); ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) throw new IllegalArgumentException("knowledge version not found");
                AiKnowledgeSaveRequest request = new AiKnowledgeSaveRequest(Long.valueOf(chunkId),
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
                return save(c, request, userId);
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    private void addVersion(Connection c, AiKnowledgeChunk chunk, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_knowledge_versions(chunk_id,version_no,source_type,title,content,"
                        + "status,created_by) SELECT ?,COALESCE(MAX(version_no),0)+1,?,?,?,?,? "
                        + "FROM ai_knowledge_versions WHERE chunk_id=?");
        try {
            ps.setLong(1, chunk.getChunkId()); ps.setString(2, chunk.getSourceType());
            ps.setString(3, chunk.getTitle()); ps.setString(4, chunk.getContent());
            ps.setString(5, chunk.getStatus()); ps.setLong(6, userId);
            ps.setLong(7, chunk.getChunkId()); ps.executeUpdate();
        } finally { ps.close(); }
    }

    public void deactivate(Connection c, long id, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_knowledge_chunks SET status='INACTIVE',updated_by=? WHERE id=?");
        try {
            ps.setLong(1, userId); ps.setLong(2, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("knowledge chunk not found");
        }
        finally { ps.close(); }
        addVersion(c, get(c, id), userId);
    }

    public long activeCount(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM ai_knowledge_chunks WHERE status='ACTIVE'");
        try { ResultSet rs = ps.executeQuery(); try { rs.next(); return rs.getLong(1); }
        finally { rs.close(); } } finally { ps.close(); }
    }

    private AiKnowledgeChunk insert(Connection c, AiKnowledgeSaveRequest r, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_knowledge_chunks(source_type,title,content,status,updated_by) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        try {
            bindSave(ps, r, userId); ps.executeUpdate(); ResultSet keys = ps.getGeneratedKeys();
            try { if (keys.next()) return get(c, keys.getLong(1)); }
            finally { keys.close(); }
        } finally { ps.close(); }
        throw new IllegalStateException("knowledge id was not generated");
    }

    private AiKnowledgeChunk get(Connection c, long id) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,source_type,title,content,status,updated_at FROM ai_knowledge_chunks WHERE id=?");
        try { ps.setLong(1, id); ResultSet rs = ps.executeQuery();
            try { if (!rs.next()) throw new IllegalArgumentException("knowledge chunk not found"); return row(rs); }
            finally { rs.close(); } } finally { ps.close(); }
    }

    private long count(Connection c, String where, String status, String keyword) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ai_knowledge_chunks" + where);
        try { bindFilter(ps, status, keyword); ResultSet rs = ps.executeQuery();
            try { rs.next(); return rs.getLong(1); } finally { rs.close(); } }
        finally { ps.close(); }
    }

    private void bindFilter(PreparedStatement ps, String status, String keyword) throws Exception {
        String like = keyword == null ? null : "%" + keyword + "%";
        ps.setString(1, status); ps.setString(2, status); ps.setString(3, keyword);
        ps.setString(4, like); ps.setString(5, like);
    }

    private void bindSave(PreparedStatement ps, AiKnowledgeSaveRequest r, long userId) throws Exception {
        ps.setString(1, r.getSourceType()); ps.setString(2, r.getTitle());
        ps.setString(3, r.getContent()); ps.setString(4, r.getStatus()); ps.setLong(5, userId);
    }

    private List<AiKnowledgeChunk> rows(ResultSet rs) throws Exception {
        List<AiKnowledgeChunk> out = new ArrayList<AiKnowledgeChunk>();
        while (rs.next()) out.add(row(rs)); return out;
    }

    private AiKnowledgeChunk row(ResultSet rs) throws Exception {
        return new AiKnowledgeChunk(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getTimestamp(6).getTime());
    }

    private String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

}
