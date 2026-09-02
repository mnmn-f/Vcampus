-- VCampus deterministic demo seed for MySQL 8.
-- Run V1__baseline.sql first and execute this file in the vcampus database.
-- The script is safe to re-run for the demo database: natural keys and
-- idempotency keys prevent duplicate seed rows.
--
-- Passwords below belong to local demo accounts and are listed in README.md.
-- They must be replaced or the demo accounts disabled before any real deployment.

USE `vcampus`;
SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';

START TRANSACTION;

-- ============================================================================
-- Roles and permissions (codes mirror vcampus-common Role/Permission enums)
-- ============================================================================

INSERT INTO `roles` (`code`, `display_name`, `description`)
VALUES
    ('STUDENT', '学生', '查看个人数据并办理校园业务'),
    ('TEACHER', '任课教师', '查看授课课程并登记本人课程成绩'),
    ('REGISTRAR', '学籍管理员', '维护学生学籍与成绩档案'),
    ('ACADEMIC_ADMIN', '教务老师', '维护课程、教务公告、比赛、SRTP与教室审批'),
    ('LIBRARIAN', '图书管理员', '维护图书、借还、自习室与线上资源'),
    ('STORE_MANAGER', '商店管理员', '维护商品、库存、订单与销售统计'),
    ('DORM_MANAGER', '宿管员', '处理住宿、门禁、卫生、报修与水电业务'),
    ('AI_KNOWLEDGE_ADMIN', 'AI知识管理员', '维护AI知识片段并监控调用日志'),
    ('SYSTEM_ADMIN', '系统管理员', '维护账号、角色和系统运行状态')
AS new
ON DUPLICATE KEY UPDATE
    `display_name` = new.display_name,
    `description` = new.description;

INSERT INTO `permissions` (`code`, `display_name`, `description`)
VALUES
    ('PROFILE_READ', '查看个人资料', '读取当前用户公共资料'),
    ('PROFILE_UPDATE', '修改个人资料', '修改当前用户可编辑资料'),
    ('STUDENT_RECORD_SELF_READ', '查看本人学籍', '学生读取本人学籍档案'),
    ('STUDENT_RECORD_MANAGE', '管理学籍档案', '学籍管理员维护学生档案'),
    ('SCORE_SELF_READ', '查看本人成绩', '学生读取本人成绩'),
    ('SCORE_RECORD', '登记或核对成绩', '教师登记本人课程成绩或管理员核对'),
    ('COURSE_READ', '查看课程', '查询课程和课程时段'),
    ('COURSE_MANAGE', '管理课程', '教务老师维护课程与排课'),
    ('COURSE_ENROLL', '选退课程', '学生办理选课和退课'),
    ('COURSE_TEACH', '授课管理', '教师查看本人授课课程'),
    ('ANNOUNCEMENT_READ', '查看公告', '读取有权范围内的公告'),
    ('ANNOUNCEMENT_MANAGE', '管理公告', '维护所属业务公告'),
    ('COMPETITION_ENROLL', '报名比赛', '学生报名或取消比赛'),
    ('COMPETITION_MANAGE', '管理比赛', '教务老师维护比赛与报名名单'),
    ('SRTP_SELF_READ', '查看本人SRTP', '学生查看本人SRTP记录'),
    ('SRTP_MANAGE', '管理SRTP', '教务老师登记与审核SRTP'),
    ('CLASSROOM_RESERVE', '申请教室', '学生或教师提交教室申请'),
    ('CLASSROOM_APPROVE', '审批教室', '教务老师审批教室申请'),
    ('LIBRARY_READ', '查看图书馆', '查询图书、资源和自习室'),
    ('LIBRARY_BORROW', '借还图书', '办理本人借书、还书和预约'),
    ('LIBRARY_MANAGE', '管理图书馆', '图书管理员维护馆藏和资源'),
    ('STUDY_ROOM_RESERVE', '预约自习室', '用户预约和取消自习室'),
    ('STUDY_ROOM_MANAGE', '管理自习室', '图书管理员维护自习室'),
    ('STORE_READ', '查看商店', '查询商品与订单状态'),
    ('STORE_PURCHASE', '购买商品', '创建订单并完成支付'),
    ('STORE_MANAGE', '管理商店', '维护商品、库存和订单'),
    ('STORE_SALES_READ', '查看销售统计', '查询已支付订单统计'),
    ('DORM_SELF_READ', '查看本人住宿', '查看本人住宿、门禁和账单'),
    ('DORM_REQUEST', '提交宿舍申请', '提交入住、调宿、退宿、请假和报修'),
    ('DORM_BILL_PAY', '缴纳水电费', '支付本人水电分摊'),
    ('DORM_MANAGE', '管理宿舍基础数据', '维护楼栋、房间、床位等'),
    ('DORM_APPROVE', '审批住宿事务', '审批入住、调宿、退宿、请假'),
    ('DORM_GOVERN', '处理宿舍治理', '处理门禁、卫生、报修和水电'),
    ('AI_QUERY', '使用AI助手', '创建会话和查询授权数据'),
    ('AI_KNOWLEDGE_MANAGE', '管理AI知识库', '维护知识片段'),
    ('USER_MANAGE', '管理用户', '维护账号状态'),
    ('ROLE_MANAGE', '管理角色权限', '分配和撤销用户角色'),
    ('SYSTEM_MONITOR', '查看系统状态', '查看运行和AI调用日志')
