-- Rich, deterministic data for UI, pagination, role and multi-user testing.
-- Every row is namespaced with TEST-/test_ and this file is safe to rerun.
USE `vcampus`;
START TRANSACTION;

CREATE TEMPORARY TABLE IF NOT EXISTS `vcampus_seed_numbers` (`n` INT NOT NULL PRIMARY KEY);
DELETE FROM `vcampus_seed_numbers`;
INSERT INTO `vcampus_seed_numbers` (`n`) VALUES
 (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
 (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
 (21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
 (31),(32),(33),(34),(35),(36),(37),(38),(39),(40);

SET @seed_password = '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi';
SET @seed_system = (SELECT `id` FROM `users` WHERE `username` = 'demo_system');
SET @seed_academic = (SELECT `id` FROM `users` WHERE `username` = 'demo_academic');
SET @seed_librarian = (SELECT `id` FROM `users` WHERE `username` = 'demo_librarian');
SET @seed_store = (SELECT `id` FROM `users` WHERE `username` = 'demo_store');
SET @seed_dorm = (SELECT `id` FROM `users` WHERE `username` = 'demo_dorm');
SET @seed_repair = (SELECT `id` FROM `users` WHERE `username` = 'demo_repair');
SET @seed_student = (SELECT `id` FROM `users` WHERE `username` = 'demo_student');

-- Accounts: test_student01..20 and test_teacher01..06 all use password student123.
INSERT IGNORE INTO `users` (`username`,`password_hash`,`display_name`,`email`,`phone`,`status`)
SELECT CONCAT('test_student', LPAD(n,2,'0')), @seed_password,
       CONCAT('测试学生', LPAD(n,2,'0')), CONCAT('test.student',LPAD(n,2,'0'),'@vcampus.local'),
       CONCAT('1390000',LPAD(n,4,'0')), 'ACTIVE'
FROM `vcampus_seed_numbers` WHERE n <= 20;

INSERT IGNORE INTO `users` (`username`,`password_hash`,`display_name`,`email`,`phone`,`status`)
SELECT CONCAT('test_teacher', LPAD(n,2,'0')), @seed_password,
       CONCAT('测试教师', LPAD(n,2,'0')), CONCAT('test.teacher',LPAD(n,2,'0'),'@vcampus.local'),
       CONCAT('1370000',LPAD(n,4,'0')), 'ACTIVE'
FROM `vcampus_seed_numbers` WHERE n <= 6;

INSERT IGNORE INTO `user_roles` (`user_id`,`role_id`,`assigned_by`)
SELECT u.id, r.id, @seed_system FROM `users` u JOIN `roles` r ON r.code='STUDENT'
WHERE u.username LIKE 'test_student%';
INSERT IGNORE INTO `user_roles` (`user_id`,`role_id`,`assigned_by`)
SELECT u.id, r.id, @seed_system FROM `users` u JOIN `roles` r ON r.code='TEACHER'
WHERE u.username LIKE 'test_teacher%';

INSERT IGNORE INTO `student_profiles`
 (`user_id`,`student_no`,`college`,`major`,`class_name`,`enrollment_year`,
  `expected_graduation_year`,`degree_level`,`gender`,`birth_date`,`address`,
  `emergency_contact`,`emergency_phone`,`status`)
SELECT u.id, CONCAT('TEST2026',RIGHT(u.username,2)),
       CASE MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)
         WHEN 0 THEN '计算机科学与工程学院' WHEN 1 THEN '电子科学与工程学院' ELSE '经济管理学院' END,
       CASE MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)
         WHEN 0 THEN '软件工程' WHEN 1 THEN '信息工程' ELSE '工商管理' END,
       CONCAT('测试班',1+MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),4)), 2026, 2030,
       '本科', IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),2)=0,'女','男'),
       DATE_ADD('2007-01-01', INTERVAL CAST(RIGHT(u.username,2) AS UNSIGNED) DAY),
       '南京市江宁区测试地址', CONCAT('家长',RIGHT(u.username,2)),
       CONCAT('1360000',RIGHT(u.username,2),'00'), 'ENROLLED'
FROM `users` u WHERE u.username LIKE 'test_student%';

INSERT IGNORE INTO `teacher_profiles` (`user_id`,`employee_no`,`department`,`title`,`status`)
SELECT u.id, CONCAT('T2026',RIGHT(u.username,2)),
       IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),2)=0,'计算机科学与工程学院','电子科学与工程学院'),
       CASE MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)
         WHEN 0 THEN '教授' WHEN 1 THEN '副教授' ELSE '讲师' END, 'ACTIVE'
FROM `users` u WHERE u.username LIKE 'test_teacher%';

INSERT IGNORE INTO `accounts` (`user_id`,`balance`,`status`)
SELECT id, 500.00, 'ACTIVE' FROM `users` WHERE username LIKE 'test_student%';
INSERT IGNORE INTO `shopping_carts` (`user_id`,`status`)
SELECT id, 'ACTIVE' FROM `users` WHERE username LIKE 'test_student%';

-- Academic: enough rows for paging, multiple teachers, schedules and mixed enrollment states.
INSERT IGNORE INTO `classrooms`
 (`building_name`,`room_no`,`classroom_type`,`capacity`,`equipment_description`,`status`)
SELECT '测试教学楼', CONCAT(100+n),
       CASE MOD(n,4) WHEN 0 THEN 'LAB' WHEN 1 THEN 'TEACHING' WHEN 2 THEN 'MEETING' ELSE 'OTHER' END,
       30+MOD(n,4)*20, CONCAT('投影、空调、测试设备组',n),
       IF(MOD(n,9)=0,'MAINTENANCE','AVAILABLE')
FROM `vcampus_seed_numbers` WHERE n <= 18;

INSERT IGNORE INTO `courses`
 (`course_code`,`course_name`,`course_type`,`semester_code`,`credits`,`total_hours`,
  `capacity`,`description`,`status`,`created_by`)
SELECT CONCAT('TEST-C-',LPAD(n,3,'0')), CONCAT('测试课程',LPAD(n,2,'0')),
       CASE MOD(n,4) WHEN 0 THEN 'PRACTICE' WHEN 1 THEN 'REQUIRED'
         WHEN 2 THEN 'ELECTIVE' ELSE 'PUBLIC' END,
       CASE WHEN n<=8 THEN '2024-2025-2' WHEN n<=16 THEN '2025-2026-1' ELSE '2025-2026-2' END,
       1+MOD(n,4), 16+MOD(n,4)*16, 25+MOD(n,5)*10,
       CONCAT('用于测试检索、选课、成绩和排课的课程 ',n),
       IF(n<=8,'CLOSED','PUBLISHED'), @seed_academic
FROM `vcampus_seed_numbers` WHERE n <= 24;

INSERT IGNORE INTO `course_instructors` (`course_id`,`teacher_user_id`,`instructor_role`)
SELECT c.id, u.id, 'PRIMARY' FROM `courses` c JOIN `users` u
  ON u.username=CONCAT('test_teacher',LPAD(1+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)-1,6),2,'0'))
WHERE c.course_code LIKE 'TEST-C-%';
INSERT IGNORE INTO `course_instructors` (`course_id`,`teacher_user_id`,`instructor_role`)
SELECT c.id, u.id, 'ASSISTANT' FROM `courses` c JOIN `users` u
  ON u.username=CONCAT('test_teacher',LPAD(1+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)+1,6),2,'0'))
