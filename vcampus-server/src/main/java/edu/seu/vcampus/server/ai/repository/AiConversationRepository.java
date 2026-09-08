package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiChatMessage;
import edu.seu.vcampus.common.ai.AiSessionSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** AI 会话与消息的 MySQL DAO；事务由服务层统一控制。 */
public final class AiConversationRepository {
    public String create(Connection c, long userId, String model) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_chat_sessions(user_id,title,status,model_name) VALUES(?,NULL,'ACTIVE',?)",
                Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setLong(1, userId); ps.setString(2, model); ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            try { if (keys.next()) return Long.toString(keys.getLong(1)); }
            finally { keys.close(); }
        } finally { ps.close(); }
        throw new IllegalStateException("AI session id was not generated");
    }

    public boolean owns(Connection c, long userId, String sessionId) throws Exception {
        Long id = id(sessionId); if (id == null) return false;
        PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM ai_chat_sessions WHERE id=? AND user_id=? AND status='ACTIVE'");
        try {
            ps.setLong(1, id.longValue()); ps.setLong(2, userId);
            ResultSet rs = ps.executeQuery();
            try { return rs.next(); } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public List<AiSessionSummary> list(Connection c, long userId) throws Exception {
        return listByStatus(c, userId, "ACTIVE");
    }

    public List<AiSessionSummary> archived(Connection c, long userId) throws Exception {
        return listByStatus(c, userId, "ARCHIVED");
    }

    private List<AiSessionSummary> listByStatus(Connection c, long userId, String status) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,title,model_name,created_at,updated_at FROM ai_chat_sessions "
                        + "WHERE user_id=? AND status=? ORDER BY updated_at DESC LIMIT 100");
        try {
            ps.setLong(1, userId); ps.setString(2, status); ResultSet rs = ps.executeQuery();
            try {
                List<AiSessionSummary> out = new ArrayList<AiSessionSummary>();
                while (rs.next()) out.add(new AiSessionSummary(Long.toString(rs.getLong(1)),
                        rs.getString(2), rs.getString(3), rs.getTimestamp(4).getTime(),
                        rs.getTimestamp(5).getTime()));
                return out;
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public List<AiChatMessage> history(Connection c, String sessionId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,request_id,sender_type,content,status,created_at "
                        + "FROM ai_chat_messages WHERE session_id=? ORDER BY sequence_no");
        try {
            ps.setLong(1, requireId(sessionId)); ResultSet rs = ps.executeQuery();
            try {
                List<AiChatMessage> out = new ArrayList<AiChatMessage>();
                while (rs.next()) out.add(new AiChatMessage(rs.getLong(1), sessionId,
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).getTime()));
                return out;
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public long append(Connection c, String sessionId, String requestId,
                       String sender, String content, String status) throws Exception {
        long id = requireId(sessionId);
        lockSession(c, id);
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_chat_messages(session_id,request_id,sequence_no,sender_type,content,status) "
                        + "SELECT ?,?,COALESCE(MAX(sequence_no),0)+1,?,?,? FROM ai_chat_messages WHERE session_id=?",
                Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setLong(1, id); ps.setString(2, requestId); ps.setString(3, sender);
            ps.setString(4, content); ps.setString(5, status); ps.setLong(6, id);
            ps.executeUpdate(); ResultSet keys = ps.getGeneratedKeys();
            try { return keys.next() ? keys.getLong(1) : 0L; }
            finally { keys.close(); }
        } finally { ps.close(); }
    }

    public boolean hasUserRequest(Connection c, String sessionId, String requestId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM ai_chat_messages WHERE session_id=? AND request_id=? "
                        + "AND sender_type='USER' LIMIT 1");
        try {
            ps.setLong(1, requireId(sessionId)); ps.setString(2, requestId);
            ResultSet rs = ps.executeQuery();
            try { return rs.next(); } finally { rs.close(); }
        } finally { ps.close(); }
    }

    private void lockSession(Connection c, long id) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM ai_chat_sessions WHERE id=? FOR UPDATE");
        try {
            ps.setLong(1, id); ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) throw new IllegalArgumentException("invalid AI session id");
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    public void titleFromFirstQuery(Connection c, String sessionId, String text) throws Exception {
        String title = text == null ? "新会话" : text.trim();
        if (title.length() > 60) title = title.substring(0, 60);
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_chat_sessions SET title=COALESCE(title,?),updated_at=CURRENT_TIMESTAMP(3) WHERE id=?");
        try { ps.setString(1, title); ps.setLong(2, requireId(sessionId)); ps.executeUpdate(); }
        finally { ps.close(); }
    }

    public void archive(Connection c, String sessionId, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_chat_sessions SET status='ARCHIVED' WHERE id=? AND user_id=?");
        try { ps.setLong(1, requireId(sessionId)); ps.setLong(2, userId); ps.executeUpdate(); }
        finally { ps.close(); }
    }

    public boolean restore(Connection c, String sessionId, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_chat_sessions SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP(3) "
                        + "WHERE id=? AND user_id=? AND status='ARCHIVED'");
        try {
            ps.setLong(1, requireId(sessionId)); ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        } finally { ps.close(); }
    }

    public void rename(Connection c, String sessionId, long userId, String title) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_chat_sessions SET title=? WHERE id=? AND user_id=? AND status='ACTIVE'");
        try {
            ps.setString(1, title); ps.setLong(2, requireId(sessionId)); ps.setLong(3, userId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("invalid AI session id");
        } finally { ps.close(); }
    }

    private long requireId(String value) {
        Long parsed = id(value);
        if (parsed == null) throw new IllegalArgumentException("invalid AI session id");
        return parsed.longValue();
    }

    private Long id(String value) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            return parsed > 0 ? Long.valueOf(parsed) : null;
        } catch (NumberFormatException ex) { return null; }
    }
}
