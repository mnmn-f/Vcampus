package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiFeedbackEntry;
import edu.seu.vcampus.common.ai.AiFeedbackRequest;
import edu.seu.vcampus.common.ai.AiFeedbackQuery;
import edu.seu.vcampus.common.ai.AiFeedbackTriageRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class AiFeedbackRepository {
    public boolean save(Connection c, long userId, AiFeedbackRequest request) throws Exception {
        long sessionId;
        try { sessionId = Long.parseLong(request.getSessionId()); }
        catch (Exception ex) { return false; }
        String question = question(c, sessionId, userId, request.getRequestId());
        if (question == null) return false;
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_answer_feedback(session_id,request_id,user_id,rating,category,"
                        + "comment,question_preview) VALUES(?,?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE rating=VALUES(rating),category=VALUES(category),"
                        + "comment=VALUES(comment),question_preview=VALUES(question_preview),"
                        + "created_at=CURRENT_TIMESTAMP(3)");
        try {
            ps.setLong(1, sessionId); ps.setString(2, request.getRequestId());
            ps.setLong(3, userId); ps.setString(4, request.getRating());
            ps.setString(5, request.getCategory()); ps.setString(6, request.getComment());
            ps.setString(7, question); return ps.executeUpdate() > 0;
        } finally { ps.close(); }
    }

    public List<AiFeedbackEntry> latest(Connection c, AiFeedbackQuery query, int limit) throws Exception {
        String rating = text(query == null ? null : query.getRating());
        String category = text(query == null ? null : query.getCategory());
        String status = text(query == null ? null : query.getProcessStatus());
        Long from = query == null ? null : query.getFromTime();
        Long to = query == null ? null : query.getToTime();
        PreparedStatement ps = c.prepareStatement(
                "SELECT f.id,f.rating,f.category,f.comment,f.question_preview,f.created_at,"
                        + "f.process_status,f.related_chunk_id,k.title "
                        + "FROM ai_answer_feedback f LEFT JOIN ai_knowledge_chunks k "
                        + "ON k.id=f.related_chunk_id WHERE (? IS NULL OR f.rating=?) "
                        + "AND (? IS NULL OR f.category LIKE ?) "
                        + "AND (? IS NULL OR f.process_status=?) "
                        + "AND (? IS NULL OR f.created_at>=FROM_UNIXTIME(?/1000)) "
                        + "AND (? IS NULL OR f.created_at<FROM_UNIXTIME(?/1000)) "
                        + "ORDER BY f.created_at DESC,f.id DESC LIMIT ?");
        try {
            String like = category == null ? null : "%" + category + "%";
            ps.setString(1, rating); ps.setString(2, rating);
            ps.setString(3, category); ps.setString(4, like);
            ps.setString(5, status); ps.setString(6, status);
            setLong(ps, 7, from); setLong(ps, 8, from);
            setLong(ps, 9, to); setLong(ps, 10, to);
            ps.setInt(11, Math.min(200, Math.max(1, limit)));
            ResultSet rs = ps.executeQuery();
            try {
                List<AiFeedbackEntry> out = new ArrayList<AiFeedbackEntry>();
                while (rs.next()) out.add(new AiFeedbackEntry(rs.getLong(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getTimestamp(6).getTime(), rs.getString(7),
                        nullableLong(rs, 8), rs.getString(9)));
                return out;
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public boolean update(Connection c, long userId, AiFeedbackTriageRequest request) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_answer_feedback SET process_status=?,related_chunk_id=?,handled_by=?,"
                        + "handled_at=CURRENT_TIMESTAMP(3) WHERE id=?");
        try {
            ps.setString(1, request.getProcessStatus());
            if (request.getRelatedChunkId() == null) ps.setNull(2, java.sql.Types.BIGINT);
            else ps.setLong(2, request.getRelatedChunkId().longValue());
            ps.setLong(3, userId); ps.setLong(4, request.getFeedbackId());
            return ps.executeUpdate() == 1;
        } finally { ps.close(); }
    }

    public boolean knowledgeExists(Connection c, long chunkId) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT 1 FROM ai_knowledge_chunks WHERE id=? LIMIT 1");
        try {
            ps.setLong(1, chunkId); ResultSet rs = ps.executeQuery();
            try { return rs.next(); } finally { rs.close(); }
        } finally { ps.close(); }
    }

    private void setLong(PreparedStatement ps, int index, Long value) throws Exception {
        if (value == null) ps.setNull(index, java.sql.Types.BIGINT);
        else ps.setLong(index, value.longValue());
    }

    private Long nullableLong(ResultSet rs, int index) throws Exception {
        long value = rs.getLong(index); return rs.wasNull() ? null : Long.valueOf(value);
    }

    private String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String question(Connection c, long sessionId, long userId, String requestId)
            throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT m.content FROM ai_chat_messages m JOIN ai_chat_sessions s ON s.id=m.session_id "
                        + "WHERE s.id=? AND s.user_id=? AND m.request_id=? AND m.sender_type='USER' "
                        + "ORDER BY m.id DESC LIMIT 1");
        try {
            ps.setLong(1, sessionId); ps.setLong(2, userId); ps.setString(3, requestId);
            ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) return null;
                String value = rs.getString(1);
                if (value == null) return null;
                value = value.replaceAll("(?i)(1[3-9]\\d{9})", "手机号已隐藏")
                        .replaceAll("\\b\\d{8,16}\\b", "编号已隐藏");
                return value.length() > 240 ? value.substring(0, 240) : value;
            } finally { rs.close(); }
        } finally { ps.close(); }
    }
}