AS new
ON DUPLICATE KEY UPDATE
    `display_name` = new.display_name,
    `description` = new.description;

-- Role policy is intentionally explicit and is kept in sync with RolePolicy.java.
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'STUDENT_RECORD_SELF_READ', 'SCORE_SELF_READ',
                  'COURSE_READ', 'COURSE_ENROLL', 'ANNOUNCEMENT_READ', 'COMPETITION_ENROLL',
                  'SRTP_SELF_READ', 'CLASSROOM_RESERVE', 'LIBRARY_READ', 'LIBRARY_BORROW',
                  'STUDY_ROOM_RESERVE', 'STORE_READ', 'STORE_PURCHASE', 'DORM_SELF_READ',
                  'DORM_REQUEST', 'DORM_BILL_PAY', 'AI_QUERY')
WHERE r.`code` = 'STUDENT'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'COURSE_READ', 'COURSE_TEACH',
                  'SCORE_RECORD', 'ANNOUNCEMENT_READ', 'CLASSROOM_RESERVE', 'LIBRARY_READ')
WHERE r.`code` = 'TEACHER'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'STUDENT_RECORD_MANAGE', 'SCORE_RECORD', 'ANNOUNCEMENT_READ')
WHERE r.`code` = 'REGISTRAR'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'COURSE_READ', 'COURSE_MANAGE', 'ANNOUNCEMENT_MANAGE',
                  'COMPETITION_MANAGE', 'SRTP_MANAGE', 'CLASSROOM_APPROVE')
WHERE r.`code` = 'ACADEMIC_ADMIN'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'LIBRARY_READ', 'LIBRARY_MANAGE', 'STUDY_ROOM_MANAGE',
                  'ANNOUNCEMENT_MANAGE')
WHERE r.`code` = 'LIBRARIAN'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'STORE_READ', 'STORE_MANAGE', 'STORE_SALES_READ')
WHERE r.`code` = 'STORE_MANAGER'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'DORM_MANAGE', 'DORM_APPROVE', 'DORM_GOVERN',
                  'ANNOUNCEMENT_MANAGE')
WHERE r.`code` = 'DORM_MANAGER'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'AI_KNOWLEDGE_MANAGE', 'SYSTEM_MONITOR')
WHERE r.`code` = 'AI_KNOWLEDGE_ADMIN'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'USER_MANAGE', 'ROLE_MANAGE', 'SYSTEM_MONITOR')
WHERE r.`code` = 'SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

-- Remove obsolete AI grants when this idempotent seed is rerun on an older demo DB.
DELETE rp FROM `role_permissions` rp
JOIN `roles` r ON r.`id` = rp.`role_id`
JOIN `permissions` p ON p.`id` = rp.`permission_id`
WHERE p.`code` = 'AI_QUERY'
  AND r.`code` IN ('TEACHER', 'AI_KNOWLEDGE_ADMIN');

-- ============================================================================
-- Demo users. These deterministic login names have documented development-only
-- passwords so the real TCP/MySQL path can be exercised after seeding.
-- ============================================================================