WHERE c.course_code LIKE 'TEST-C-%' AND MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED),4)=0;

INSERT INTO `course_schedules`
 (`course_id`,`weekday`,`start_period`,`end_period`,`start_date`,`end_date`,`classroom_id`)
SELECT c.id, 1+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)-1,5),
       1+2*MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)-1,5),
       2+2*MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)-1,5),
       '2026-02-23','2026-06-28',cr.id
FROM `courses` c JOIN `classrooms` cr
  ON cr.building_name='测试教学楼'
 AND cr.room_no=CONCAT(100+1+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)-1,18))
WHERE c.course_code LIKE 'TEST-C-%'
  AND NOT EXISTS (SELECT 1 FROM `course_schedules` s
      WHERE s.course_id=c.id AND s.start_date='2026-02-23');

INSERT IGNORE INTO `enrollments` (`student_user_id`,`course_id`,`status`,`dropped_at`)
SELECT u.id,c.id,
       CASE WHEN CAST(RIGHT(c.course_code,3) AS UNSIGNED)<=8 THEN 'COMPLETED'
            WHEN MOD(CAST(RIGHT(u.username,2) AS UNSIGNED)+CAST(RIGHT(c.course_code,3) AS UNSIGNED),7)=0
              THEN 'DROPPED' ELSE 'ENROLLED' END,
       CASE WHEN CAST(RIGHT(c.course_code,3) AS UNSIGNED)>8
              AND MOD(CAST(RIGHT(u.username,2) AS UNSIGNED)+CAST(RIGHT(c.course_code,3) AS UNSIGNED),7)=0
            THEN '2026-02-20 10:00:00' ELSE NULL END
FROM `users` u CROSS JOIN `courses` c
WHERE u.username LIKE 'test_student%' AND c.course_code LIKE 'TEST-C-%'
  AND MOD(CAST(RIGHT(u.username,2) AS UNSIGNED)+CAST(RIGHT(c.course_code,3) AS UNSIGNED),3)=0;

INSERT IGNORE INTO `course_grades`
 (`enrollment_id`,`score`,`grade_point`,`gpa_included`,`recorded_by`,`remark`)
SELECT e.id, 60+MOD(e.id*7,41),
       CASE WHEN 60+MOD(e.id*7,41)>=96 THEN 4.8 WHEN 60+MOD(e.id*7,41)>=93 THEN 4.5
            WHEN 60+MOD(e.id*7,41)>=90 THEN 4.0 WHEN 60+MOD(e.id*7,41)>=86 THEN 3.8
            WHEN 60+MOD(e.id*7,41)>=83 THEN 3.5 WHEN 60+MOD(e.id*7,41)>=80 THEN 3.0
            WHEN 60+MOD(e.id*7,41)>=76 THEN 2.8 WHEN 60+MOD(e.id*7,41)>=73 THEN 2.5
            WHEN 60+MOD(e.id*7,41)>=70 THEN 2.0 WHEN 60+MOD(e.id*7,41)>=66 THEN 1.5
            WHEN 60+MOD(e.id*7,41)>=60 THEN 1.0 ELSE 0 END,
       1, @seed_academic, '扩展测试成绩'
FROM `enrollments` e JOIN `users` u ON u.id=e.student_user_id
WHERE u.username LIKE 'test_student%' AND e.status='COMPLETED';

-- Keep the primary demo login useful: multiple terms of grades plus a current timetable.
INSERT IGNORE INTO `enrollments` (`student_user_id`,`course_id`,`status`)
SELECT @seed_student,c.id,
       IF(CAST(RIGHT(c.course_code,3) AS UNSIGNED)<=8,'COMPLETED','ENROLLED')
FROM `courses` c
WHERE c.course_code LIKE 'TEST-C-%'
  AND CAST(RIGHT(c.course_code,3) AS UNSIGNED)<=16;

INSERT IGNORE INTO `course_grades`
 (`enrollment_id`,`score`,`grade_point`,`gpa_included`,`recorded_by`,`remark`)
SELECT e.id,68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31),
       CASE WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=96 THEN 4.8
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=93 THEN 4.5
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=90 THEN 4.0
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=86 THEN 3.8
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=83 THEN 3.5
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=80 THEN 3.0
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=76 THEN 2.8
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=73 THEN 2.5
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=70 THEN 2.0
            WHEN 68+MOD(CAST(RIGHT(c.course_code,3) AS UNSIGNED)*4,31)>=66 THEN 1.5 ELSE 1.0 END,
       1,@seed_academic,'demo_student 多学期测试成绩'
FROM `enrollments` e JOIN `courses` c ON c.id=e.course_id
WHERE e.student_user_id=@seed_student AND e.status='COMPLETED'
  AND c.course_code LIKE 'TEST-C-%';

INSERT INTO `announcements`
 (`module_code`,`title`,`content`,`visible_scope`,`status`,`publish_at`,`publisher_id`)
SELECT CASE MOD(n,4) WHEN 0 THEN 'SYSTEM' WHEN 1 THEN 'ACADEMIC'
         WHEN 2 THEN 'LIBRARY' ELSE 'DORM' END,
       CONCAT('扩展测试公告 ',LPAD(n,2,'0')),
       CONCAT('这是第 ',n,' 条完整公告正文，用于测试长文本、分页、弹窗和不同模块展示。'),
       'ALL','PUBLISHED',DATE_SUB(NOW(3),INTERVAL n DAY),
       CASE MOD(n,4) WHEN 1 THEN @seed_academic WHEN 2 THEN @seed_librarian
         WHEN 3 THEN @seed_dorm ELSE @seed_system END
FROM `vcampus_seed_numbers` x WHERE n<=24
  AND NOT EXISTS (SELECT 1 FROM `announcements` a
      WHERE a.title=CONCAT('扩展测试公告 ',LPAD(x.n,2,'0')));

INSERT INTO `competitions`
 (`title`,`description`,`organizer_id`,`start_at`,`end_at`,`registration_deadline`,`capacity`,`status`)
SELECT CONCAT('扩展测试竞赛 ',LPAD(n,2,'0')), CONCAT('竞赛完整介绍与报名要求 ',n),
       @seed_academic, DATE_ADD('2026-10-01 09:00:00',INTERVAL n DAY),
       DATE_ADD('2026-10-02 17:00:00',INTERVAL n DAY),
       DATE_ADD('2026-09-20 23:59:00',INTERVAL n HOUR), 30+n,
       IF(MOD(n,5)=0,'CLOSED','PUBLISHED')
FROM `vcampus_seed_numbers` x WHERE n<=12
  AND NOT EXISTS (SELECT 1 FROM `competitions` c
      WHERE c.title=CONCAT('扩展测试竞赛 ',LPAD(x.n,2,'0')));

INSERT IGNORE INTO `competition_registrations` (`competition_id`,`student_user_id`,`status`,`cancelled_at`)
SELECT c.id,u.id,IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),6)=0,'CANCELLED','REGISTERED'),
       IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),6)=0,NOW(3),NULL)
FROM `competitions` c JOIN `users` u ON u.username LIKE 'test_student%'
WHERE c.title LIKE '扩展测试竞赛 %'
  AND MOD(c.id+CAST(RIGHT(u.username,2) AS UNSIGNED),5)=0;

