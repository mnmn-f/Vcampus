package edu.seu.vcampus.server.db;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Read-only verification of the optional expanded local MySQL data set. */
public final class ExpandedDemoDataMySqlIntegrationTest {
    private Connection connection;

    @Before
    public void connect() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        JdbcConnectionFactory factory = new JdbcConnectionFactory();
        connection = factory.open();
        Assume.assumeTrue(scalar("SELECT COUNT(*) FROM users WHERE username='test_student01'") > 0);
    }

    @After
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void everyMajorModuleHasEnoughRowsForPagingAndFiltering() throws Exception {
        assertAtLeast("users", 30);
        assertAtLeast("student_profiles", 20);
        assertAtLeast("teacher_profiles", 6);
        assertAtLeast("courses", 20);
        assertAtLeast("enrollments", 100);
        assertAtLeast("course_grades", 40);
        assertAtLeast("classrooms", 15);
        assertAtLeast("announcements", 20);
        assertAtLeast("books", 40);
        assertAtLeast("borrow_records", 30);
        assertAtLeast("products", 40);
        assertAtLeast("store_orders", 40);
        assertAtLeast("store_coupons", 10);
        assertAtLeast("leave_requests", 20);
        assertAtLeast("repair_orders", 20);
        assertAtLeast("classroom_reservations", 20);
        assertAtLeast("dorm_meter_readings", 20);
        assertAtLeast("dorm_absence_warnings", 20);
        assertAtLeast("hygiene_inspections", 15);
        assertAtLeast("dorm_hygiene_item_scores", 75);
        assertAtLeast("dorm_hygiene_tasks", 15);
        assertAtLeast("library_pdf_downloads", 15);
        assertAtLeast("ai_tool_call_logs", 20);
    }

    @Test
    public void primaryStudentCanExerciseHistoryMetricsAndCurrentFlows() throws Exception {
        assertTrue(scalar("SELECT COUNT(*) FROM enrollments e JOIN users u ON u.id=e.student_user_id "
                + "WHERE u.username='demo_student'") >= 16);
        assertTrue(scalar("SELECT COUNT(*) FROM course_grades g JOIN enrollments e ON e.id=g.enrollment_id "
                + "JOIN users u ON u.id=e.student_user_id WHERE u.username='demo_student'") >= 8);
        assertTrue(scalar("SELECT COUNT(DISTINCT c.semester_code) FROM enrollments e "
                + "JOIN courses c ON c.id=e.course_id JOIN users u ON u.id=e.student_user_id "
                + "WHERE u.username='demo_student'") >= 2);
        assertTrue(scalar("SELECT COUNT(DISTINCT o.status) FROM store_orders o "
                + "JOIN users u ON u.id=o.buyer_id WHERE u.username='demo_student'") >= 4);
        assertTrue(scalar("SELECT COUNT(*) FROM borrow_records b JOIN users u ON u.id=b.borrower_user_id "
                + "WHERE u.username='demo_student'") >= 10);
    }

    @Test
    public void expandedRowsRespectImportantRelationalAndAmountRules() throws Exception {
        assertEquals(0, scalar("SELECT COUNT(*) FROM store_orders "
                + "WHERE total_amount + discount_amount <> original_amount"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM store_order_items i "
                + "LEFT JOIN store_orders o ON o.id=i.order_id WHERE o.id IS NULL"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM course_grades "
                + "WHERE score<0 OR score>100 OR grade_point<0 OR grade_point>5"));
        assertTrue(scalar("SELECT COUNT(*) FROM products WHERE sku LIKE 'TEST-SKU-%' "
                + "AND image_url LIKE 'https://picsum.photos/%'") >= 40);
    }

    private void assertAtLeast(String table, long minimum) throws Exception {
        assertTrue(table + " should have at least " + minimum + " rows",
                scalar("SELECT COUNT(*) FROM `" + table + "`") >= minimum);
    }

    private long scalar(String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }
}