INSERT INTO `users` (`username`, `password_hash`, `display_name`, `email`, `status`)
VALUES
    ('demo_student', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生', 'demo.student@vcampus.local', 'ACTIVE'),
    ('demo_teacher', '$2a$10$5WUEQdJ2eGj6V6mC1ky2mu0iPPB/qTqMk8bUpEdpOD.9kvyzjiEaG', '演示教师兼教务员', 'demo.teacher@vcampus.local', 'ACTIVE'),
    ('demo_registrar', '$2a$10$5ekn2HRLljAFzcUnmZKaEOP6Hv3lmPwYClugS0LwmC3N3NgwZRvtC', '演示学籍管理员', 'demo.registrar@vcampus.local', 'ACTIVE'),
    ('demo_academic', '$2a$10$yjcvppAQlhH5kKE2Zd8oeOPydQJ4fYH1UNWMa3qSTa5ojeG6U3RY2', '演示教务老师', 'demo.academic@vcampus.local', 'ACTIVE'),
    ('demo_librarian', '$2a$10$1EGZqHksUfmAFpbNUUYjmO6fLznNlLMJeMPzcV7Z1tjTyCoO.pBuK', '演示图书管理员', 'demo.librarian@vcampus.local', 'ACTIVE'),
    ('demo_store', '$2a$10$MUcAM/wOO/dLPxIn4tBQTuXnQv7/2Zk5sGcCQSzcGLbtisRIqUSSm', '演示商店管理员', 'demo.store@vcampus.local', 'ACTIVE'),
    ('demo_dorm', '$2a$10$zXg9yG2WFWzADsSoTBNJJOpJAsUU2Yw.x7YhZzsh/8inwZIMSZEV6', '演示宿管员', 'demo.dorm@vcampus.local', 'ACTIVE'),
    ('demo_ai', '$2a$10$5vJUUJ5qZusyhaLyDWE/MO4.cbpaoJF7Ccav66mjmDxmII0rZlmYu', '演示AI知识管理员', 'demo.ai@vcampus.local', 'ACTIVE'),
    ('demo_system', '$2a$10$ffS7jMPz9A0Rnz9bYzraxeDWeksow9bBW426QJBBLs4EiJJvVBgLi', '演示系统管理员', 'demo.system@vcampus.local', 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE
    `password_hash` = new.password_hash,
    `display_name` = new.display_name,
    `email` = new.email;

SET @student_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_student');
SET @teacher_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_teacher');
SET @registrar_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_registrar');
SET @academic_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_academic');
SET @librarian_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_librarian');
SET @store_manager_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_store');
SET @dorm_manager_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_dorm');
SET @ai_admin_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_ai');
SET @system_admin_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_system');

INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @student_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'STUDENT'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @teacher_id, `id`, @system_admin_id FROM `roles` WHERE `code` IN ('TEACHER', 'ACADEMIC_ADMIN')
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @registrar_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'REGISTRAR'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @academic_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'ACADEMIC_ADMIN'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @librarian_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'LIBRARIAN'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @store_manager_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'STORE_MANAGER'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @dorm_manager_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'DORM_MANAGER'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @ai_admin_id, `id`, @system_admin_id FROM `roles` WHERE `code` = 'AI_KNOWLEDGE_ADMIN'
ON DUPLICATE KEY UPDATE `assigned_by` = @system_admin_id;
INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @system_admin_id, `id`, NULL FROM `roles` WHERE `code` = 'SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE `assigned_by` = NULL;

INSERT INTO `student_profiles` (`user_id`, `student_no`, `college`, `major`, `class_name`, `enrollment_year`, `expected_graduation_year`, `degree_level`, `gender`, `status`)
VALUES (@student_id, 'DEMO2026001', '计算机科学与工程学院', '软件工程', '软件工程2601', 2026, 2030, 'UNDERGRADUATE', 'UNKNOWN', 'ENROLLED')
AS new
ON DUPLICATE KEY UPDATE
    `college` = new.college, `major` = new.major, `class_name` = new.class_name, `status` = new.status;

INSERT INTO `teacher_profiles` (`user_id`, `employee_no`, `department`, `title`, `status`)
VALUES (@teacher_id, 'DEMO-T-001', '计算机科学与工程学院', '讲师', 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE `department` = new.department, `title` = new.title, `status` = new.status;

-- ============================================================================
-- Academic demo rows
-- ============================================================================

INSERT INTO `classrooms` (`building_name`, `room_no`, `classroom_type`, `capacity`, `equipment_description`, `status`)
VALUES ('九龙湖计算机楼', 'B201', 'TEACHING', 60, '投影、电子讲台、网络', 'AVAILABLE')
AS new
ON DUPLICATE KEY UPDATE `capacity` = new.capacity, `status` = new.status;
SET @classroom_id = (SELECT `id` FROM `classrooms` WHERE `building_name` = '九龙湖计算机楼' AND `room_no` = 'B201');

INSERT INTO `courses` (`course_code`, `course_name`, `course_type`, `credits`, `total_hours`, `capacity`, `description`, `status`, `created_by`)
VALUES ('DEMO-SE-001', '软件工程实践', 'PRACTICE', 3.00, 48, 50, '用于演示课程查询、选课、课表与成绩链路。', 'PUBLISHED', @teacher_id)
AS new
ON DUPLICATE KEY UPDATE `course_name` = new.course_name, `status` = new.status, `capacity` = new.capacity;
SET @course_id = (SELECT `id` FROM `courses` WHERE `course_code` = 'DEMO-SE-001');

INSERT INTO `course_instructors` (`course_id`, `teacher_user_id`, `instructor_role`)
VALUES (@course_id, @teacher_id, 'PRIMARY')
AS new
ON DUPLICATE KEY UPDATE `instructor_role` = new.instructor_role;

INSERT INTO `course_schedules` (`course_id`, `weekday`, `start_period`, `end_period`, `start_date`, `end_date`, `classroom_id`)
SELECT @course_id, 2, 1, 2, '2026-09-01', '2026-12-31', @classroom_id
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `course_schedules`
    WHERE `course_id` = @course_id AND `weekday` = 2 AND `start_period` = 1
      AND `end_period` = 2 AND `start_date` = '2026-09-01'
);

INSERT INTO `enrollments` (`student_user_id`, `course_id`, `status`)
VALUES (@student_id, @course_id, 'COMPLETED')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status;
SET @enrollment_id = (SELECT `id` FROM `enrollments` WHERE `student_user_id` = @student_id AND `course_id` = @course_id);

INSERT INTO `course_grades` (`enrollment_id`, `score`, `grade_point`, `recorded_by`, `remark`)
VALUES (@enrollment_id, 92.00, 4.00, @teacher_id, '演示成绩')
AS new
ON DUPLICATE KEY UPDATE `score` = new.score, `grade_point` = new.grade_point, `recorded_by` = new.recorded_by;

INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `publisher_id`)
SELECT 'ACADEMIC', '虚拟校园系统演示公告', '本公告用于演示公告查询、发布状态和角色可见范围。', 'ALL', 'PUBLISHED', '2026-08-01 09:00:00', @academic_id
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `announcements` WHERE `module_code` = 'ACADEMIC' AND `title` = '虚拟校园系统演示公告'
);