INSERT IGNORE INTO `srtp_records`
 (`project_code`,`student_user_id`,`title`,`description`,`credits`,`status`,
  `reviewed_by`,`reviewed_at`,`review_remark`)
SELECT CONCAT('TEST-SRTP-',LPAD(CAST(RIGHT(u.username,2) AS UNSIGNED),3,'0')),u.id,
       CONCAT('学生创新项目 ',RIGHT(u.username,2)),'用于测试提交、修改和审核同步。',2.0,
       CASE MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)
         WHEN 0 THEN 'APPROVED' WHEN 1 THEN 'SUBMITTED' ELSE 'REJECTED' END,
       IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)=1,NULL,@seed_academic),
       IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)=1,NULL,NOW(3)),
       IF(MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)=1,NULL,'扩展测试审核意见')
FROM `users` u WHERE u.username LIKE 'test_student%';

-- Library: catalog, borrowing history, rooms, reservations and resource access logs.
INSERT IGNORE INTO `books`
 (`isbn`,`title`,`author`,`publisher`,`category`,`total_copies`,`available_copies`,
  `location`,`description`,`status`,`created_by`,`publication_year`)
SELECT CONCAT('978-7-TEST-',LPAD(n,4,'0')), CONCAT('扩展测试图书 ',LPAD(n,2,'0')),
       CONCAT('作者',1+MOD(n,8)),'东南大学出版社',
       CASE MOD(n,5) WHEN 0 THEN '文学' WHEN 1 THEN '计算机' WHEN 2 THEN '历史'
         WHEN 3 THEN '艺术' ELSE '工程' END,
       5,4,CONCAT('测试书架-',1+MOD(n,6)),CONCAT('用于测试图书检索与借阅的简介 ',n),
       IF(MOD(n,13)=0,'UNAVAILABLE','ON_SHELF'),@seed_librarian,2000+MOD(n,25)
FROM `vcampus_seed_numbers` WHERE n<=40;

INSERT INTO `borrow_records`
 (`book_id`,`borrower_user_id`,`issued_at`,`due_at`,`returned_at`,`status`,`renew_count`,`handled_by`,`remark`)
SELECT b.id,u.id,DATE_SUB(NOW(3),INTERVAL n DAY),DATE_ADD(NOW(3),INTERVAL (20-n) DAY),
       IF(MOD(n,4)=0,DATE_SUB(NOW(3),INTERVAL 1 DAY),NULL),
       CASE MOD(n,4) WHEN 0 THEN 'RETURNED' WHEN 1 THEN 'BORROWED'
         WHEN 2 THEN 'OVERDUE' ELSE 'BORROWED' END, MOD(n,2),@seed_librarian,
       CONCAT('EXPANDED-SEED-BORROW-',LPAD(n,2,'0'))
FROM `vcampus_seed_numbers` x JOIN `books` b ON b.isbn=CONCAT('978-7-TEST-',LPAD(x.n,4,'0'))
JOIN `users` u ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0'))
WHERE n<=35 AND NOT EXISTS (SELECT 1 FROM `borrow_records` r
  WHERE r.remark=CONCAT('EXPANDED-SEED-BORROW-',LPAD(x.n,2,'0')));

INSERT INTO `borrow_records`
 (`book_id`,`borrower_user_id`,`issued_at`,`due_at`,`returned_at`,`status`,`renew_count`,`handled_by`,`remark`)
SELECT b.id,@seed_student,DATE_SUB(NOW(3),INTERVAL n*3 DAY),DATE_ADD(NOW(3),INTERVAL 30-n*3 DAY),
       IF(MOD(n,3)=0,DATE_SUB(NOW(3),INTERVAL n DAY),NULL),
       CASE MOD(n,3) WHEN 0 THEN 'RETURNED' WHEN 1 THEN 'BORROWED' ELSE 'OVERDUE' END,
       MOD(n,2),@seed_librarian,CONCAT('DEMO-STUDENT-BORROW-',LPAD(n,2,'0'))
FROM `vcampus_seed_numbers` x JOIN `books` b ON b.isbn=CONCAT('978-7-TEST-',LPAD(x.n,4,'0'))
WHERE n<=10 AND NOT EXISTS (SELECT 1 FROM `borrow_records` r
  WHERE r.remark=CONCAT('DEMO-STUDENT-BORROW-',LPAD(x.n,2,'0')));

INSERT IGNORE INTO `study_rooms`
 (`building_name`,`room_no`,`capacity`,`open_time`,`close_time`,`status`,`description`)
SELECT '测试图书馆',CONCAT('R-',LPAD(n,2,'0')),20+MOD(n,4)*10,'08:00:00','22:00:00',
       IF(MOD(n,8)=0,'MAINTENANCE','OPEN'),CONCAT('扩展测试自习室 ',n)
FROM `vcampus_seed_numbers` WHERE n<=12;

INSERT INTO `study_room_reservations`
 (`room_id`,`user_id`,`start_at`,`end_at`,`status`,`cancelled_at`)
SELECT r.id,u.id,DATE_ADD('2026-10-10 08:00:00',INTERVAL n HOUR),
       DATE_ADD('2026-10-10 09:00:00',INTERVAL n HOUR),
       CASE MOD(n,4) WHEN 0 THEN 'CANCELLED' WHEN 1 THEN 'RESERVED'
         WHEN 2 THEN 'COMPLETED' ELSE 'NO_SHOW' END,
       IF(MOD(n,4)=0,DATE_ADD('2026-10-10 07:00:00',INTERVAL n HOUR),NULL)
FROM `vcampus_seed_numbers` x JOIN `study_rooms` r ON r.room_no=CONCAT('R-',LPAD(1+MOD(x.n-1,12),2,'0'))
JOIN `users` u ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0'))
WHERE n<=30 AND NOT EXISTS (SELECT 1 FROM `study_room_reservations` s
  WHERE s.room_id=r.id AND s.user_id=u.id AND s.start_at=DATE_ADD('2026-10-10 08:00:00',INTERVAL x.n HOUR));

INSERT INTO `online_resources`
 (`title`,`resource_type`,`url`,`description`,`publisher_id`,`status`,`published_at`)
SELECT CONCAT('扩展线上资源 ',LPAD(n,2,'0')),
       CASE MOD(n,3) WHEN 0 THEN '数据库' WHEN 1 THEN '电子期刊' ELSE '学习平台' END,
       CONCAT('https://example.invalid/vcampus/resource/',n),CONCAT('资源详细说明 ',n),
       @seed_librarian,IF(MOD(n,7)=0,'INACTIVE','ACTIVE'),DATE_SUB(NOW(3),INTERVAL n DAY)
FROM `vcampus_seed_numbers` x WHERE n<=18 AND NOT EXISTS (SELECT 1 FROM `online_resources` r
  WHERE r.url=CONCAT('https://example.invalid/vcampus/resource/',x.n));

INSERT INTO `online_resource_access_logs` (`resource_id`,`user_id`,`accessed_at`,`client_ip`)
SELECT r.id,u.id,DATE_SUB(NOW(3),INTERVAL n HOUR),CONCAT('10.20.0.',n)
FROM `vcampus_seed_numbers` x JOIN `online_resources` r
  ON r.url=CONCAT('https://example.invalid/vcampus/resource/',1+MOD(x.n-1,18))
