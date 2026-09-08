package edu.seu.vcampus.server.db;

import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.server.academic.repository.mysql.MySqlAcademicRepository;
import edu.seu.vcampus.server.campus.repository.mysql.MySqlCampusRepository;
import edu.seu.vcampus.server.dorm.repository.mysql.MySqlDormRepository;
import edu.seu.vcampus.server.library.repository.mysql.MySqlBookRepository;
import edu.seu.vcampus.server.store.repository.mysql.MySqlStoreRecordRepository;
import edu.seu.vcampus.server.student.repository.mysql.MySqlStudentRecordRepository;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 可选真实 MySQL 8 验证；默认跳过，避免单元测试依赖外部数据库。 */
public final class MySqlDaoIntegrationTest {
    private TransactionManager transactions;
    private MySqlAcademicRepository academic;
    private long studentId;
    private long teacherId;
    private long academicAdminId;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        String url = System.getProperty("vcampus.db.url", JdbcConnectionFactory.DEFAULT_URL);
        String user = System.getProperty("vcampus.db.user", JdbcConnectionFactory.DEFAULT_USER);
        String password = System.getProperty("vcampus.db.password", JdbcConnectionFactory.DEFAULT_PASSWORD);
        JdbcConnectionFactory factory = new JdbcConnectionFactory(url, user, password);
        transactions = new TransactionManager(factory);
        academic = new MySqlAcademicRepository(factory);
        loadDemoIds();
    }

    @Test
    public void everyCompletedModuleCanReadItsSeededRows() throws Exception {
        transactions.execute(new TransactionWork<Void>() {
            @Override
            public Void execute(Connection connection) throws Exception {
                assertTrue(new MySqlStudentRecordRepository().findProfile(connection, studentId) != null);
                assertTrue(academic.findCourses(connection, new CourseQuery()).getTotalElements() > 0);
                assertTrue(new MySqlBookRepository().search(connection, new BookSearchRequest()).getTotal() > 0);
                assertTrue(new MySqlStoreRecordRepository().searchProducts(connection, new ProductQuery()).getTotalElements() > 0);
                assertTrue(new MySqlDormRepository().listBuildings(connection, new DormPageQuery()).getTotalElements() > 0);
                assertTrue(new MySqlCampusRepository().listClassrooms(connection, new CampusPageQuery()).getTotalElements() > 0);
                return null;
            }
        });
    }

    @Test
    public void courseWriteRollsBackAndLeavesNoRow() throws Exception {
        final String code = "DBIT-" + System.currentTimeMillis();
        try {
            transactions.execute(new TransactionWork<Void>() {
                @Override
                public Void execute(Connection connection) throws Exception {
                    CourseSaveRequest request = CourseSaveRequest.create(code, "数据库回滚探针",
                            CourseType.ELECTIVE, BigDecimal.ONE, 16, 2, "integration",
                            CourseStatus.DRAFT, Collections.singletonList(teacherId));
                    academic.saveCourse(connection, request, academicAdminId);
                    throw new SQLException("intentional rollback probe");
                }
            });
            fail("rollback probe must fail the transaction");
        } catch (SQLException expected) {
            assertEquals("intentional rollback probe", expected.getMessage());
        }
        CoursePageDto page = transactions.execute(new TransactionWork<CoursePageDto>() {
            @Override
            public CoursePageDto execute(Connection connection) throws Exception {
                return academic.findCourses(connection, new CourseQuery(1, 20, code, null, null));
            }
        });
        assertEquals(0L, page.getTotalElements());
    }

    @Test
    public void mysqlChecksRejectBedOccupancyAndPaidAllocationConflicts() throws Exception {
        final long bedId = scalar("SELECT bed_id FROM accommodation_records "
                + "WHERE student_user_id=? AND status='ACTIVE'", studentId);
        final long billId = scalar("SELECT id FROM utility_bills LIMIT 1");
        expectConstraint(new TransactionWork<Void>() {
            @Override
            public Void execute(Connection connection) throws Exception {
                runUpdate(connection, "INSERT INTO accommodation_records "
                        + "(student_user_id,bed_id,start_date,status,created_by) VALUES(?,?,CURRENT_DATE,'ACTIVE',?)",
                        teacherId, bedId, academicAdminId);
                throw new SQLException("bed-conflict-probe-succeeded");
            }
        }, "bed-conflict-probe-succeeded");
        expectConstraint(new TransactionWork<Void>() {
            @Override
            public Void execute(Connection connection) throws Exception {
                runUpdate(connection, "INSERT INTO utility_allocations "
                        + "(bill_id,student_user_id,amount,status) VALUES(?,?,1.00,'PAID')",
                        billId, teacherId);
                throw new SQLException("paid-check-probe-succeeded");
            }
        }, "paid-check-probe-succeeded");
    }

    private void loadDemoIds() throws Exception {
        transactions.execute(new TransactionWork<Void>() {
            @Override
            public Void execute(Connection connection) throws Exception {
                studentId = id(connection, "demo_student");
                teacherId = id(connection, "demo_teacher");
                academicAdminId = id(connection, "demo_academic");
                return null;
            }
        });
    }

    private long scalar(final String sql, final Object... values) throws Exception {
        return transactions.execute(new TransactionWork<Long>() {
            @Override
            public Long execute(Connection connection) throws Exception {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (int i = 0; i < values.length; i++) {
                        statement.setObject(i + 1, values[i]);
                    }
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) return result.getLong(1);
                    }
                }
                throw new SQLException("missing probe row");
            }
        });
    }

    private void expectConstraint(TransactionWork<Void> work, String successMarker)
            throws Exception {
        try {
            transactions.execute(work);
            fail("expected MySQL constraint violation");
        } catch (SQLException expected) {
            assertTrue(!successMarker.equals(expected.getMessage()));
        }
    }

    private static void runUpdate(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    private static long id(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong(1);
                }
            }
        }
        throw new SQLException("missing demo user " + username);
    }
}