INSERT INTO `competitions` (`title`, `description`, `organizer_id`, `start_at`, `end_at`, `registration_deadline`, `capacity`, `status`)
SELECT '校园创新实践演示赛', '用于演示比赛发布、报名、取消与名单查看。', @academic_id,
       '2026-10-10 09:00:00', '2026-10-10 17:00:00', '2026-10-01 23:59:59', 100, 'PUBLISHED'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `competitions` WHERE `title` = '校园创新实践演示赛' AND `organizer_id` = @academic_id
);
SET @competition_id = (SELECT `id` FROM `competitions` WHERE `title` = '校园创新实践演示赛' AND `organizer_id` = @academic_id ORDER BY `id` LIMIT 1);

INSERT INTO `competition_registrations` (`competition_id`, `student_user_id`, `status`)
VALUES (@competition_id, @student_id, 'REGISTERED')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status, `cancelled_at` = NULL;

INSERT INTO `srtp_records` (`project_code`, `student_user_id`, `title`, `description`, `credits`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`)
VALUES ('DEMO-SRTP-001', @student_id, '虚拟校园服务体验优化', '用于演示SRTP提交、审核和学生查询。', 2.00, 'APPROVED', @academic_id, '2026-08-20 10:00:00', '演示记录：审核通过。')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status, `reviewed_by` = new.reviewed_by, `reviewed_at` = new.reviewed_at;

INSERT INTO `classroom_reservations` (`classroom_id`, `applicant_id`, `purpose`, `start_at`, `end_at`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`)
SELECT @classroom_id, @student_id, '项目组阶段汇报演示', '2026-09-15 18:00:00', '2026-09-15 20:00:00', 'APPROVED', @academic_id, '2026-09-10 10:00:00', '演示申请已批准。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `classroom_reservations`
    WHERE `classroom_id` = @classroom_id AND `applicant_id` = @student_id
      AND `start_at` = '2026-09-15 18:00:00'
);

-- ============================================================================
-- Library demo rows
-- ============================================================================

INSERT INTO `books` (`isbn`, `title`, `author`, `publisher`, `category`, `total_copies`, `available_copies`, `location`, `status`, `created_by`)
VALUES ('DEMO-978000000001', '软件工程实践导论', 'VCampus编写组', '演示出版社', '软件工程', 3, 2, 'A区-01-01', 'ON_SHELF', @librarian_id)
AS new
ON DUPLICATE KEY UPDATE `available_copies` = new.available_copies, `status` = new.status;
SET @book_id = (SELECT `id` FROM `books` WHERE `isbn` = 'DEMO-978000000001');