JOIN `users` u ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0'))
WHERE n<=40 AND NOT EXISTS (SELECT 1 FROM `online_resource_access_logs` l
  WHERE l.client_ip=CONCAT('10.20.0.',x.n));

-- Store: products, carts, mixed order states, sales history, reviews, coupons and promotions.
INSERT IGNORE INTO `products`
 (`sku`,`name`,`category`,`category_code`,`description`,`price`,`stock_qty`,`status`,
  `image_url`,`rating_average`,`rating_count`,`created_by`)
SELECT CONCAT('TEST-SKU-',LPAD(n,3,'0')),CONCAT('扩展测试商品 ',LPAD(n,2,'0')),
       CASE MOD(n,5) WHEN 0 THEN '文创' WHEN 1 THEN '食品' WHEN 2 THEN '日用'
         WHEN 3 THEN '文具' ELSE '其他' END,
       CASE MOD(n,5) WHEN 0 THEN 'CULTURE' WHEN 1 THEN 'FOOD' WHEN 2 THEN 'DAILY'
         WHEN 3 THEN 'STATIONERY' ELSE 'OTHER' END,
       CONCAT('商品完整介绍、规格和使用说明 ',n),5.00+n*1.25,10+MOD(n*7,80),
       IF(MOD(n,11)=0,'DRAFT','ON_SALE'),
       CONCAT('https://picsum.photos/seed/vcampus-product-',LPAD(n,3,'0'),'/640/480'),
       0,0,@seed_store
FROM `vcampus_seed_numbers` WHERE n<=40;

UPDATE `products`
SET `image_url`=CONCAT('https://picsum.photos/seed/vcampus-product-',RIGHT(`sku`,3),'/640/480')
WHERE `sku` LIKE 'TEST-SKU-%' AND (`image_url` IS NULL OR `image_url`='');

INSERT IGNORE INTO `store_coupons`
 (`code`,`name`,`threshold_amount`,`discount_amount`,`expires_at`,`active`)
SELECT CONCAT('TEST-COUPON-',LPAD(n,2,'0')),CONCAT('测试优惠券 ',n),n*10,n*2,
       DATE_ADD(NOW(3),INTERVAL 180+n DAY),IF(MOD(n,6)=0,0,1)
FROM `vcampus_seed_numbers` WHERE n<=12;

INSERT IGNORE INTO `store_promotions`
 (`code`,`name`,`promotion_type`,`threshold_amount`,`discount_value`,`product_scope`,
  `product_id`,`category_code`,`starts_at`,`ends_at`,`stackable`,`active`)
SELECT CONCAT('TEST-PROMO-',LPAD(n,2,'0')),CONCAT('测试促销 ',n),
       CASE MOD(n,3) WHEN 0 THEN 'PERCENT' WHEN 1 THEN 'THRESHOLD' ELSE 'FIXED' END,
       IF(MOD(n,3)=1,50.00,NULL),IF(MOD(n,3)=0,90.00,5.00),'ALL',NULL,NULL,
       DATE_SUB(NOW(3),INTERVAL 10 DAY),DATE_ADD(NOW(3),INTERVAL 120 DAY),MOD(n,2),1
FROM `vcampus_seed_numbers` WHERE n<=9;

INSERT IGNORE INTO `store_user_coupons` (`coupon_id`,`user_id`)
SELECT c.id,u.id FROM `store_coupons` c CROSS JOIN `users` u
WHERE c.code LIKE 'TEST-COUPON-%' AND u.username LIKE 'test_student%'
  AND MOD(c.id+CAST(RIGHT(u.username,2) AS UNSIGNED),4)=0;

INSERT IGNORE INTO `store_orders`
 (`order_no`,`buyer_id`,`total_amount`,`original_amount`,`discount_amount`,`payment_mode`,
  `status`,`shipping_status`,`tracking_no`,`shipping_remark`,`created_at`,`paid_at`,
  `cancelled_at`,`completed_at`)
SELECT CONCAT('TEST-ORDER-',LPAD(n,4,'0')),u.id,10+n,10+n+MOD(n,4)*2,MOD(n,4)*2,'SELF',
       CASE MOD(n,5) WHEN 0 THEN 'CREATED' WHEN 1 THEN 'PAID' WHEN 2 THEN 'COMPLETED'
         WHEN 3 THEN 'CANCELLED' ELSE 'REFUNDED' END,
       CASE MOD(n,5) WHEN 1 THEN 'PREPARING' WHEN 2 THEN 'DELIVERED' ELSE NULL END,
       IF(MOD(n,5)=2,CONCAT('TEST-TRACK-',LPAD(n,4,'0')),NULL),
       IF(MOD(n,5)=2,'已送达测试收货点',NULL),DATE_SUB(NOW(3),INTERVAL n DAY),
       IF(MOD(n,5) IN (1,2,4),DATE_SUB(NOW(3),INTERVAL n DAY),NULL),
       IF(MOD(n,5)=3,DATE_SUB(NOW(3),INTERVAL n-1 DAY),NULL),
       IF(MOD(n,5)=2,DATE_SUB(NOW(3),INTERVAL n-1 DAY),NULL)
FROM `vcampus_seed_numbers` x JOIN `users` u
  ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0')) WHERE n<=40;

INSERT IGNORE INTO `store_orders`
 (`order_no`,`buyer_id`,`total_amount`,`original_amount`,`discount_amount`,`payment_mode`,
  `status`,`shipping_status`,`tracking_no`,`shipping_remark`,`created_at`,`paid_at`,
  `cancelled_at`,`completed_at`)
SELECT CONCAT('DEMO-ORDER-',LPAD(n,3,'0')),@seed_student,20+n,22+n,2,'SELF',
       CASE MOD(n,5) WHEN 0 THEN 'CREATED' WHEN 1 THEN 'PAID' WHEN 2 THEN 'COMPLETED'
         WHEN 3 THEN 'CANCELLED' ELSE 'REFUNDED' END,
       CASE MOD(n,5) WHEN 1 THEN 'PREPARING' WHEN 2 THEN 'DELIVERED' ELSE NULL END,
       IF(MOD(n,5)=2,CONCAT('DEMO-TRACK-',LPAD(n,3,'0')),NULL),
       IF(MOD(n,5)=2,'已送达九龙湖校区测试点',NULL),DATE_SUB(NOW(3),INTERVAL n*2 DAY),
       IF(MOD(n,5) IN (1,2,4),DATE_SUB(NOW(3),INTERVAL n*2 DAY),NULL),
       IF(MOD(n,5)=3,DATE_SUB(NOW(3),INTERVAL n*2-1 DAY),NULL),
       IF(MOD(n,5)=2,DATE_SUB(NOW(3),INTERVAL n*2-1 DAY),NULL)
FROM `vcampus_seed_numbers` WHERE n<=12;

INSERT IGNORE INTO `store_order_items`
 (`order_id`,`product_id`,`product_name_snapshot`,`unit_price_snapshot`,`quantity`,`line_amount`)
SELECT o.id,p.id,p.name,o.original_amount,1,o.original_amount
FROM `store_orders` o JOIN `products` p
  ON p.sku=CONCAT('TEST-SKU-',RIGHT(o.order_no,3)) WHERE o.order_no LIKE 'TEST-ORDER-%';

INSERT IGNORE INTO `store_order_items`
 (`order_id`,`product_id`,`product_name_snapshot`,`unit_price_snapshot`,`quantity`,`line_amount`)
