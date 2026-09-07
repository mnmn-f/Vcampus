package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiFeedbackEntry;
import edu.seu.vcampus.common.ai.AiFeedbackRequest;

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

    public List<AiFeedbackEntry> latest(Connection c, int limit) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,rating,category,comment,question_preview,created_at "
                        + "FROM ai_answer_feedback ORDER BY created_at DESC,id DESC LIMIT ?");
        try {
            ps.setInt(1, Math.min(200, Math.max(1, limit)));
            ResultSet rs = ps.executeQuery();
            try {
                List<AiFeedbackEntry> out = new ArrayList<AiFeedbackEntry>();
                while (rs.next()) out.add(new AiFeedbackEntry(rs.getLong(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getTimestamp(6).getTime()));
                return out;
            } finally { rs.close(); }
        } finally { ps.close(); }
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