INSERT INTO `borrow_records` (`book_id`, `borrower_user_id`, `issued_at`, `due_at`, `status`, `handled_by`, `remark`)
SELECT @book_id, @student_id, '2026-08-20 09:00:00', '2026-09-20 23:59:59', 'BORROWED', @librarian_id, '演示借阅记录。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `borrow_records` WHERE `book_id` = @book_id AND `borrower_user_id` = @student_id
      AND `issued_at` = '2026-08-20 09:00:00'
);

INSERT INTO `study_rooms` (`building_name`, `room_no`, `capacity`, `open_time`, `close_time`, `status`, `description`)
VALUES ('图书馆', '研习室A', 8, '08:00:00', '22:00:00', 'OPEN', '用于演示自习室预约和冲突检查。')
AS new
ON DUPLICATE KEY UPDATE `capacity` = new.capacity, `status` = new.status;
SET @study_room_id = (SELECT `id` FROM `study_rooms` WHERE `building_name` = '图书馆' AND `room_no` = '研习室A');

INSERT INTO `study_room_reservations` (`room_id`, `user_id`, `start_at`, `end_at`, `status`)
SELECT @study_room_id, @student_id, '2026-09-05 14:00:00', '2026-09-05 16:00:00', 'RESERVED'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `study_room_reservations` WHERE `room_id` = @study_room_id
      AND `user_id` = @student_id AND `start_at` = '2026-09-05 14:00:00'
);

INSERT INTO `online_resources` (`title`, `resource_type`, `url`, `description`, `publisher_id`, `status`, `published_at`)
SELECT 'MySQL 8参考文档', 'DOCUMENTATION', 'https://dev.mysql.com/doc/', '用于演示线上资源启停和访问记录。', @librarian_id, 'ACTIVE', '2026-08-01 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `online_resources` WHERE `url` = 'https://dev.mysql.com/doc/');
SET @resource_id = (SELECT `id` FROM `online_resources` WHERE `url` = 'https://dev.mysql.com/doc/' ORDER BY `id` LIMIT 1);

INSERT INTO `online_resource_access_logs` (`resource_id`, `user_id`, `accessed_at`, `client_ip`)
SELECT @resource_id, @student_id, '2026-08-29 10:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `online_resource_access_logs` WHERE `resource_id` = @resource_id
      AND `user_id` = @student_id AND `accessed_at` = '2026-08-29 10:00:00'
);

-- ============================================================================
-- Store demo rows
-- ============================================================================

