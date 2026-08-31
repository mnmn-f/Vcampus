package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormBillingRepository;
import edu.seu.vcampus.server.dorm.repository.DormPaymentPort;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** MySQL 水电分摊查询与支付；支付由同一连接上的最小账户端口完成。 */
public final class MySqlDormBillingRepository implements DormBillingRepository {
    private static final String SELECT = "SELECT ua.id allocation_id,ub.id bill_id,dr.id room_id,ua.student_user_id,dr.room_no,"
            + "ub.period_start,ub.period_end,ub.electricity_units,ub.water_units,ub.total_amount,ua.amount allocated_amount,"
            + "ub.status bill_status,ua.status allocation_status,ub.due_at,ua.paid_transaction_id,ua.paid_at";
    private static final String FROM = " FROM utility_allocations ua JOIN utility_bills ub ON ub.id=ua.bill_id"
            + " JOIN dorm_rooms dr ON dr.id=ub.room_id";
    private final DormPaymentPort payments;

    public MySqlDormBillingRepository() { this(new MySqlDormPaymentAdapter()); }
    public MySqlDormBillingRepository(DormPaymentPort payments) { this.payments = payments; }

    @Override
    public DormPage<UtilityBillDto> listBills(Connection c, long student, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE ua.student_user_id=?";
        p.add(Long.valueOf(student));
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND ua.status=?"; p.add(query.getStatus().trim()); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                SELECT + FROM + where + " ORDER BY ub.period_end DESC,ua.id DESC LIMIT ? OFFSET ?",
                p, query.getPage(), query.getPageSize(), new JdbcDormSupport.Reader<UtilityBillDto>() { public UtilityBillDto read(ResultSet r) throws SQLException { return JdbcDormSupport.bill(r); } });
    }

    @Override
    public DormPage<UtilityBillDto> listAllBills(Connection c, UtilityBillQuery q) throws SQLException {
        UtilityBillQuery query = q == null ? UtilityBillQuery.all() : q;
        List<Object> params = new ArrayList<Object>();
        String where = " WHERE 1=1";
        String keyword = JdbcDormSupport.clean(query.getKeyword());
        if (keyword != null) {
            String like = "%" + keyword + "%";
            where += " AND (dr.room_no LIKE ? OR dr.building_code LIKE ?"
                    + " OR CAST(ua.student_user_id AS CHAR) LIKE ?"
                    + " OR DATE_FORMAT(ub.period_start,'%Y-%m-%d') LIKE ?"
                    + " OR DATE_FORMAT(ub.period_end,'%Y-%m-%d') LIKE ?)";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (JdbcDormSupport.clean(query.getStatus()) != null) {
            where += " AND ua.status=?";
            params.add(query.getStatus().trim());
        }
        if (query.getRoomId() != null) {
            where += " AND ub.room_id=?";
            params.add(query.getRoomId());
        }
        if (query.getPeriodStart() != null) {
            where += " AND ub.period_end>=?";
            params.add(JdbcTemporal.date(query.getPeriodStart()));
        }
        if (query.getPeriodEnd() != null) {
            where += " AND ub.period_start<=?";
            params.add(JdbcTemporal.date(query.getPeriodEnd()));
        }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                SELECT + FROM + where + " ORDER BY ub.period_end DESC,ua.id DESC LIMIT ? OFFSET ?",
                params, query.getPage(), query.getPageSize(), new JdbcDormSupport.Reader<UtilityBillDto>() { public UtilityBillDto read(ResultSet r) throws SQLException { return JdbcDormSupport.bill(r); } });
    }

    @Override
    public UtilityBillDto lockAllocation(Connection c, long student, long allocationId) throws SQLException {
        String sql = SELECT + FROM + " WHERE ua.id=? AND ua.student_user_id=? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, allocationId); s.setLong(2, student);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new DormRepositoryException(DormCommands.BILL_NOT_FOUND, "水电分摊不存在");
                return JdbcDormSupport.bill(r);
            }
        }
    }

    @Override
    public UtilityBillDto pay(Connection c, long student, UtilityPaymentRequest request) throws SQLException {
        UtilityBillDto old = lockAllocation(c, student, request.getAllocationId());
        if ("PAID".equals(old.getAllocationStatus())) {
            throw new DormRepositoryException(DormCommands.BILL_ALREADY_PAID, "该分摊已经缴费");
        }
        if (!"UNPAID".equals(old.getAllocationStatus())) {
            throw new DormRepositoryException(DormCommands.BILL_NOT_PAYABLE, "该分摊当前不可缴费");
        }
        long transactionId = payments.charge(c, student, old.getAllocatedAmount(),
                request.getIdempotencyKey(), old.getAllocationId());
        try (PreparedStatement s = c.prepareStatement("UPDATE utility_allocations SET status='PAID',paid_transaction_id=?,paid_at=CURRENT_TIMESTAMP(3) WHERE id=? AND status='UNPAID'")) {
            s.setLong(1, transactionId); s.setLong(2, old.getAllocationId());
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.PAYMENT_DUPLICATE, "重复支付请求");
        }
        try (PreparedStatement s = c.prepareStatement("UPDATE utility_bills SET status=CASE WHEN NOT EXISTS(SELECT 1 FROM utility_allocations WHERE bill_id=? AND status='UNPAID') THEN 'PAID' ELSE 'PARTIAL' END WHERE id=?")) {
            s.setLong(1, old.getBillId()); s.setLong(2, old.getBillId()); s.executeUpdate();
        }
        return lockAllocation(c, student, old.getAllocationId());
    }
}
