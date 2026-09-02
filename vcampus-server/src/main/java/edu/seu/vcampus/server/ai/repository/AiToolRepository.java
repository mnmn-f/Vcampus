package edu.seu.vcampus.server.ai.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** 工具调用、待确认状态与审计结果 DAO。 */
public final class AiToolRepository {
    public long create(Connection c, String sessionId, String requestId, String toolName,
                       boolean write, String arguments, String status, long userId) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ai_tool_call_logs(session_id,request_id,tool_name,action_type,"
                        + "arguments_json,status,requested_by) VALUES(?,?,?,?,CAST(? AS JSON),?,?)",
                Statement.RETURN_GENERATED_KEYS);
        try {
            if (sessionId == null) ps.setNull(1, java.sql.Types.BIGINT);
            else ps.setLong(1, Long.parseLong(sessionId));
            ps.setString(2, requestId); ps.setString(3, toolName);
            ps.setString(4, write ? "WRITE" : "READ");
            ps.setString(5, arguments == null ? "{}" : arguments);
            ps.setString(6, status); ps.setLong(7, userId); ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            try { if (keys.next()) return keys.getLong(1); }
            finally { keys.close(); }
        } finally { ps.close(); }
        throw new IllegalStateException("tool log id was not generated");
    }

    public AiToolRecord find(Connection c, long id) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT id,session_id,request_id,tool_name,arguments_json,status,requested_by,created_at "
                        + "FROM ai_tool_call_logs WHERE id=?");
        try { ps.setLong(1, id); ResultSet rs = ps.executeQuery();
            try {
                if (!rs.next()) return null;
                long session = rs.getLong(2);
                return new AiToolRecord(rs.getLong(1), rs.wasNull() ? null : Long.toString(session),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getLong(7), rs.getTimestamp(8).getTime());
            } finally { rs.close(); }
        } finally { ps.close(); }
    }

    /** 原子领取待确认操作；并发请求中只有一个调用方会得到 true。 */
    public boolean claim(Connection c, long id, long userId, long oldestCreatedAt) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_tool_call_logs SET status='CONFIRMED',confirmed_by=? "
                        + "WHERE id=? AND requested_by=? AND status='CONFIRM_REQUIRED' "
                        + "AND created_at>=FROM_UNIXTIME(? / 1000.0)");
        try {
            ps.setLong(1, userId); ps.setLong(2, id); ps.setLong(3, userId);
            ps.setLong(4, oldestCreatedAt);
            return ps.executeUpdate() == 1;
        } finally { ps.close(); }
    }

    public void finish(Connection c, long id, String status, String summary,
                       Long confirmedBy) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "UPDATE ai_tool_call_logs SET status=?,result_summary=?,confirmed_by=?,"
                        + "completed_at=CURRENT_TIMESTAMP(3) WHERE id=?");
        try {
            ps.setString(1, status); ps.setString(2, summary);
            if (confirmedBy == null) ps.setNull(3, java.sql.Types.BIGINT);
            else ps.setLong(3, confirmedBy.longValue());
            ps.setLong(4, id); ps.executeUpdate();
        } finally { ps.close(); }
    }

    public long count(Connection c, String status) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM ai_tool_call_logs WHERE status=?");
        try { ps.setString(1, status); ResultSet rs = ps.executeQuery();
            try { rs.next(); return rs.getLong(1); } finally { rs.close(); } }
        finally { ps.close(); }
    }

    public long tableCount(Connection c, String table, String where) throws Exception {
        Statement st = c.createStatement();
        try { ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table + " " + where);
            try { rs.next(); return rs.getLong(1); } finally { rs.close(); } }
        finally { st.close(); }
    }
}