INSERT INTO `accounts` (`user_id`, `balance`, `status`)
VALUES (@student_id, 87.50, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status;
SET @account_id = (SELECT `id` FROM `accounts` WHERE `user_id` = @student_id);

INSERT INTO `products` (`sku`, `name`, `category`, `description`, `price`, `stock_qty`, `status`, `created_by`)
VALUES ('DEMO-CUP-001', 'VCampus纪念马克杯', '文创', '用于演示商品、购物车、库存与订单。', 12.50, 19, 'ON_SALE', @store_manager_id)
AS new
ON DUPLICATE KEY UPDATE `price` = new.price, `stock_qty` = new.stock_qty, `status` = new.status;
SET @product_id = (SELECT `id` FROM `products` WHERE `sku` = 'DEMO-CUP-001');

INSERT INTO `shopping_carts` (`user_id`, `status`)
VALUES (@student_id, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status;
SET @cart_id = (SELECT `id` FROM `shopping_carts` WHERE `user_id` = @student_id);

INSERT INTO `cart_items` (`cart_id`, `product_id`, `quantity`)
VALUES (@cart_id, @product_id, 1)
AS new
ON DUPLICATE KEY UPDATE `quantity` = new.quantity;

INSERT INTO `store_orders` (`order_no`, `buyer_id`, `total_amount`, `status`, `paid_at`)
VALUES ('DEMO-ORDER-0001', @student_id, 12.50, 'PAID', '2026-08-29 10:05:00')
AS new
ON DUPLICATE KEY UPDATE `total_amount` = new.total_amount, `status` = new.status, `paid_at` = new.paid_at;
SET @order_id = (SELECT `id` FROM `store_orders` WHERE `order_no` = 'DEMO-ORDER-0001');

INSERT INTO `store_order_items` (`order_id`, `product_id`, `product_name_snapshot`, `unit_price_snapshot`, `quantity`, `line_amount`)
VALUES (@order_id, @product_id, 'VCampus纪念马克杯', 12.50, 1, 12.50)
AS new
ON DUPLICATE KEY UPDATE `unit_price_snapshot` = new.unit_price_snapshot, `quantity` = new.quantity, `line_amount` = new.line_amount;

INSERT INTO `account_transactions` (`account_id`, `transaction_type`, `amount`, `balance_before`, `balance_after`, `reference_type`, `reference_id`, `idempotency_key`, `operator_id`, `remark`)
VALUES (@account_id, 'RECHARGE', 100.00, 0.00, 100.00, 'DEMO_SEED', @student_id, 'DEMO-ACCOUNT-RECHARGE-0001', @system_admin_id, '演示账户初始化余额')
AS new
ON DUPLICATE KEY UPDATE `remark` = new.remark;
SET @recharge_tx_id = (SELECT `id` FROM `account_transactions` WHERE `idempotency_key` = 'DEMO-ACCOUNT-RECHARGE-0001');

INSERT INTO `account_transactions` (`account_id`, `transaction_type`, `amount`, `balance_before`, `balance_after`, `reference_type`, `reference_id`, `idempotency_key`, `operator_id`, `remark`)
VALUES (@account_id, 'PURCHASE', -12.50, 100.00, 87.50, 'STORE_ORDER', @order_id, 'DEMO-STORE-PAYMENT-0001', @student_id, '演示订单支付')
AS new
ON DUPLICATE KEY UPDATE `remark` = new.remark;

-- ============================================================================
-- Dormitory demo rows
-- ============================================================================

INSERT INTO `dorm_buildings` (`building_code`, `building_name`, `address`, `gender_policy`, `status`)
VALUES ('DEMO-D1', '九龙湖学生公寓D1', '九龙湖校区', 'MIXED', 'OPEN')
AS new
ON DUPLICATE KEY UPDATE `building_name` = new.building_name, `status` = new.status;
SET @building_id = (SELECT `id` FROM `dorm_buildings` WHERE `building_code` = 'DEMO-D1');

INSERT INTO `dorm_rooms` (`building_id`, `room_no`, `floor_no`, `capacity`, `room_type`, `status`, `description`)
VALUES (@building_id, '101', 1, 4, 'STANDARD', 'AVAILABLE', '用于演示住宿、报修和水电分摊。')
AS new
ON DUPLICATE KEY UPDATE `capacity` = new.capacity, `status` = new.status;
SET @room_id = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @building_id AND `room_no` = '101');

INSERT INTO `dorm_beds` (`room_id`, `bed_no`, `status`)
VALUES (@room_id, 'A', 'OCCUPIED'), (@room_id, 'B', 'AVAILABLE'), (@room_id, 'C', 'AVAILABLE'), (@room_id, 'D', 'AVAILABLE')
AS new
ON DUPLICATE KEY UPDATE `status` = new.status;
SET @bed_id = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @room_id AND `bed_no` = 'A');

INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @student_id, @bed_id, '2026-09-01', 'ACTIVE', @dorm_manager_id
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @student_id AND `status` = 'ACTIVE'
);
SET @accommodation_id = (SELECT `id` FROM `accommodation_records` WHERE `student_user_id` = @student_id AND `status` = 'ACTIVE' ORDER BY `id` LIMIT 1);

INSERT INTO `leave_requests` (`student_user_id`, `leave_type`, `start_at`, `end_at`, `reason`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`)
SELECT @student_id, 'PERSONAL', '2026-09-20 18:00:00', '2026-09-21 22:00:00', '周末返乡演示申请', 'APPROVED', @dorm_manager_id, '2026-09-18 09:00:00', '演示审批通过。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `leave_requests` WHERE `student_user_id` = @student_id AND `start_at` = '2026-09-20 18:00:00'
);

INSERT INTO `access_records` (`student_user_id`, `record_type`, `occurred_at`, `door_name`, `source`, `note`)
SELECT @student_id, 'ENTRY', '2026-08-29 08:00:00', 'D1-101门禁', 'MANUAL', '演示进出记录。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `access_records` WHERE `student_user_id` = @student_id AND `occurred_at` = '2026-08-29 08:00:00'
);

INSERT INTO `late_return_alerts` (`student_user_id`, `alert_date`, `detected_at`, `status`, `handled_by`, `handled_at`, `note`)
SELECT @student_id, '2026-08-28', '2026-08-29 00:30:00', 'CLEARED', @dorm_manager_id, '2026-08-29 08:30:00', '演示预警已核实。'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `late_return_alerts` WHERE `student_user_id` = @student_id AND `alert_date` = '2026-08-28');

INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `status`)
SELECT @room_id, @dorm_manager_id, '2026-08-28 10:00:00', 94.00, 'PASS', 'NORMAL'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `hygiene_inspections` WHERE `room_id` = @room_id AND `inspected_at` = '2026-08-28 10:00:00');

INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`)
SELECT @room_id, @student_id, 'LIGHTING', '卫生间灯具偶尔闪烁，申请检查。', 'NORMAL', 'SUBMITTED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `room_id` = @room_id AND `reporter_id` = @student_id AND `description` = '卫生间灯具偶尔闪烁，申请检查。');

INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `publisher_id`)
SELECT 'DORM', '宿舍水电缴费演示公告', '请在页面内查看本月房间水电账单和个人分摊。', 'ALL', 'PUBLISHED', '2026-08-01 09:00:00', @dorm_manager_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `announcements` WHERE `module_code` = 'DORM' AND `title` = '宿舍水电缴费演示公告');

INSERT INTO `utility_bills` (`room_id`, `period_start`, `period_end`, `electricity_units`, `water_units`, `total_amount`, `due_at`, `status`, `created_by`)
VALUES (@room_id, '2026-08-01', '2026-08-31', 120.500, 18.200, 60.00, '2026-09-15 23:59:59', 'PARTIAL', @dorm_manager_id)
AS new
ON DUPLICATE KEY UPDATE `total_amount` = new.total_amount, `status` = new.status;
SET @bill_id = (SELECT `id` FROM `utility_bills` WHERE `room_id` = @room_id AND `period_start` = '2026-08-01' AND `period_end` = '2026-08-31');

INSERT INTO `utility_allocations` (`bill_id`, `student_user_id`, `amount`, `status`)
VALUES (@bill_id, @student_id, 60.00, 'UNPAID')
AS new
ON DUPLICATE KEY UPDATE `amount` = new.amount, `status` = new.status;

-- ============================================================================
-- AI storage boundary demo rows: no real model or write tool is invoked.
-- ============================================================================

