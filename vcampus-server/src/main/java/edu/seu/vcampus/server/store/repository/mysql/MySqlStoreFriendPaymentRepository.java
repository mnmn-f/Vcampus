package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.threeten.bp.LocalDate;

/** V4 好友代付和销售趋势的 MySQL DAO。 */
final class MySqlStoreFriendPaymentRepository {
    FriendPaymentDto create(Connection c, long buyerId, FriendPaymentRequest r) {
        try {
            long payer = userId(c, r.getFriendAccount());
            String sql = "INSERT INTO store_friend_payments(order_id,buyer_id,payer_id,amount,message,expires_at) "
                    + "SELECT so.id,so.buyer_id,?,so.total_amount,?,CURRENT_TIMESTAMP(3)+INTERVAL 48 HOUR "
                    + "FROM store_orders so WHERE so.id=? AND so.buyer_id=? AND so.status='CREATED'";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, payer); if (r.getMessage() == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, r.getMessage());
                ps.setLong(3, r.getOrderId()); ps.setLong(4, buyerId);
                if (ps.executeUpdate() != 1) throw new StoreRepositoryException("订单不存在或不可代付");
                try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new StoreRepositoryException("代付编号生成失败"); return find(c, keys.getLong(1), false); }
            }
        } catch (SQLException ex) { throw fail("发起好友代付失败", ex); }
    }

    FriendPaymentPage list(Connection c, long userId, FriendPaymentQuery q) {
        FriendPaymentQuery safe = q == null ? new FriendPaymentQuery("INBOX") : q;
        boolean mine = "MINE".equalsIgnoreCase(safe.getScope());
        String column = mine ? "fp.buyer_id" : "fp.payer_id";
        String sql = "SELECT fp.id,fp.order_id,so.order_no,fp.buyer_id,bu.display_name buyer_name,fp.payer_id,"
                + "pu.display_name payer_name,fp.amount,CASE WHEN fp.status='PENDING' AND fp.expires_at<=CURRENT_TIMESTAMP(3) THEN 'EXPIRED' ELSE fp.status END status,"
                + "fp.message,fp.expires_at,fp.created_at FROM store_friend_payments fp JOIN store_orders so ON so.id=fp.order_id "
                + "JOIN users bu ON bu.id=fp.buyer_id JOIN users pu ON pu.id=fp.payer_id WHERE " + column + "=? "
                + "ORDER BY fp.created_at DESC,fp.id DESC LIMIT ? OFFSET ?";
        List<FriendPaymentDto> rows = new ArrayList<FriendPaymentDto>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId); ps.setInt(2, safe.getPageSize()); ps.setInt(3, safe.getOffset());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(read(rs)); }
            return new FriendPaymentPage(rows, count(c, column, userId));
        } catch (SQLException ex) { throw fail("查询好友代付失败", ex); }
    }

    FriendPaymentDto find(Connection c, long id, boolean lock) {
        String sql = "SELECT fp.id,fp.order_id,so.order_no,fp.buyer_id,bu.display_name buyer_name,fp.payer_id,"
                + "pu.display_name payer_name,fp.amount,CASE WHEN fp.status='PENDING' AND fp.expires_at<=CURRENT_TIMESTAMP(3) THEN 'EXPIRED' ELSE fp.status END status,"
                + "fp.message,fp.expires_at,fp.created_at FROM store_friend_payments fp JOIN store_orders so ON so.id=fp.order_id "
                + "JOIN users bu ON bu.id=fp.buyer_id JOIN users pu ON pu.id=fp.payer_id WHERE fp.id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? read(rs) : null; } }
        catch (SQLException ex) { throw fail("查询代付请求失败", ex); }
    }

    boolean updateStatus(Connection c, long id, String status) {
        String sql = "UPDATE store_friend_payments SET status=?,decided_at=CURRENT_TIMESTAMP(3) "
                + "WHERE id=? AND status='PENDING' AND (?='EXPIRED' OR expires_at>CURRENT_TIMESTAMP(3))";
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setString(1, status); ps.setLong(2, id); ps.setString(3, status); return ps.executeUpdate() == 1; }
        catch (SQLException ex) { throw fail("更新代付状态失败", ex); }
    }

    List<StoreSalesTrendDto> trend(Connection c, StoreSalesTrendQuery q) {
        StringBuilder sql = new StringBuilder("SELECT DATE(so.paid_at) day,SUM(items.qty) qty,SUM(so.total_amount) amount "
                + "FROM store_orders so JOIN (SELECT order_id,SUM(quantity) qty FROM store_order_items GROUP BY order_id) items ON items.order_id=so.id "
                + "WHERE so.status IN ('PAID','COMPLETED') AND so.paid_at IS NOT NULL");
        List<Object> args = new ArrayList<Object>();
        if (q.getStartDate() != null) { sql.append(" AND so.paid_at>=?"); args.add(JdbcTemporal.timestamp(q.getStartDate().atStartOfDay())); }
        if (q.getEndDate() != null) { sql.append(" AND so.paid_at<?"); args.add(JdbcTemporal.timestamp(q.getEndDate().plusDays(1L).atStartOfDay())); }
        sql.append(" GROUP BY DATE(so.paid_at) ORDER BY day");
        List<StoreSalesTrendDto> rows = new ArrayList<StoreSalesTrendDto>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) { int i=1; for (Object arg:args) ps.setObject(i++,arg); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) rows.add(new StoreSalesTrendDto(LocalDate.parse(rs.getString("day")),rs.getLong("qty"),rs.getBigDecimal("amount"))); } return rows; }
        catch (SQLException ex) { throw fail("查询销售趋势失败", ex); }
    }

    private static FriendPaymentDto read(ResultSet rs) throws SQLException {
        return new FriendPaymentDto(rs.getLong("id"),rs.getLong("order_id"),rs.getString("order_no"),rs.getLong("buyer_id"),rs.getString("buyer_name"),rs.getLong("payer_id"),rs.getString("payer_name"),rs.getBigDecimal("amount"),rs.getString("status"),rs.getString("message"),JdbcTemporal.localDateTime(rs.getTimestamp("expires_at")),JdbcTemporal.localDateTime(rs.getTimestamp("created_at")));
    }
    private static long userId(Connection c, String account) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT id FROM users WHERE username=? AND status='ACTIVE'")){ps.setString(1,account);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new StoreRepositoryException("好友账号不存在");return rs.getLong(1);}}
    }
    private static long count(Connection c,String column,long id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM store_friend_payments fp WHERE "+column+"=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getLong(1);}}}
    private static StoreRepositoryException fail(String m,Throwable x){return new StoreRepositoryException(m,x);}
}
