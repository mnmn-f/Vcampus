package edu.seu.vcampus.server.db;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 静态守护 MySQL DAO 使用到的 V1 表/列，以及 V2 演示数据契约。 */
public final class DatabaseCompatibilityTest {
    private static final String[][] TABLE_COLUMNS = {
            {"users", "id,username,password_hash,display_name,email,status"},
            {"account_cancellation_requests", "id,user_id,reason,status,reviewed_by,reviewed_at,"
                    + "review_remark,created_at,updated_at,pending_user_id"},
            {"roles", "id,code,display_name"},
            {"user_roles", "user_id,role_id,assigned_by"},
            {"role_permissions", "role_id,permission_id"},
            {"student_profiles", "user_id,student_no,college,major,class_name,enrollment_year,"
                    + "expected_graduation_year,degree_level,gender,birth_date,address,emergency_contact,"
                    + "emergency_phone,status"},
            {"teacher_profiles", "user_id,employee_no,department,title,status"},
            {"classrooms", "id,building_name,room_no,classroom_type,capacity,equipment_description,status"},
            {"courses", "id,course_code,course_name,course_type,credits,total_hours,capacity,description,status,created_by"},
            {"course_instructors", "course_id,teacher_user_id,instructor_role"},
            {"course_schedules", "id,course_id,weekday,start_period,end_period,start_date,end_date,classroom_id"},
            {"enrollments", "id,student_user_id,course_id,status,dropped_at,version"},
            {"course_grades", "id,enrollment_id,score,grade_point,recorded_by,recorded_at,remark"},
            {"announcements", "id,module_code,title,content,visible_scope,target_role_id,status,publish_at,expire_at,publisher_id"},
            {"competitions", "id,title,description,organizer_id,start_at,end_at,registration_deadline,capacity,status"},
            {"competition_registrations", "competition_id,student_user_id,status,registered_at,cancelled_at"},
            {"srtp_records", "id,project_code,student_user_id,title,description,credits,status,submitted_at,reviewed_by,reviewed_at,review_remark"},
            {"classroom_reservations", "id,classroom_id,applicant_id,purpose,start_at,end_at,status,reviewed_by,reviewed_at,review_remark"},
            {"books", "id,isbn,title,author,publisher,category,total_copies,available_copies,location,description,status,created_by"},
            {"borrow_records", "id,book_id,borrower_user_id,issued_at,due_at,returned_at,status,renew_count,handled_by,remark"},
            {"study_rooms", "id,building_name,room_no,capacity,open_time,close_time,status,description"},
            {"study_room_reservations", "id,room_id,user_id,start_at,end_at,status,cancelled_at"},
            {"online_resources", "id,title,resource_type,url,description,publisher_id,status,published_at"},
            {"online_resource_access_logs", "id,resource_id,user_id,accessed_at,client_ip"},
            {"accounts", "id,user_id,balance,status,version"},
            {"account_transactions", "id,account_id,transaction_type,amount,balance_before,balance_after,reference_type,reference_id,idempotency_key,operator_id,remark"},
            {"products", "id,sku,name,category,description,price,stock_qty,status,created_by,version"},
            {"shopping_carts", "id,user_id,status"},
            {"cart_items", "cart_id,product_id,quantity"},
            {"store_orders", "id,order_no,buyer_id,total_amount,status,paid_at,cancelled_at,completed_at,version"},
            {"store_order_items", "id,order_id,product_id,product_name_snapshot,unit_price_snapshot,quantity,line_amount"},
            {"dorm_buildings", "id,building_code,building_name,address,gender_policy,status"},
            {"dorm_rooms", "id,building_id,room_no,floor_no,capacity,room_type,status,description"},
            {"dorm_beds", "id,room_id,bed_no,status"},
            {"accommodation_records", "id,student_user_id,bed_id,start_date,end_date,status,created_by"},
            {"accommodation_requests", "id,student_user_id,request_type,current_record_id,requested_bed_id,reason,status,reviewed_by,reviewed_at,review_remark"},
            {"access_records", "id,student_user_id,record_type,occurred_at,door_name,source,note"},
            {"late_return_alerts", "id,student_user_id,alert_date,detected_at,status,handled_by,handled_at,note"},
            {"hygiene_inspections", "id,room_id,inspector_id,inspected_at,score,result,issue_description,status,rectified_at,rectification_note"},
            {"repair_orders", "id,room_id,reporter_id,category,description,priority,status,handler_id,accepted_at,completed_at,evaluation_score,evaluation_note"},
            {"utility_bills", "id,room_id,period_start,period_end,electricity_units,water_units,total_amount,due_at,status,created_by"},
            {"utility_allocations", "id,bill_id,student_user_id,amount,status,paid_transaction_id,paid_at"}
    };

    @Test
    public void daoOwnedTablesExposeExpectedColumns() throws Exception {
        String schema = resource("/db/migration/V1__baseline.sql");
        for (String[] table : TABLE_COLUMNS) {
            String block = tableBlock(schema, table[0]);
            for (String column : table[1].split(",")) {
                String name = column.trim();
                assertTrue(table[0] + "." + name, block.contains("`" + name + "`"));
            }
        }
    }

    @Test
    public void mysql8ConstraintsAndDemoSeedContractsArePresent() throws Exception {
        String schema = resource("/db/migration/V1__baseline.sql");
        String seed = resource("/db/migration/V2__demo_data.sql");
        assertTrue(schema.contains("CONSTRAINT `ck_announcements_scope_role`"));
        assertTrue(schema.contains("UNIQUE KEY `uk_account_cancellation_pending_user`"));
        assertTrue(schema.contains("UNIQUE KEY `uk_student_profiles_student_no`"));
        assertTrue(schema.contains("UNIQUE KEY `uk_teacher_profiles_employee_no`"));
        assertTrue(schema.contains("CONSTRAINT `ck_account_cancellation_review`"));
        assertTrue(schema.contains("CONSTRAINT `ck_utility_allocations_paid`"));
        assertTrue(schema.contains("FOREIGN KEY (`target_role_id`) REFERENCES `roles` (`id`),"));
        assertTrue(schema.contains("FOREIGN KEY (`paid_transaction_id`) REFERENCES `account_transactions` (`id`),"));
        assertTrue(schema.contains("WHERE so.`status` IN ('PAID', 'COMPLETED')"));
        assertFalse(schema.contains("WHERE so.`status` IN ('PAID', 'COMPLETED', 'REFUNDED')"));
        assertTrue(seed.contains("START TRANSACTION;"));
        assertTrue(seed.contains("COMMIT;"));
        assertTrue(seed.contains("`srtp_records`"));
        assertFalse(seed.contains("VALUES(`"));
        String[] demoUsers = {"demo_student", "demo_teacher", "demo_registrar", "demo_academic",
                "demo_librarian", "demo_store", "demo_dorm", "demo_ai", "demo_system"};
        for (String user : demoUsers) {
            assertTrue(user, seed.contains("'" + user + "'"));
        }
    }

    private static String tableBlock(String schema, String table) {
        String marker = "CREATE TABLE IF NOT EXISTS `" + table + "` (";
        int start = schema.indexOf(marker);
        assertTrue("missing table " + table, start >= 0);
        int end = schema.indexOf(") ENGINE", start);
        assertTrue("unterminated table " + table, end > start);
        return schema.substring(start, end);
    }

    private static String resource(String name) throws Exception {
        InputStream input = DatabaseCompatibilityTest.class.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("missing resource " + name);
        }
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