INSERT INTO `ai_chat_sessions` (`user_id`, `title`, `status`, `model_name`)
SELECT @student_id, '演示校园助手会话', 'ACTIVE', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_chat_sessions` WHERE `user_id` = @student_id AND `title` = '演示校园助手会话');
SET @ai_session_id = (SELECT `id` FROM `ai_chat_sessions` WHERE `user_id` = @student_id AND `title` = '演示校园助手会话' ORDER BY `id` LIMIT 1);

INSERT INTO `ai_chat_messages` (`session_id`, `request_id`, `sequence_no`, `sender_type`, `content`, `status`)
VALUES
    (@ai_session_id, 'demo-ai-request-0001', 1, 'USER', '我如何查看本学期课程？', 'COMPLETED'),
    (@ai_session_id, 'demo-ai-request-0001', 2, 'ASSISTANT', '已通过教务服务查询本人课表；AI 助手会沿用当前登录会话和业务权限返回结果。', 'COMPLETED')
AS new
ON DUPLICATE KEY UPDATE `content` = new.content, `status` = new.status;

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '课程查询说明', '学生可以在虚拟教务模块查看已选课程、上课时间和教室。', 'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '课程查询说明');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '学生选课并加入课表操作步骤',
       '适用角色：学生。步骤：1. 登录后进入“教务管理”。2. 打开“选课中心”，在“课程与课表”区域按课程编号或名称搜索。3. 只选择状态为“已发布”的课程，并查看课程名称、学分、容量和已有上课时段。4. 选中目标课程后点击“选课”。5. 系统会校验课程状态、剩余容量、重复选课和课表时间冲突；校验失败时按页面提示更换课程或联系教务老师。6. 页面出现“选课成功”后，切换到“我的课表”查看课程、上课时间和教室。学生不能在课表中手工创建课程或时段；课表内容来自已成功选修的课程。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '学生选课并加入课表操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '教务老师添加课程上课时段操作步骤',
       '适用角色：教务老师。步骤：1. 登录后进入“教务管理”，打开“课程与排课”。2. 在课程列表中选择已有课程；没有课程时先点击“新建课程”，填写课程资料并保存。3. 在下方“排课维护”区域点击“新建时段”。4. 依次填写星期、开始节次、结束节次、可选的起止日期和教室编号。5. 点击“保存时段”。6. 系统会校验节次范围、日期范围、教师课表冲突和教室占用冲突；失败时根据提示调整。7. 页面显示“课程时段已保存”且时段出现在列表中即完成。修改时先选中已有时段再保存；删除时必须再次确认。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '教务老师添加课程上课时段操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '图书借阅与归还操作步骤',
       '适用角色：具有图书借阅权限的用户。借书步骤：1. 进入“虚拟图书馆”的图书列表。2. 按书名、作者或关键词搜索。3. 选择有可借库存的图书并提交借阅。4. 确认借阅操作后等待系统校验库存和重复借阅。5. 成功后在“我的借阅”查看记录。归还步骤：1. 打开“我的借阅”。2. 选中状态仍为借阅中的记录。3. 点击归还并确认。4. 页面显示归还成功且记录状态更新即完成。AI 代办借书或归还同样必须由当前用户确认，并继续使用图书馆模块的库存和权限校验。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '图书借阅与归还操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '校园商店购物操作步骤',
       '适用角色：具有商店购买权限的用户。步骤：1. 进入“校园商店”并搜索商品。2. 查看商品状态、单价和库存。3. 将商品加入购物车并调整数量。4. 检查购物车后创建订单。5. 在订单页面确认金额并支付。6. 系统在支付时重新校验商品状态、库存、账户余额和订单状态，成功后生成账户流水。AI 可以查询商品、购物车、本人订单和余额；加入购物车或创建订单属于写操作，必须确认。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '校园商店购物操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '宿舍报修与水电查询操作步骤',
       '住宿信息与水电查询：进入“宿舍管理”，学生可以查看本人当前住宿信息和水电分摊，不能查看其他学生的数据。报修步骤：1. 打开本人报修页面。2. 新建报修并填写地点、问题类型和描述。3. 提交后在报修列表查看处理状态。4. 维修完成后可按页面提供的入口评价。水电缴费会修改账户和账单状态，执行前必须确认，并由宿舍模块再次校验账单是否可支付、是否重复支付。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '宿舍报修与水电查询操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_GUIDE', NULL, '账号、学籍与成绩查询操作步骤',
       '查看账号资料：登录后进入个人中心或身份信息页面，系统只返回当前登录人的公开资料，不返回密码、密码哈希或会话令牌。查看学籍：学生进入“学籍信息”查看本人学号、院系、专业和状态。查看成绩：学生在学籍模块打开“我的成绩”，可按课程分页查看；教师或管理员的成绩登记权限不能与学生本人查询权限混用。AI 查询这些信息时使用当前会话身份，不接受用户在问题中伪造的用户编号。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_GUIDE' AND `title` = '账号、学籍与成绩查询操作步骤');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_RULE', NULL, '选课业务规则',
       '课程必须处于已发布且可选状态。系统拒绝重复选修同一课程、超过课程容量的选课以及与已选课程上课时段冲突的选课。退课会修改选课记录，必须由当前学生确认；已完成或不允许退选的记录不能强制修改。最终结果以教务模块返回的信息为准。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_RULE' AND `title` = '选课业务规则');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'KNOWLEDGE_SCOPE', NULL, '校纪校规知识回答范围',
       'AI 助手只能依据知识库中由知识管理员录入、标注来源并启用的校纪校规条款回答正式规定。若检索结果没有对应条款，助手必须明确说明“当前知识库未收录该规定”，建议咨询学校主管部门或由知识管理员补充正式文件，不得依据常识编造处分标准、申请期限或管理办法。管理员录入时应在标题中写明制度名称和条款，在正文中保留适用对象、具体要求、生效范围和官方来源。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'KNOWLEDGE_SCOPE' AND `title` = '校纪校规知识回答范围');

INSERT INTO `ai_knowledge_chunks` (`source_type`, `source_ref_id`, `title`, `content`, `status`, `updated_by`)
SELECT 'SYSTEM_RULE', NULL, 'AI业务代办安全规则',
       'AI 只通过系统已有命令路由调用业务模块，不直接修改课程、图书、商品、宿舍、账号或学籍数据。查询按当前登录角色鉴权。选课、退课、借书、归还、报名、取消报名、购物车修改和创建订单等写操作先生成待确认卡片，只有原用户在有效期内确认后才执行；确认时原业务模块仍会再次校验权限、状态、容量、库存、余额和并发冲突。',
       'ACTIVE', @ai_admin_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `source_type` = 'SYSTEM_RULE' AND `title` = 'AI业务代办安全规则');

INSERT INTO `ai_tool_call_logs` (`session_id`, `request_id`, `tool_name`, `action_type`, `arguments_json`, `result_summary`, `status`, `requested_by`, `completed_at`)
SELECT @ai_session_id, 'demo-ai-request-0001', 'academic.schedule.read', 'READ', JSON_OBJECT(), '已通过教务服务接口查询本人课表。', 'SUCCEEDED', @student_id, '2026-08-29 10:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_tool_call_logs` WHERE `request_id` = 'demo-ai-request-0001' AND `tool_name` = 'academic.schedule.read');

COMMIT;