SELECT o.id,p.id,p.name,o.original_amount,1,o.original_amount
FROM `store_orders` o JOIN `products` p
  ON p.sku=CONCAT('TEST-SKU-',RIGHT(o.order_no,3)) WHERE o.order_no LIKE 'DEMO-ORDER-%';

INSERT IGNORE INTO `store_product_reviews` (`product_id`,`order_id`,`user_id`,`score`,`content`)
SELECT i.product_id,o.id,o.buyer_id,1+MOD(o.id,5),CONCAT('扩展测试商品评价，订单 ',o.order_no)
FROM `store_orders` o JOIN `store_order_items` i ON i.order_id=o.id
WHERE o.order_no LIKE 'TEST-ORDER-%' AND o.status='COMPLETED';

INSERT IGNORE INTO `store_product_reviews` (`product_id`,`order_id`,`user_id`,`score`,`content`)
SELECT i.product_id,o.id,o.buyer_id,4+MOD(o.id,2),CONCAT('demo_student 已购商品评价，订单 ',o.order_no)
FROM `store_orders` o JOIN `store_order_items` i ON i.order_id=o.id
WHERE o.order_no LIKE 'DEMO-ORDER-%' AND o.status='COMPLETED';

INSERT IGNORE INTO `store_user_coupons` (`coupon_id`,`user_id`)
SELECT c.id,@seed_student FROM `store_coupons` c
WHERE c.code IN ('TEST-COUPON-01','TEST-COUPON-02','TEST-COUPON-03','TEST-COUPON-04');

INSERT IGNORE INTO `cart_items` (`cart_id`,`product_id`,`quantity`)
SELECT c.id,p.id,1+MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),3)
FROM `shopping_carts` c JOIN `users` u ON u.id=c.user_id JOIN `products` p
  ON p.sku=CONCAT('TEST-SKU-',LPAD(CAST(RIGHT(u.username,2) AS UNSIGNED),3,'0'))
WHERE u.username LIKE 'test_student%' AND CAST(RIGHT(u.username,2) AS UNSIGNED)<=10;

INSERT IGNORE INTO `account_transactions`
 (`account_id`,`transaction_type`,`amount`,`balance_before`,`balance_after`,`reference_type`,
  `reference_id`,`idempotency_key`,`operator_id`,`remark`)
SELECT a.id,'RECHARGE',500.00,0.00,500.00,'EXPANDED_SEED',u.id,
       CONCAT('EXPANDED-SEED-RECHARGE-',RIGHT(u.username,2)),@seed_system,'扩展测试账户初始化'
FROM `accounts` a JOIN `users` u ON u.id=a.user_id WHERE u.username LIKE 'test_student%';

-- Dormitory: buildings, rooms, beds, occupants, leave, repair and utility histories.
INSERT IGNORE INTO `dorm_buildings`
 (`building_code`,`building_name`,`address`,`gender_policy`,`status`)
SELECT CONCAT('TEST-D',n),CONCAT('测试宿舍楼 ',n),'九龙湖校区测试区域',
       CASE n WHEN 1 THEN 'MALE' WHEN 2 THEN 'FEMALE' ELSE 'MIXED' END,'OPEN'
FROM `vcampus_seed_numbers` WHERE n<=4;

INSERT IGNORE INTO `dorm_rooms`
 (`building_id`,`room_no`,`floor_no`,`capacity`,`room_type`,`status`,`description`)
SELECT b.id,CONCAT(100+1+MOD(n-1,5)),1+FLOOR((n-1)/5),4,
       IF(MOD(n,5)=0,'SUITE','STANDARD'),'AVAILABLE',CONCAT('扩展测试宿舍房间 ',n)
FROM `vcampus_seed_numbers` x JOIN `dorm_buildings` b
  ON b.building_code=CONCAT('TEST-D',1+FLOOR((x.n-1)/5)) WHERE n<=20;

INSERT IGNORE INTO `dorm_beds` (`room_id`,`bed_no`,`status`)
SELECT r.id,CAST(x.n AS CHAR),'AVAILABLE' FROM `dorm_rooms` r
CROSS JOIN `vcampus_seed_numbers` x JOIN `dorm_buildings` b ON b.id=r.building_id
WHERE b.building_code LIKE 'TEST-D%' AND x.n<=4;

INSERT IGNORE INTO `accommodation_records` (`student_user_id`,`bed_id`,`start_date`,`status`,`created_by`)
SELECT students.id,beds.id,'2026-09-01','ACTIVE',@seed_dorm
FROM (SELECT id,ROW_NUMBER() OVER(ORDER BY username) rn FROM `users`
      WHERE username LIKE 'test_student%') students
JOIN (SELECT db.id,ROW_NUMBER() OVER(ORDER BY d.building_code,r.room_no,db.bed_no) rn
      FROM `dorm_beds` db JOIN `dorm_rooms` r ON r.id=db.room_id
      JOIN `dorm_buildings` d ON d.id=r.building_id WHERE d.building_code LIKE 'TEST-D%') beds
  ON beds.rn=students.rn;
UPDATE `dorm_beds` b SET b.status='OCCUPIED'
WHERE EXISTS (SELECT 1 FROM `accommodation_records` a WHERE a.bed_id=b.id AND a.status='ACTIVE');

INSERT INTO `leave_requests`
 (`student_user_id`,`leave_type`,`start_at`,`end_at`,`reason`,`status`,`reviewed_by`,`reviewed_at`,`review_remark`)
SELECT u.id,CASE MOD(n,3) WHEN 0 THEN 'ILLNESS' WHEN 1 THEN 'PERSONAL' ELSE 'OFF_CAMPUS' END,
       DATE_ADD('2026-10-01 08:00:00',INTERVAL n DAY),DATE_ADD('2026-10-02 20:00:00',INTERVAL n DAY),
       CONCAT('EXPANDED-SEED-LEAVE-',LPAD(n,2,'0')),
       CASE MOD(n,4) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'APPROVED'
         WHEN 2 THEN 'REJECTED' ELSE 'CANCELLED' END,
       IF(MOD(n,4) IN (1,2),@seed_dorm,NULL),IF(MOD(n,4) IN (1,2),NOW(3),NULL),
       IF(MOD(n,4) IN (1,2),'扩展测试审核备注',NULL)
FROM `vcampus_seed_numbers` x JOIN `users` u
  ON u.username=CONCAT('test_student',LPAD(x.n,2,'0')) WHERE n<=20
 AND NOT EXISTS (SELECT 1 FROM `leave_requests` l
   WHERE l.reason=CONCAT('EXPANDED-SEED-LEAVE-',LPAD(x.n,2,'0')));

INSERT INTO `repair_orders`
 (`room_id`,`reporter_id`,`category`,`description`,`priority`,`status`,`handler_id`,
  `accepted_at`,`completed_at`,`evaluation_score`,`evaluation_note`)
