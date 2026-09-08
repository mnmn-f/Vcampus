package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.dorm.repository.mysql.MySqlDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Optional V5 smoke test; it is skipped unless a real MySQL instance is requested. */
public final class MySqlDormExtIntegrationTest {
    private static final LocalDate START = LocalDate.of(2099, 1, 1);
    private static final LocalDate END = LocalDate.of(2099, 1, 31);
    private TransactionManager transactions;
    private DormExtService service;
    private SessionContext admin;
    private long roomId;
    private long adminId;

    @Before public void setUp() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        transactions = new TransactionManager(new JdbcConnectionFactory());
        service = new DormExtService(new MySqlDormExtRepository(), transactions);
        transactions.execute(new TransactionWork<Void>() {
            @Override public Void execute(Connection c) throws Exception {
                adminId = id(c, "demo_dorm");
                roomId = scalar(c, "SELECT id FROM dorm_rooms ORDER BY id LIMIT 1");
                deleteReading(c);
                return null;
            }
        });
        admin = new SessionContext("mysql-dorm-ext-test", adminId, "demo_dorm", "宿管员",
                EnumSet.of(Role.DORM_MANAGER), Role.DORM_MANAGER);
    }

    @After public void cleanUp() throws Exception {
        if (transactions == null || roomId <= 0) return;
        transactions.execute(new TransactionWork<Void>() { @Override public Void execute(Connection c) throws Exception {
            deleteReading(c);
            return null;
        } });
    }

    @Test public void v5MeterWriteAndQueryRoundTrip() {
        MeterReadingDto written = service.saveMeterReading(admin, new MeterReadingRequest(roomId, START, END, new BigDecimal("12.500"), new BigDecimal("3.250"), new BigDecimal("0.80"), new BigDecimal("4.20")));
        assertNotNull(written);
        assertEquals(roomId, written.getRoomId());
        assertEquals(new BigDecimal("23.65"), written.getTotalAmount());
        DormPageQuery query = new DormPageQuery(1, 20, null, null, null,
                Long.valueOf(roomId));
        assertTrue(service.meterReadings(admin, query).getItems().stream()
                .anyMatch(row -> row.getId() == written.getId()));
    }

    private static long id(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("required V5 user fixture missing");
                return rs.getLong(1);
            }
        }
    }

    private static long scalar(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("required V5 room fixture missing");
            return rs.getLong(1);
        }
    }

    private void deleteReading(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM dorm_meter_readings WHERE room_id=? "
                        + "AND period_start=? AND period_end=?")) {
            ps.setLong(1, roomId);
            ps.setDate(2, java.sql.Date.valueOf(START.toString()));
            ps.setDate(3, java.sql.Date.valueOf(END.toString()));
            ps.executeUpdate();
        }
    }
}
