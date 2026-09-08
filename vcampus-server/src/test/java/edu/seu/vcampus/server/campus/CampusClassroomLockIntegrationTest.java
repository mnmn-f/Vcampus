package edu.seu.vcampus.server.campus;

import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.campus.registry.CampusCommandRegistry;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Assume;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.threeten.bp.LocalDateTime;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/** 两个同教室审批并发执行，验证申请/审批使用同一 classroom→reservation 锁序。 */
public final class CampusClassroomLockIntegrationTest {
    @Test public void concurrentApprovalsSerializeWithoutDeadlock() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        JdbcConnectionFactory factory = new JdbcConnectionFactory(System.getProperty(
                "vcampus.db.url", JdbcConnectionFactory.DEFAULT_URL), System.getProperty(
                "vcampus.db.user", JdbcConnectionFactory.DEFAULT_USER), System.getProperty(
                "vcampus.db.password", JdbcConnectionFactory.DEFAULT_PASSWORD));
        final TransactionManager transactions = new TransactionManager(factory);
        final CampusService campus = CampusCommandRegistry.createMySqlService(transactions);
        final long classroomId = scalar(transactions,
                "SELECT id FROM classrooms WHERE status='AVAILABLE' ORDER BY id LIMIT 1");
        final long applicantId = scalar(transactions,
                "SELECT id FROM users WHERE username='demo_student'");
        final long reviewerId = scalar(transactions,
                "SELECT id FROM users WHERE username='demo_academic'");
        final long[] reservations = seed(transactions, classroomId, applicantId);
        SessionManager sessions = new SessionManager();
        final SessionContext reviewer = sessions.createSession(reviewerId, "demo_academic",
                "演示教务老师", EnumSet.of(Role.ACADEMIC_ADMIN), Role.ACADEMIC_ADMIN);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ClassroomReservationDto> first = submit(pool, start, campus, reviewer, reservations[0]);
            Future<ClassroomReservationDto> second = submit(pool, start, campus, reviewer, reservations[1]);
            start.countDown();
            assertEquals("APPROVED", first.get(15L, TimeUnit.SECONDS).getStatus());
            assertEquals("APPROVED", second.get(15L, TimeUnit.SECONDS).getStatus());
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5L, TimeUnit.SECONDS);
            cleanup(transactions, reservations);
        }
    }

    private static Future<ClassroomReservationDto> submit(ExecutorService pool,
            final CountDownLatch start, final CampusService campus, final SessionContext reviewer,
            final long reservationId) {
        return pool.submit(new java.util.concurrent.Callable<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto call() throws Exception {
                start.await();
                return campus.reviewClassroom(reviewer,
                        new ClassroomReviewRequest(reservationId, true, "并发锁序探针"));
            }
        });
    }

    private static long[] seed(TransactionManager tx, final long classroom, final long applicant)
            throws Exception {
        return tx.execute(new TransactionWork<long[]>() {
            @Override public long[] execute(Connection c) throws Exception {
                return new long[] { insert(c, classroom, applicant, 9), insert(c, classroom, applicant, 10) };
            }
        });
    }

    private static long insert(Connection c, long classroom, long applicant, int hour)
            throws Exception {
        String sql = "INSERT INTO classroom_reservations(classroom_id,applicant_id,purpose,start_at,end_at,status) "
                + "VALUES(?,?,?,?,?,'PENDING')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, classroom); ps.setLong(2, applicant); ps.setString(3, "并发锁序探针");
            ps.setObject(4, LocalDateTime.of(2099, 1, 1, hour, 0));
            ps.setObject(5, LocalDateTime.of(2099, 1, 1, hour + 1, 0));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("reservation id missing");
                return rs.getLong(1);
            }
        }
    }

    private static long scalar(TransactionManager tx, final String sql) throws Exception {
        return tx.execute(new TransactionWork<Long>() {
            @Override public Long execute(Connection c) throws Exception {
                try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Long.valueOf(rs.getLong(1));
                }
                throw new SQLException("missing lock-order probe row");
            }
        }).longValue();
    }

    private static void cleanup(TransactionManager tx, final long[] ids) throws Exception {
        tx.execute(new TransactionWork<Void>() {
            @Override public Void execute(Connection c) throws Exception {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM classroom_reservations WHERE id IN (?,?)")) {
                    ps.setLong(1, ids[0]); ps.setLong(2, ids[1]); ps.executeUpdate();
                }
                return null;
            }
        });
    }
}