SELECT a.room_id,u.id,CASE MOD(n,4) WHEN 0 THEN 'WATER' WHEN 1 THEN 'ELECTRICITY'
         WHEN 2 THEN 'FURNITURE' ELSE 'NETWORK' END,
       CONCAT('EXPANDED-SEED-REPAIR-',LPAD(n,2,'0')),
       CASE MOD(n,4) WHEN 0 THEN 'URGENT' WHEN 1 THEN 'NORMAL' WHEN 2 THEN 'HIGH' ELSE 'LOW' END,
       CASE MOD(n,4) WHEN 0 THEN 'SUBMITTED' WHEN 1 THEN 'ACCEPTED'
         WHEN 2 THEN 'IN_PROGRESS' ELSE 'COMPLETED' END,
       IF(MOD(n,4)=0,NULL,@seed_repair),IF(MOD(n,4)=0,NULL,NOW(3)),
       IF(MOD(n,4)=3,NOW(3),NULL),IF(MOD(n,4)=3,4,NULL),
       IF(MOD(n,4)=3,'维修及时',NULL)
FROM `vcampus_seed_numbers` x JOIN `users` u
  ON u.username=CONCAT('test_student',LPAD(x.n,2,'0'))
JOIN `vw_current_accommodation` a ON a.student_user_id=u.id WHERE n<=20
 AND NOT EXISTS (SELECT 1 FROM `repair_orders` r
   WHERE r.description=CONCAT('EXPANDED-SEED-REPAIR-',LPAD(x.n,2,'0')));

INSERT IGNORE INTO `utility_bills`
 (`room_id`,`period_start`,`period_end`,`electricity_units`,`water_units`,`total_amount`,
  `due_at`,`status`,`created_by`)
SELECT r.id,'2026-08-01','2026-08-31',80+MOD(r.id,30),12+MOD(r.id,8),
       40+MOD(r.id,20), '2026-09-20 23:59:00',IF(MOD(r.id,3)=0,'PAID','UNPAID'),@seed_dorm
FROM `dorm_rooms` r JOIN `dorm_buildings` b ON b.id=r.building_id
WHERE b.building_code LIKE 'TEST-D%';

INSERT INTO `utility_allocations` (`bill_id`,`student_user_id`,`amount`,`status`)
SELECT ub.id,a.student_user_id,ub.total_amount,
       IF(ub.status='PAID','WAIVED','UNPAID')
FROM `utility_bills` ub JOIN `vw_current_accommodation` a ON a.room_id=ub.room_id
WHERE ub.period_start='2026-08-01' AND NOT EXISTS (
  SELECT 1 FROM `utility_allocations` ua
  WHERE ua.bill_id=ub.id AND ua.student_user_id=a.student_user_id);

-- Dormitory secondary workflows: readings, alerts, hygiene, permits and notice metadata.
INSERT INTO `dorm_meter_readings`
 (`room_id`,`period_start`,`period_end`,`electricity_units`,`water_units`,
  `electricity_price`,`water_price`,`recorded_by`)
SELECT r.id,'2026-09-01','2026-09-30',70+MOD(n*7,45),8+MOD(n*3,12),0.55,3.20,@seed_dorm
FROM `vcampus_seed_numbers` x JOIN `dorm_buildings` b
 ON b.building_code=CONCAT('TEST-D',1+FLOOR((x.n-1)/5))
JOIN `dorm_rooms` r ON r.building_id=b.id AND r.room_no=CONCAT(101+MOD(x.n-1,5))
WHERE n<=20 AND NOT EXISTS (SELECT 1 FROM `dorm_meter_readings` m
 WHERE m.room_id=r.id AND m.period_start='2026-09-01' AND m.period_end='2026-09-30');

INSERT INTO `late_return_alerts`
 (`student_user_id`,`alert_date`,`detected_at`,`status`,`handled_by`,`handled_at`,`note`)
SELECT u.id,DATE_SUB('2026-09-10',INTERVAL n DAY),DATE_SUB(NOW(3),INTERVAL n DAY),
       CASE MOD(n,4) WHEN 0 THEN 'OPEN' WHEN 1 THEN 'CONFIRMED'
         WHEN 2 THEN 'CLEARED' ELSE 'IGNORED' END,
       IF(MOD(n,4)=0,NULL,@seed_dorm),IF(MOD(n,4)=0,NULL,NOW(3)),
       CONCAT('EXPANDED-SEED-LATE-',LPAD(n,2,'0'))
FROM `vcampus_seed_numbers` x JOIN `users` u
 ON u.username=CONCAT('test_student',LPAD(x.n,2,'0')) WHERE n<=20
 AND NOT EXISTS (SELECT 1 FROM `late_return_alerts` a
  WHERE a.student_user_id=u.id AND a.alert_date=DATE_SUB('2026-09-10',INTERVAL x.n DAY));

INSERT INTO `dorm_absence_warnings`
 (`student_user_id`,`room_id`,`scan_date`,`last_leave_at`,`absence_days`,`warning_level`,
  `handle_status`,`notified_teacher_id`,`notified_at`,`note`)
SELECT a.student_user_id,a.room_id,DATE_SUB('2026-09-10',INTERVAL n DAY),
       DATE_SUB(NOW(3),INTERVAL 2+MOD(n,9) DAY),2+MOD(n,9),
       CASE MOD(n,3) WHEN 0 THEN 'NORMAL' WHEN 1 THEN 'SEVERE' ELSE 'EXEMPT' END,
       CASE MOD(n,3) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'NOTIFIED' ELSE 'VERIFIED' END,
       IF(MOD(n,3)=1,t.id,NULL),IF(MOD(n,3)=1,NOW(3),NULL),
       CONCAT('EXPANDED-SEED-ABSENCE-',LPAD(n,2,'0'))
FROM `vcampus_seed_numbers` x
JOIN (SELECT v.*,ROW_NUMBER() OVER(ORDER BY v.student_user_id) rn FROM `vw_current_accommodation` v
      JOIN `users` u0 ON u0.id=v.student_user_id WHERE u0.username LIKE 'test_student%') a ON a.rn=x.n
JOIN `users` t ON t.username=CONCAT('test_teacher',LPAD(1+MOD(x.n-1,6),2,'0'))
WHERE n<=20 AND NOT EXISTS (SELECT 1 FROM `dorm_absence_warnings` w
 WHERE w.student_user_id=a.student_user_id AND w.scan_date=DATE_SUB('2026-09-10',INTERVAL x.n DAY));

INSERT INTO `hygiene_inspections`
 (`room_id`,`inspector_id`,`inspected_at`,`score`,`result`,`issue_description`,`status`,
  `rectified_at`,`rectification_note`)
SELECT r.id,@seed_dorm,DATE_SUB(NOW(3),INTERVAL n DAY),
       CASE MOD(n,3) WHEN 1 THEN 92 ELSE 68 END,
       IF(MOD(n,3)=1,'PASS','FAIL'),CONCAT('EXPANDED-SEED-HYGIENE-',LPAD(n,2,'0')),
       CASE MOD(n,3) WHEN 0 THEN 'RECTIFICATION_REQUIRED' WHEN 1 THEN 'NORMAL' ELSE 'RECTIFIED' END,
       IF(MOD(n,3)=2,NOW(3),NULL),IF(MOD(n,3)=2,'已完成复查整改',NULL)
FROM `vcampus_seed_numbers` x JOIN `dorm_buildings` b
 ON b.building_code=CONCAT('TEST-D',1+FLOOR((x.n-1)/5))
JOIN `dorm_rooms` r ON r.building_id=b.id AND r.room_no=CONCAT(101+MOD(x.n-1,5))
WHERE n<=16 AND NOT EXISTS (SELECT 1 FROM `hygiene_inspections` h
 WHERE h.issue_description=CONCAT('EXPANDED-SEED-HYGIENE-',LPAD(x.n,2,'0')));

INSERT IGNORE INTO `dorm_hygiene_item_scores` (`inspection_id`,`item_code`,`score`,`deduct_reason`)
SELECT h.id,c.item_code,14+MOD(h.id+LENGTH(c.item_code),7),
       IF(MOD(h.id+LENGTH(c.item_code),4)=0,'扩展测试扣分原因',NULL)
FROM `hygiene_inspections` h CROSS JOIN
 (SELECT 'FLOOR' item_code UNION ALL SELECT 'DESK' UNION ALL SELECT 'BED'
  UNION ALL SELECT 'BATHROOM' UNION ALL SELECT 'BALCONY') c
WHERE h.issue_description LIKE 'EXPANDED-SEED-HYGIENE-%';

INSERT IGNORE INTO `dorm_hygiene_tasks`
 (`room_id`,`task_type`,`plan_date`,`status`,`inspection_id`,`source_inspection_id`)
SELECT h.room_id,'WEEKLY',DATE_ADD('2026-09-15',INTERVAL MOD(h.id,14) DAY),
       CASE MOD(h.id,3) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'DONE' ELSE 'SKIPPED' END,
       IF(MOD(h.id,3)=1,h.id,NULL),NULL
FROM `hygiene_inspections` h WHERE h.issue_description LIKE 'EXPANDED-SEED-HYGIENE-%';

INSERT IGNORE INTO `dorm_repair_entry_permits`
 (`repair_order_id`,`allow_enter`,`note`,`updated_by`)
SELECT r.id,MOD(r.id,2),CONCAT('扩展测试入户许可 ',r.id),r.reporter_id
FROM `repair_orders` r WHERE r.description LIKE 'EXPANDED-SEED-REPAIR-%';

INSERT IGNORE INTO `dorm_notice_extras`
 (`announcement_id`,`notice_type`,`scope_type`,`pinned`,`pinned_at`,`updated_by`)
SELECT a.id,CASE MOD(a.id,4) WHEN 0 THEN 'GENERAL' WHEN 1 THEN 'MAINTENANCE'
       WHEN 2 THEN 'HYGIENE' ELSE 'SAFETY' END,'ALL',MOD(a.id,3)=0,
       IF(MOD(a.id,3)=0,a.publish_at,NULL),@seed_dorm
FROM `announcements` a WHERE a.module_code='DORM' AND a.title LIKE '扩展测试公告 %';

-- Administrator and dormitory workflow queues.
INSERT INTO `account_cancellation_requests`
 (`user_id`,`reason`,`status`,`reviewed_by`,`reviewed_at`,`review_remark`)
SELECT u.id,CONCAT('EXPANDED-SEED-CANCEL-',LPAD(n,2,'0')),
       CASE MOD(n,3) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'REJECTED' ELSE 'CANCELLED' END,
       IF(MOD(n,3)=1,@seed_system,NULL),IF(MOD(n,3)=1,NOW(3),NULL),
       IF(MOD(n,3)=1,'保留账号继续使用',NULL)
FROM `vcampus_seed_numbers` x JOIN `users` u
 ON u.username=CONCAT('test_student',LPAD(x.n,2,'0')) WHERE n<=12
 AND NOT EXISTS (SELECT 1 FROM `account_cancellation_requests` r
  WHERE r.reason=CONCAT('EXPANDED-SEED-CANCEL-',LPAD(x.n,2,'0')));

INSERT INTO `classroom_reservations`
 (`classroom_id`,`applicant_id`,`purpose`,`start_at`,`end_at`,`status`,
  `reviewed_by`,`reviewed_at`,`review_remark`)
SELECT cr.id,u.id,CONCAT('EXPANDED-SEED-CLASSROOM-',LPAD(n,2,'0')),
       DATE_ADD('2026-11-01 08:00:00',INTERVAL n DAY),
       DATE_ADD('2026-11-01 10:00:00',INTERVAL n DAY),
       CASE MOD(n,4) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'APPROVED'
         WHEN 2 THEN 'REJECTED' ELSE 'CANCELLED' END,
       IF(MOD(n,4) IN (1,2),@seed_academic,NULL),IF(MOD(n,4) IN (1,2),NOW(3),NULL),
       IF(MOD(n,4) IN (1,2),'扩展测试教室审核',NULL)
FROM `vcampus_seed_numbers` x JOIN `classrooms` cr
 ON cr.building_name='测试教学楼' AND cr.room_no=CONCAT(100+1+MOD(x.n-1,18))
JOIN `users` u ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0'))
WHERE n<=24 AND NOT EXISTS (SELECT 1 FROM `classroom_reservations` r
 WHERE r.purpose=CONCAT('EXPANDED-SEED-CLASSROOM-',LPAD(x.n,2,'0')));

INSERT INTO `accommodation_requests`
 (`student_user_id`,`request_type`,`current_record_id`,`requested_bed_id`,`reason`,
  `status`,`reviewed_by`,`reviewed_at`,`review_remark`)
SELECT a.student_user_id,IF(MOD(n,2)=0,'TRANSFER','CHECK_OUT'),a.accommodation_id,
       IF(MOD(n,2)=0,free_bed.id,NULL),CONCAT('EXPANDED-SEED-DORM-REQUEST-',LPAD(n,2,'0')),
       CASE MOD(n,3) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'REJECTED' ELSE 'CANCELLED' END,
       IF(MOD(n,3)=1,@seed_dorm,NULL),IF(MOD(n,3)=1,NOW(3),NULL),
       IF(MOD(n,3)=1,'扩展测试住宿审核',NULL)
FROM `vcampus_seed_numbers` x
JOIN (SELECT v.*,ROW_NUMBER() OVER(ORDER BY v.student_user_id) rn FROM `vw_current_accommodation` v
      JOIN `users` u0 ON u0.id=v.student_user_id WHERE u0.username LIKE 'test_student%') a ON a.rn=x.n
LEFT JOIN (SELECT db.id,ROW_NUMBER() OVER(ORDER BY db.id DESC) rn FROM `dorm_beds` db
      WHERE db.status='AVAILABLE') free_bed ON free_bed.rn=x.n
WHERE n<=12 AND (MOD(n,2)=1 OR free_bed.id IS NOT NULL)
 AND NOT EXISTS (SELECT 1 FROM `accommodation_requests` r
  WHERE r.reason=CONCAT('EXPANDED-SEED-DORM-REQUEST-',LPAD(x.n,2,'0')));

INSERT INTO `access_records` (`student_user_id`,`record_type`,`occurred_at`,`door_name`,`source`,`note`)
SELECT u.id,IF(MOD(n,2)=0,'ENTRY','EXIT'),DATE_SUB(NOW(3),INTERVAL n HOUR),
       '测试宿舍东门','CARD',CONCAT('EXPANDED-SEED-ACCESS-',LPAD(n,2,'0'))
FROM `vcampus_seed_numbers` x JOIN `users` u
 ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0')) WHERE n<=40
 AND NOT EXISTS (SELECT 1 FROM `access_records` r
  WHERE r.note=CONCAT('EXPANDED-SEED-ACCESS-',LPAD(x.n,2,'0')));

INSERT INTO `dorm_visitor_registrations`
 (`student_user_id`,`room_id`,`visitor_name`,`visitor_id_card`,`visitor_phone`,`visit_reason`,
  `start_at`,`end_at`,`audit_status`,`auditor_id`,`audited_at`,`audit_remark`)
SELECT a.student_user_id,a.room_id,CONCAT('访客',LPAD(n,2,'0')),
       CONCAT('32010020000101',LPAD(n,4,'0')),CONCAT('1350000',LPAD(n,4,'0')),
       CONCAT('EXPANDED-SEED-VISITOR-',LPAD(n,2,'0')),
       DATE_ADD('2026-10-20 09:00:00',INTERVAL n DAY),DATE_ADD('2026-10-20 12:00:00',INTERVAL n DAY),
       CASE MOD(n,4) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'APPROVED'
         WHEN 2 THEN 'REJECTED' ELSE 'CANCELLED' END,
       IF(MOD(n,4) IN (1,2),@seed_dorm,NULL),IF(MOD(n,4) IN (1,2),NOW(3),NULL),
       IF(MOD(n,4) IN (1,2),'扩展测试访客审核',NULL)
FROM `vcampus_seed_numbers` x JOIN
 (SELECT v.*,ROW_NUMBER() OVER(ORDER BY v.student_user_id) rn FROM `vw_current_accommodation` v
  JOIN `users` u ON u.id=v.student_user_id WHERE u.username LIKE 'test_student%') a ON a.rn=x.n
WHERE n<=20 AND NOT EXISTS (SELECT 1 FROM `dorm_visitor_registrations` r
 WHERE r.visit_reason=CONCAT('EXPANDED-SEED-VISITOR-',LPAD(x.n,2,'0')));

-- PDF rows store metadata only; they do not pretend that a file exists on disk.
INSERT INTO `library_pdf_resources`
 (`title`,`description`,`file_name`,`file_size`,`sha256`,`uploader_id`,`uploader_name`,
  `status`,`uploaded_at`,`reviewer_id`,`reviewer_name`,`reviewed_at`,`rejection_reason`)
SELECT CONCAT('扩展测试文献 ',LPAD(n,2,'0')),CONCAT('PDF 元数据测试 ',n),
       CONCAT('test-paper-',LPAD(n,2,'0'),'.pdf'),1024+n,SHA2(CONCAT('vcampus-pdf-',n),256),
       u.id,u.display_name,CASE MOD(n,4) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'APPROVED'
         WHEN 2 THEN 'REJECTED' ELSE 'INACTIVE' END,1788998400000+n*1000,
       IF(MOD(n,4)=0,0,@seed_librarian),IF(MOD(n,4)=0,NULL,'演示图书管理员'),
       IF(MOD(n,4)=0,0,1788998500000+n*1000),IF(MOD(n,4)=2,'扩展测试驳回原因',NULL)
FROM `vcampus_seed_numbers` x JOIN `users` u
 ON u.username=CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0')) WHERE n<=16
 AND NOT EXISTS (SELECT 1 FROM `library_pdf_resources` p
 WHERE p.sha256=SHA2(CONCAT('vcampus-pdf-',x.n),256));

INSERT INTO `library_pdf_downloads`
 (`user_id`,`resource_id`,`title`,`file_name`,`file_size`,`sha256`,`transferred`,
  `status`,`started_at`,`finished_at`,`failure_reason`)
SELECT u.id,p.id,p.title,p.file_name,p.file_size,p.sha256,
       IF(MOD(n,3)=0,FLOOR(p.file_size/2),p.file_size),
       CASE MOD(n,3) WHEN 0 THEN 'FAILED' WHEN 1 THEN 'COMPLETED' ELSE 'DOWNLOADING' END,
       1788999000000+n*1000,IF(MOD(n,3)=2,0,1788999100000+n*1000),
       IF(MOD(n,3)=0,'扩展测试网络中断',NULL)
FROM `vcampus_seed_numbers` x JOIN `library_pdf_resources` p
 ON p.sha256=SHA2(CONCAT('vcampus-pdf-',x.n),256)
JOIN `users` u ON u.username=IF(MOD(x.n,2)=0,'demo_student',
 CONCAT('test_student',LPAD(1+MOD(x.n-1,20),2,'0')))
WHERE n<=16 AND NOT EXISTS (SELECT 1 FROM `library_pdf_downloads` d
 WHERE d.user_id=u.id AND d.resource_id=p.id AND d.started_at=1788999000000+x.n*1000);

INSERT INTO `ai_knowledge_chunks` (`source_type`,`title`,`content`,`status`,`updated_by`)
SELECT 'EXPANDED_DEMO',CONCAT('扩展校园知识 ',LPAD(n,2,'0')),
       CONCAT('这是第 ',n,' 条校园办事测试知识，覆盖学籍、选课、图书、商店、宿舍和账号服务。'),
       'ACTIVE',@seed_system FROM `vcampus_seed_numbers` x WHERE n<=24
 AND NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` k
 WHERE k.title=CONCAT('扩展校园知识 ',LPAD(x.n,2,'0')));

INSERT INTO `ai_tool_call_logs`
 (`request_id`,`tool_name`,`action_type`,`arguments_json`,`result_summary`,`status`,
  `requested_by`,`confirmed_by`,`created_at`,`completed_at`)
SELECT CONCAT('EXPANDED-SEED-AI-',LPAD(n,3,'0')),
       CASE MOD(n,4) WHEN 0 THEN 'academic.course.list' WHEN 1 THEN 'library.book.search'
         WHEN 2 THEN 'store.product.list' ELSE 'student.profile.self' END,
       IF(MOD(n,5)=0,'WRITE','READ'),JSON_OBJECT('seed',TRUE,'sequence',n),
       CONCAT('扩展测试工具调用结果 ',n),
       CASE MOD(n,4) WHEN 0 THEN 'REQUESTED' WHEN 1 THEN 'SUCCEEDED'
         WHEN 2 THEN 'FAILED' ELSE 'CANCELLED' END,@seed_student,
       IF(MOD(n,5)=0,@seed_student,NULL),DATE_SUB(NOW(3),INTERVAL n HOUR),
       IF(MOD(n,4)=0,NULL,NOW(3))
FROM `vcampus_seed_numbers` x WHERE n<=20 AND NOT EXISTS (SELECT 1 FROM `ai_tool_call_logs` l
 WHERE l.request_id=CONCAT('EXPANDED-SEED-AI-',LPAD(x.n,3,'0')));

INSERT INTO `audit_logs`
 (`actor_user_id`,`actor_role_id`,`action`,`resource_type`,`resource_id`,`outcome`,`detail_json`,`occurred_at`)
SELECT @seed_system,(SELECT id FROM `roles` WHERE code='SYSTEM_ADMIN'),
       CONCAT('EXPANDED_SEED_ACTION_',LPAD(n,2,'0')),'DEMO_DATA',n,
       IF(MOD(n,5)=0,'FAILURE','SUCCESS'),JSON_OBJECT('seed',TRUE,'sequence',n),
       DATE_SUB(NOW(3),INTERVAL n HOUR)
FROM `vcampus_seed_numbers` x WHERE n<=30 AND NOT EXISTS (SELECT 1 FROM `audit_logs` a
  WHERE a.action=CONCAT('EXPANDED_SEED_ACTION_',LPAD(x.n,2,'0')));

DROP TEMPORARY TABLE `vcampus_seed_numbers`;
COMMIT;
