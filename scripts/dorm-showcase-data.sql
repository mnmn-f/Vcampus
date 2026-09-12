-- ============================================================================
-- 宿舍模块「即刻可演示」数据集（可重复执行）
--
-- 目标：登录任意一个宿舍相关角色，每个页签都有内容，每个动作都有一条现成的数据
-- 可以直接点，不用先在别的账号上造数据。
--
--   学生端 demo_student：住宿信息 / 调宿与退宿申请历史 / 待审核请假 / 待审核来访
--                        / 各状态报修单（可取消、可评价）/ 未缴水电 / 门禁流水含一次晚归
--                        / 卫生检查刚被判定需整改（首页会给整改提示）/ 分类公告
--   宿管端 demo_dorm：    待审批的调宿(TRANSFER)、入住(CHECK_IN)、退宿(CHECK_OUT) 各一条
--                        / 待审请假与来访 / 待派单、待审核报修 / 待出账抄表读数
--                        / 一般、严重、已豁免的未归预警 / 未处理晚归 / 待检卫生任务与复查任务
--   维修员 demo_repair：  在手工单（已接单 1 张、维修中 1 张）/ 处理记录（待审核、已完工）
--
-- 学生账号一律口令 student123：
--   demo_student 演示学生      D1-101-1 床
--   demo_stu2    钱雨桐        D1-101-2 床
--   demo_stu3    孙嘉怡        D1-101-3 床（离宿 4 天 → 一般预警）
--   demo_stu4    李思彤        D1-102-1 床（离宿 9 天 → 严重预警，已通知辅导员）
--   demo_stu5    周若曦        D1-103-1 床（离宿 12 天，但请了实习假 → 豁免；有待审批的调宿申请）
--   demo_stu6    吴欣然        D1-102-2 床（前天 23:35 归宿 → 晚归待处理）
--   demo_stu7    郑一诺        D2-101-1 床（离宿 20 天 → 严重预警，未处理）
--   demo_stu8    赵书瑶        尚未分配床位（有待审批的入住申请）
--   demo_stu9    王梓萱        D2-102-1 床（有待审批的退宿申请）
--
-- 幂等方式：先把这批学生、这几间房名下的宿舍数据全部删掉再重建，所以重复执行不会
-- 越堆越多；也会顺手清掉 V18 扩展测试数据在宿舍模块里留下的 EXPANDED-SEED 行和
-- 旧脚本留下的「演示数据：」行。不动其他模块的数据。
--
-- 前置：先执行过 scripts/apply-dorm-migrations.sql（需要 PENDING_REVIEW 状态和
-- 「申请可不填床位」两条约束），V7 已建好 demo_repair 账号。
--
-- 用法（Git Bash / PowerShell，项目根目录）：
--   mysql --default-character-set=utf8mb4 -u root -p vcampus < scripts/dorm-showcase-data.sql
-- ============================================================================

SET NAMES utf8mb4;

SET @sys     = (SELECT `id` FROM `users` WHERE `username` = 'demo_system');
SET @dorm    = (SELECT `id` FROM `users` WHERE `username` = 'demo_dorm');
SET @teacher = (SELECT `id` FROM `users` WHERE `username` = 'demo_teacher');
SET @worker  = (SELECT `id` FROM `users` WHERE `username` = 'demo_repair');
SET @pwd     = '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi';   -- student123
SET @role_student = (SELECT `id` FROM `roles` WHERE `code` = 'STUDENT');

-- ---------------------------------------------------------------------------
-- 1. 学生账号（存在则只更新姓名/手机号，不动口令以外的东西）
-- ---------------------------------------------------------------------------
INSERT INTO `users` (`username`, `password_hash`, `display_name`, `email`, `phone`, `status`) VALUES
    ('demo_stu2', @pwd, '钱雨桐', 'demo.stu2@vcampus.local', '13812340002', 'ACTIVE'),
    ('demo_stu3', @pwd, '孙嘉怡', 'demo.stu3@vcampus.local', '13812340003', 'ACTIVE'),
    ('demo_stu4', @pwd, '李思彤', 'demo.stu4@vcampus.local', '13812340004', 'ACTIVE'),
    ('demo_stu5', @pwd, '周若曦', 'demo.stu5@vcampus.local', '13812340005', 'ACTIVE'),
    ('demo_stu6', @pwd, '吴欣然', 'demo.stu6@vcampus.local', '13812340006', 'ACTIVE'),
    ('demo_stu7', @pwd, '郑一诺', 'demo.stu7@vcampus.local', '13812340007', 'ACTIVE'),
    ('demo_stu8', @pwd, '赵书瑶', 'demo.stu8@vcampus.local', '13812340008', 'ACTIVE'),
    ('demo_stu9', @pwd, '王梓萱', 'demo.stu9@vcampus.local', '13812340009', 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE `display_name` = new.display_name, `phone` = new.phone, `status` = 'ACTIVE';

INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT u.`id`, @role_student, @sys FROM `users` u
WHERE u.`username` IN ('demo_stu2','demo_stu3','demo_stu4','demo_stu5','demo_stu6','demo_stu7','demo_stu8','demo_stu9');

SET @stu1 = (SELECT `id` FROM `users` WHERE `username` = 'demo_student');
SET @stu2 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu2');
SET @stu3 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu3');
SET @stu4 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu4');
SET @stu5 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu5');
SET @stu6 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu6');
SET @stu7 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu7');
SET @stu8 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu8');
SET @stu9 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu9');

-- 报修入内许可要求学生留有手机号，V7 已给 demo_student 补过；这里兜底一次
UPDATE `users` SET `phone` = '13900000001' WHERE `id` = @stu1 AND `phone` IS NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT `id` FROM `users` WHERE `phone` = '13900000001') AS taken);

-- ---------------------------------------------------------------------------
-- 2. 楼栋、房间、床位
-- ---------------------------------------------------------------------------
INSERT INTO `dorm_buildings` (`building_code`, `building_name`, `address`, `gender_policy`, `status`) VALUES
    ('DEMO-D1', '九龙湖学生公寓D1', '九龙湖校区桃园片区', 'FEMALE', 'OPEN'),
    ('DEMO-D2', '九龙湖学生公寓D2', '九龙湖校区桃园片区', 'FEMALE', 'OPEN')
AS new
ON DUPLICATE KEY UPDATE `building_name` = new.building_name, `address` = new.address,
    `gender_policy` = new.gender_policy, `status` = new.status;
SET @b1 = (SELECT `id` FROM `dorm_buildings` WHERE `building_code` = 'DEMO-D1');
SET @b2 = (SELECT `id` FROM `dorm_buildings` WHERE `building_code` = 'DEMO-D2');

INSERT INTO `dorm_rooms` (`building_id`, `room_no`, `floor_no`, `capacity`, `room_type`, `status`, `description`) VALUES
    (@b1, '101', 1, 4, 'STANDARD', 'AVAILABLE', '四人间，朝南。'),
    (@b1, '102', 1, 4, 'STANDARD', 'AVAILABLE', '四人间，朝南。'),
    (@b1, '103', 1, 4, 'STANDARD', 'AVAILABLE', '四人间，朝北，靠近楼梯口。'),
    (@b1, '201', 2, 4, 'STANDARD', 'AVAILABLE', '四人间，本学期暂未安排入住。'),
    (@b2, '101', 1, 4, 'STANDARD', 'AVAILABLE', '四人间，朝南。'),
    (@b2, '102', 1, 4, 'STANDARD', 'AVAILABLE', '四人间，朝南。')
AS new
ON DUPLICATE KEY UPDATE `capacity` = new.capacity, `status` = new.status, `description` = new.description;

SET @r101  = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '101');
SET @r102  = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '102');
SET @r103  = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '103');
SET @r201  = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '201');
SET @r2101 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b2 AND `room_no` = '101');
SET @r2102 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b2 AND `room_no` = '102');

-- V2 给 101 建的床位叫 A/B/C/D，平面图和现实都用 1/2/3/4，这里统一改名
UPDATE `dorm_beds` SET `bed_no` = CASE `bed_no` WHEN 'A' THEN '1' WHEN 'B' THEN '2' WHEN 'C' THEN '3' WHEN 'D' THEN '4' END
WHERE `room_id` = @r101 AND `bed_no` IN ('A','B','C','D')
  AND NOT EXISTS (SELECT 1 FROM (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r101 AND `bed_no` IN ('1','2','3','4')) AS numbered);

INSERT IGNORE INTO `dorm_beds` (`room_id`, `bed_no`, `status`)
SELECT r.`id`, n.`bed_no`, 'AVAILABLE'
FROM (SELECT @r101 AS id UNION SELECT @r102 UNION SELECT @r103 UNION SELECT @r201 UNION SELECT @r2101 UNION SELECT @r2102) r
CROSS JOIN (SELECT '1' AS bed_no UNION SELECT '2' UNION SELECT '3' UNION SELECT '4') n;

-- ---------------------------------------------------------------------------
-- 3. 清场：这批学生 + 这几间房 + V18/旧脚本残留，全部删掉再重建
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `sc_students`;
CREATE TEMPORARY TABLE `sc_students` (`id` BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO `sc_students` SELECT `id` FROM `users`
WHERE `username` IN ('demo_student','demo_stu2','demo_stu3','demo_stu4','demo_stu5','demo_stu6','demo_stu7','demo_stu8','demo_stu9')
   OR `username` LIKE 'test_student%';

DROP TEMPORARY TABLE IF EXISTS `sc_rooms`;
CREATE TEMPORARY TABLE `sc_rooms` (`id` BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO `sc_rooms` SELECT r.`id` FROM `dorm_rooms` r JOIN `dorm_buildings` b ON b.`id` = r.`building_id`
WHERE b.`building_code` IN ('DEMO-D1','DEMO-D2') OR b.`building_code` LIKE 'TEST-D%';

-- 水电（分摊 → 读数解绑 → 账单）
DELETE ua FROM `utility_allocations` ua JOIN `utility_bills` ub ON ub.`id` = ua.`bill_id` WHERE ub.`room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE ua FROM `utility_allocations` ua WHERE ua.`student_user_id` IN (SELECT `id` FROM `sc_students`);
DELETE FROM `dorm_meter_readings` WHERE `room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE FROM `utility_bills` WHERE `room_id` IN (SELECT `id` FROM `sc_rooms`);
-- 卫生（任务 → 检查；分项随检查级联）
DELETE FROM `dorm_hygiene_tasks` WHERE `room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE FROM `hygiene_inspections` WHERE `room_id` IN (SELECT `id` FROM `sc_rooms`);
-- 报修（许可随工单级联）
DELETE FROM `repair_orders` WHERE `room_id` IN (SELECT `id` FROM `sc_rooms`) OR `reporter_id` IN (SELECT `id` FROM `sc_students`);
-- 来访、预警、晚归、门禁、请假
DELETE FROM `dorm_visitor_registrations` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`) OR `room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE FROM `dorm_absence_warnings` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`) OR `room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE FROM `late_return_alerts` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`);
DELETE FROM `access_records` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`);
DELETE FROM `leave_requests` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`);
-- 住宿申请与住宿记录
DELETE FROM `accommodation_requests` WHERE `student_user_id` IN (SELECT `id` FROM `sc_students`)
   OR `reason` LIKE 'EXPANDED-SEED%' OR `review_remark` LIKE '扩展测试%';
-- 住在这几间房里、但不在上面名单里的人（比如测试时手工分配的），他们的申请也得先清，否则外键拦住
DELETE rq FROM `accommodation_requests` rq JOIN `accommodation_records` ar ON ar.`id` = rq.`current_record_id`
  JOIN `dorm_beds` bd ON bd.`id` = ar.`bed_id` WHERE bd.`room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE rq FROM `accommodation_requests` rq JOIN `dorm_beds` bd ON bd.`id` = rq.`requested_bed_id`
  WHERE bd.`room_id` IN (SELECT `id` FROM `sc_rooms`);
DELETE ar FROM `accommodation_records` ar WHERE ar.`student_user_id` IN (SELECT `id` FROM `sc_students`);
DELETE ar FROM `accommodation_records` ar JOIN `dorm_beds` bd ON bd.`id` = ar.`bed_id` WHERE bd.`room_id` IN (SELECT `id` FROM `sc_rooms`);
-- 宿舍公告（扩展属性随公告级联）
DELETE FROM `announcements` WHERE `module_code` = 'DORM';
-- V18 的测试楼整栋拆掉（只剩空壳时才删得动，上面已把引用清空）
DELETE bd FROM `dorm_beds` bd JOIN `dorm_rooms` r ON r.`id` = bd.`room_id` JOIN `dorm_buildings` b ON b.`id` = r.`building_id` WHERE b.`building_code` LIKE 'TEST-D%';
DELETE r FROM `dorm_rooms` r JOIN `dorm_buildings` b ON b.`id` = r.`building_id` WHERE b.`building_code` LIKE 'TEST-D%';
DELETE FROM `dorm_buildings` WHERE `building_code` LIKE 'TEST-D%';

-- 床位编号 → id
SET @bed101_1 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r101 AND `bed_no` = '1');
SET @bed101_2 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r101 AND `bed_no` = '2');
SET @bed101_3 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r101 AND `bed_no` = '3');
SET @bed102_1 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r102 AND `bed_no` = '1');
SET @bed102_2 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r102 AND `bed_no` = '2');
SET @bed102_3 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r102 AND `bed_no` = '3');
SET @bed103_1 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r103 AND `bed_no` = '1');
SET @bed2101_1 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r2101 AND `bed_no` = '1');
SET @bed2102_1 = (SELECT `id` FROM `dorm_beds` WHERE `room_id` = @r2102 AND `bed_no` = '1');

-- ---------------------------------------------------------------------------
-- 4. 住宿记录
--    101：演示学生(1)、钱雨桐(2)、孙嘉怡(3)，4 床空
--    102：李思彤(1)、吴欣然(2)，3、4 床空 —— 审批调宿时在平面图上点这两张
--    103：周若曦(1) 独住            201：空房（出账时会被「无在住学生」跳过）
--    D2-101：郑一诺(1)              D2-102：王梓萱(1)
--    演示学生另有一条已结束的 102-3 床历史记录（上月调宿到 101）
-- ---------------------------------------------------------------------------
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `end_date`, `status`, `created_by`) VALUES
    (@stu1, @bed102_3, '2026-02-23', DATE_SUB(CURDATE(), INTERVAL 11 DAY), 'ENDED', @dorm);
SET @old_rec_stu1 = LAST_INSERT_ID();

INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`) VALUES
    (@stu1, @bed101_1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'ACTIVE', @dorm),
    (@stu2, @bed101_2, '2026-09-01', 'ACTIVE', @dorm),
    (@stu3, @bed101_3, '2026-09-01', 'ACTIVE', @dorm),
    (@stu4, @bed102_1, '2026-09-01', 'ACTIVE', @dorm),
    (@stu6, @bed102_2, '2026-09-01', 'ACTIVE', @dorm),
    (@stu5, @bed103_1, '2026-09-01', 'ACTIVE', @dorm),
    (@stu7, @bed2101_1, '2026-09-01', 'ACTIVE', @dorm),
    (@stu9, @bed2102_1, '2026-09-01', 'ACTIVE', @dorm);

SET @rec_stu5 = (SELECT `id` FROM `accommodation_records` WHERE `student_user_id` = @stu5 AND `status` = 'ACTIVE');
SET @rec_stu9 = (SELECT `id` FROM `accommodation_records` WHERE `student_user_id` = @stu9 AND `status` = 'ACTIVE');

UPDATE `dorm_beds` b SET b.`status` = IF(EXISTS (
    SELECT 1 FROM `accommodation_records` a WHERE a.`bed_id` = b.`id` AND a.`status` = 'ACTIVE'), 'OCCUPIED', 'AVAILABLE')
WHERE b.`room_id` IN (SELECT `id` FROM `sc_rooms`);

-- ---------------------------------------------------------------------------
-- 5. 住宿申请：待审批的调宿 / 入住 / 退宿各一条，外加演示学生自己的两条历史
-- ---------------------------------------------------------------------------
INSERT INTO `accommodation_requests`
    (`student_user_id`, `request_type`, `current_record_id`, `requested_bed_id`, `reason`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`, `created_at`) VALUES
    -- 宿管端可直接批复的三条
    (@stu5, 'TRANSFER', @rec_stu5, @bed102_3,
     '103 目前只有我一个人住，晚上回来楼道很暗，心里不太踏实。同班同学吴欣然住在 102，那边还有空床，希望能调过去和她同住，方便一起上下课。',
     'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (@stu8, 'CHECK_IN', NULL, NULL,
     '本学期从软件学院转入计算机学院，原宿舍在丁家桥校区，已办完转专业手续，目前在九龙湖没有床位，申请安排入住 D1。',
     'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (@stu9, 'CHECK_OUT', @rec_stu9, NULL,
     '下学期获批到上海一家企业实习六个月，实习期间在企业附近租房，申请自 9 月底起退宿，实习结束后再申请入住。',
     'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    -- 演示学生的历史：上月调宿获批，暑假退宿被驳回
    (@stu1, 'TRANSFER', @old_rec_stu1, @bed101_1,
     '原住 102 室，室友作息差异较大，经常凌晨才休息，长期影响我早上上课。听说 101 室有空床，申请调至 101。',
     'APPROVED', @dorm, DATE_SUB(NOW(), INTERVAL 11 DAY), '已与双方室友核实情况，同意调至 101 室 1 号床，请在本周内完成搬迁并到值班室登记。',
     DATE_SUB(NOW(), INTERVAL 14 DAY)),
    (@stu1, 'CHECK_OUT', @old_rec_stu1, NULL,
     '暑假两个月都不在校，想申请短期退宿，节省一部分住宿费。',
     'REJECTED', @dorm, DATE_SUB(NOW(), INTERVAL 75 DAY), '住宿费按学年收取，不支持假期短期退宿。暑假不留校请到值班室做离校登记即可。',
     DATE_SUB(NOW(), INTERVAL 78 DAY));

-- ---------------------------------------------------------------------------
-- 6. 请假 / 离校
-- ---------------------------------------------------------------------------
INSERT INTO `leave_requests`
    (`student_user_id`, `leave_type`, `start_at`, `end_at`, `reason`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`, `created_at`) VALUES
    -- 演示学生：一条待审（宿管端可批），一条已批准，一条被驳回
    (@stu1, 'PERSONAL',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '18:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 9 DAY), '20:00:00'),
     '表姐下周六在镇江老家办婚礼，家里希望我回去帮忙，周五晚上回去，周日晚饭后返校。',
     'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (@stu1, 'PERSONAL',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 21 DAY), '16:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 19 DAY), '21:00:00'),
     '回家参加外婆八十岁生日家宴。',
     'APPROVED', @dorm, DATE_SUB(NOW(), INTERVAL 23 DAY), '已核实，注意往返路上安全，按时返校。', DATE_SUB(NOW(), INTERVAL 24 DAY)),
    (@stu1, 'OTHER',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '20:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '08:00:00'),
     '室友生日，想和几个同学去新街口通宵唱歌庆祝。',
     'REJECTED', @dorm, DATE_SUB(NOW(), INTERVAL 11 DAY), '非必要情况不批准通宵外出，请在门禁时间前返校。', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    -- 周若曦：校外实习假，覆盖当前 → 连续未归被豁免
    (@stu5, 'OFF_CAMPUS',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 14 DAY), '08:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 16 DAY), '22:00:00'),
     '赴苏州一家软件企业参加为期一个月的校外实习，实习期间住企业安排的员工宿舍，实习证明已交辅导员。',
     'APPROVED', @dorm, DATE_SUB(NOW(), INTERVAL 15 DAY), '已收到实习单位接收函，同意。实习期间保持联系方式畅通。', DATE_SUB(NOW(), INTERVAL 16 DAY)),
    -- 钱雨桐：一条待审，让宿管端的请假列表不止一条
    (@stu2, 'ILLNESS',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '18:00:00'),
     '明天上午去市区医院复查，下午返校。',
     'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ---------------------------------------------------------------------------
-- 7. 来访登记
-- ---------------------------------------------------------------------------
INSERT INTO `dorm_visitor_registrations`
    (`student_user_id`, `room_id`, `visitor_name`, `visitor_id_card`, `visitor_phone`, `visit_reason`, `start_at`, `end_at`, `submitted_at`, `audit_status`, `auditor_id`, `audited_at`, `audit_remark`) VALUES
    (@stu1, @r101, '王丽华', '320102197505124521', '13905167788',
     '家长来校探望，顺便送换季衣物和被褥。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00:00'),
     DATE_SUB(NOW(), INTERVAL 6 HOUR), 'PENDING', NULL, NULL, NULL),
    (@stu1, @r101, '刘雨欣', '320582200409185624', '13851923366',
     '高中同学来南京旅游，白天到宿舍坐一会儿，不留宿。',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '13:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '20:00:00'),
     DATE_SUB(NOW(), INTERVAL 8 DAY), 'APPROVED', @dorm, DATE_SUB(NOW(), INTERVAL 7 DAY), '已核对身份信息，来访人员请在 21:00 前离开宿舍区。'),
    (@stu4, @r102, '李建国', '320106197009087736', '13705188899',
     '父亲来校送生活费和药品。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '17:00:00'),
     DATE_SUB(NOW(), INTERVAL 1 DAY), 'PENDING', NULL, NULL, NULL);

-- ---------------------------------------------------------------------------
-- 8. 门禁流水（连续未归扫描与晚归判定都从这里算）
-- ---------------------------------------------------------------------------
INSERT INTO `access_records` (`student_user_id`, `record_type`, `occurred_at`, `door_name`, `source`, `note`) VALUES
    -- 演示学生：近五天正常进出，前天 23:40 归宿一次（晚归）
    (@stu1, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '07:52:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '21:35:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '07:48:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '22:05:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:10:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '23:40:00'), 'D1 南门', 'CARD', '门禁后刷卡进入'),
    (@stu1, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '07:55:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '21:20:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'EXIT',  TIMESTAMP(CURDATE(), '07:45:00'), 'D1 南门', 'CARD', NULL),
    (@stu1, 'ENTRY', TIMESTAMP(CURDATE(), '12:10:00'), 'D1 南门', 'CARD', NULL),
    -- 钱雨桐：在宿；五天前 23:20 归宿过一次（已处理的晚归）
    (@stu2, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:00:00'), 'D1 南门', 'CARD', NULL),
    (@stu2, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '23:20:00'), 'D1 南门', 'CARD', NULL),
    (@stu2, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:00:00'), 'D1 南门', 'CARD', NULL),
    (@stu2, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '22:10:00'), 'D1 南门', 'CARD', NULL),
    -- 孙嘉怡：离宿 4 天 → 一般预警
    (@stu3, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '21:00:00'), 'D1 南门', 'CARD', NULL),
    (@stu3, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:30:00'), 'D1 南门', 'CARD', NULL),
    -- 李思彤：离宿 9 天 → 严重预警
    (@stu4, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '20:15:00'), 'D1 南门', 'CARD', NULL),
    (@stu4, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '14:00:00'), 'D1 南门', 'CARD', NULL),
    -- 周若曦：离宿 12 天，但有已批准的实习假 → 豁免
    (@stu5, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 12 DAY), '07:30:00'), 'D1 南门', 'CARD', NULL),
    -- 吴欣然：前天 23:35 归宿 → 晚归待处理
    (@stu6, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00:00'), 'D1 南门', 'CARD', NULL),
    (@stu6, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '23:35:00'), 'D1 南门', 'CARD', NULL),
    (@stu6, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:20:00'), 'D1 南门', 'CARD', NULL),
    (@stu6, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '21:50:00'), 'D1 南门', 'CARD', NULL),
    -- 郑一诺：离宿 20 天，无请假 → 严重预警
    (@stu7, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '10:00:00'), 'D2 东门', 'CARD', NULL),
    -- 王梓萱：在宿
    (@stu9, 'EXIT',  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:00:00'), 'D2 东门', 'CARD', NULL),
    (@stu9, 'ENTRY', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '21:05:00'), 'D2 东门', 'CARD', NULL);

-- 晚归记录：两条待处理、一条已核实
INSERT INTO `late_return_alerts` (`student_user_id`, `alert_date`, `detected_at`, `status`, `handled_by`, `handled_at`, `note`) VALUES
    (@stu1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '23:40:00'), 'OPEN', NULL, NULL, NULL),
    (@stu6, DATE_SUB(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '23:35:00'), 'OPEN', NULL, NULL, NULL),
    (@stu2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '23:20:00'), 'CLEARED', @dorm,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:10:00'), '参加学院迎新晚会彩排晚归，辅导员已出具证明。');

-- 连续未归预警：以「昨天」为扫描日铺一批，宿管端今天点「扫描」会再生成今天这批
INSERT INTO `dorm_warning_configs` (`id`, `warn_days`, `notify_days`, `exempt_on_leave`, `updated_by`)
VALUES (1, 3, 7, 1, @dorm) AS new
ON DUPLICATE KEY UPDATE `warn_days` = new.warn_days, `notify_days` = new.notify_days, `exempt_on_leave` = new.exempt_on_leave;

INSERT INTO `dorm_absence_warnings`
    (`student_user_id`, `room_id`, `scan_date`, `last_leave_at`, `absence_days`, `warning_level`, `handle_status`, `notified_teacher_id`, `notified_at`, `note`) VALUES
    (@stu3, @r101,  DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:30:00'),  3,  'NORMAL', 'PENDING',  NULL, NULL, NULL),
    (@stu4, @r102,  DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '14:00:00'),  8,  'SEVERE', 'NOTIFIED', @teacher,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:30:00'), '已电话通知辅导员，反馈学生在家备考研究生考试，家长知情。'),
    (@stu7, @r2101, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '10:00:00'), 19, 'SEVERE', 'PENDING',  NULL, NULL, NULL),
    (@stu5, @r103,  DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 12 DAY), '07:30:00'), 11, 'EXEMPT', 'VERIFIED', NULL, NULL,
     '校外实习请假已批准，按规则豁免。');

-- ---------------------------------------------------------------------------
-- 9. 报修工单：状态机每一格都放一张
-- ---------------------------------------------------------------------------
INSERT INTO `repair_orders`
    (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `handler_id`, `submitted_at`, `accepted_at`, `completed_at`, `evaluation_score`, `evaluation_note`) VALUES
    -- ① 刚提交：学生可取消，宿管可派单
    (@r101, @stu1, 'ELECTRICAL', '书桌上方的阅读灯接触不良，开关要按好几次才亮，有时用着用着会自己灭。', 'NORMAL', 'SUBMITTED', NULL,
     DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL, NULL),
    -- ② 刚提交（别的房间，高优先级）：宿管派单时能看到排序
    (@r102, @stu4, 'PLUMBING', '卫生间地漏堵塞，洗澡时积水会漫到门口，已经影响正常使用。', 'HIGH', 'SUBMITTED', NULL,
     DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, NULL, NULL),
    -- ③ 已派给维修员，尚未开工：维修员端可点「开工」
    (@r101, @stu1, 'FURNITURE', '衣柜左侧柜门铰链松动，柜门关不严，一碰就自己弹开。', 'NORMAL', 'ACCEPTED', @worker,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL, NULL, NULL),
    -- ④ 维修中：维修员端可点「报完工」
    (@r101, @stu2, 'APPLIANCE', '空调制冷效果很差，出风口有持续的异响，晚上开着睡不着。', 'HIGH', 'IN_PROGRESS', @worker,
     DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 40 HOUR), NULL, NULL, NULL),
    -- ⑤ 维修员已报完工，等宿管审核：宿管端可「通过」或「打回」
    (@r101, @stu1, 'DOOR_WINDOW', '靠窗一侧的窗户锁扣损坏，窗户关不严，下雨天会渗水到书桌上。', 'HIGH', 'PENDING_REVIEW', @worker,
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 60 HOUR), NULL, NULL, NULL),
    -- ⑥ 已完工，学生尚未评价：学生端可评价
    (@r101, @stu1, 'PLUMBING', '洗手池水龙头关不紧，一直滴水，夜里声音很明显。', 'NORMAL', 'COMPLETED', @worker,
     DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL),
    -- ⑦ 已完工且已评价：作为历史记录
    (@r101, @stu1, 'NETWORK', '墙上的网口松动，插上网线没有信号，换了网线也不行。', 'NORMAL', 'COMPLETED', @worker,
     DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), 5, '师傅来得很快，修好后还顺手帮我把线理整齐了。'),
    -- ⑧ 学生自己取消的
    (@r102, @stu6, 'FURNITURE', '床板有一块松动，翻身会响。', 'LOW', 'CANCELLED', NULL,
     DATE_SUB(NOW(), INTERVAL 8 DAY), NULL, NULL, NULL, NULL);

SET @ro_accepted  = (SELECT `id` FROM `repair_orders` WHERE `reporter_id` = @stu1 AND `status` = 'ACCEPTED');
SET @ro_review    = (SELECT `id` FROM `repair_orders` WHERE `reporter_id` = @stu1 AND `status` = 'PENDING_REVIEW');
SET @ro_submitted = (SELECT `id` FROM `repair_orders` WHERE `reporter_id` = @stu1 AND `status` = 'SUBMITTED');

-- 入内许可：一张允许不在场进门，一张要求本人在场
INSERT INTO `dorm_repair_entry_permits` (`repair_order_id`, `allow_enter`, `note`, `updated_by`) VALUES
    (@ro_accepted, 1, '白天都在上课，可直接联系宿管开门维修，贵重物品已收好。', @stu1),
    (@ro_review,   1, '维修时请提前十分钟打电话，我从教室赶回来开门。', @stu1),
    (@ro_submitted, 0, '希望本人在场，周三下午没课。', @stu1);

-- ---------------------------------------------------------------------------
-- 10. 水电：8 月账期。101 已出账（三人分摊、均未缴），其余房间读数已录、待出账
--     电 0.55 元/度，水 3.20 元/吨
-- ---------------------------------------------------------------------------
INSERT INTO `dorm_meter_readings`
    (`room_id`, `period_start`, `period_end`, `electricity_units`, `water_units`, `electricity_price`, `water_price`, `recorded_by`, `recorded_at`) VALUES
    (@r101,  '2026-08-01', '2026-08-31', 132.500, 21.300, 0.5500, 3.2000, @dorm, '2026-09-01 09:20:00'),
    (@r102,  '2026-08-01', '2026-08-31',  98.200, 15.600, 0.5500, 3.2000, @dorm, '2026-09-01 09:24:00'),
    (@r103,  '2026-08-01', '2026-08-31',  41.000,  6.800, 0.5500, 3.2000, @dorm, '2026-09-01 09:27:00'),
    (@r201,  '2026-08-01', '2026-08-31',  12.300,  0.500, 0.5500, 3.2000, @dorm, '2026-09-01 09:30:00'),
    (@r2101, '2026-08-01', '2026-08-31',  75.400, 12.000, 0.5500, 3.2000, @dorm, '2026-09-01 10:05:00'),
    (@r2102, '2026-08-01', '2026-08-31',  60.900,  9.400, 0.5500, 3.2000, @dorm, '2026-09-01 10:08:00');

-- 101：132.5×0.55 + 21.3×3.20 = 72.875 + 68.16 = 141.04 元，三人分摊 47.02 / 47.01 / 47.01
INSERT INTO `utility_bills`
    (`room_id`, `period_start`, `period_end`, `electricity_units`, `water_units`, `total_amount`, `due_at`, `status`, `created_by`, `created_at`) VALUES
    (@r101, '2026-08-01', '2026-08-31', 132.500, 21.300, 141.04, '2026-09-15 23:59:00', 'UNPAID', @dorm, '2026-09-01 09:35:00');
SET @bill101 = LAST_INSERT_ID();
UPDATE `dorm_meter_readings` SET `bill_id` = @bill101 WHERE `room_id` = @r101 AND `period_start` = '2026-08-01';

INSERT INTO `utility_allocations` (`bill_id`, `student_user_id`, `amount`, `status`) VALUES
    (@bill101, @stu1, 47.02, 'UNPAID'),
    (@bill101, @stu2, 47.01, 'UNPAID'),
    (@bill101, @stu3, 47.01, 'UNPAID');

-- ---------------------------------------------------------------------------
-- 11. 卫生检查
--     101：上周 92 分优秀；前天 64 分不合格 → 学生首页给整改提示，明天有复查任务
--     102：上周 58 分不合格，前天复查 82 分通过（前一条标记已整改）
--     103：上周 88 分
--     今天待检：102、103、D2-101、D2-102 的周检查任务
-- ---------------------------------------------------------------------------
INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`, `rectified_at`, `rectification_note`) VALUES
    (@r101, @dorm, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '10:05:00'), 92.00, 'PASS', NULL, 'NORMAL', NULL, NULL);
SET @hi101_good = LAST_INSERT_ID();
INSERT INTO `dorm_hygiene_item_scores` (`inspection_id`, `item_code`, `score`, `deduct_reason`) VALUES
    (@hi101_good, 'FLOOR', 19.00, NULL), (@hi101_good, 'DESK', 18.00, '一张书桌有少量杂物'), (@hi101_good, 'BED', 19.00, NULL),
    (@hi101_good, 'BATHROOM', 18.00, '镜面有水渍'), (@hi101_good, 'BALCONY', 18.00, '晾晒区有未收的衣架');

INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`, `rectified_at`, `rectification_note`) VALUES
    (@r101, @dorm, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:20:00'), 64.00, 'FAIL',
     '地面有零食碎屑未清扫，两张书桌堆放外卖盒和杂物，卫生间镜面与台面水渍明显，垃圾桶未及时清理。', 'RECTIFICATION_REQUIRED', NULL, NULL);
SET @hi101_bad = LAST_INSERT_ID();
INSERT INTO `dorm_hygiene_item_scores` (`inspection_id`, `item_code`, `score`, `deduct_reason`) VALUES
    (@hi101_bad, 'FLOOR', 12.00, '零食碎屑、头发未清扫'), (@hi101_bad, 'DESK', 11.00, '外卖盒与杂物堆放'), (@hi101_bad, 'BED', 15.00, '一张床被褥未整理'),
    (@hi101_bad, 'BATHROOM', 10.00, '镜面台面水渍、垃圾桶未清'), (@hi101_bad, 'BALCONY', 16.00, '地面有积水');

INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`, `rectified_at`, `rectification_note`) VALUES
    (@r102, @dorm, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '10:30:00'), 58.00, 'FAIL',
     '地面积水未清理，垃圾未倒，阳台堆放大量纸箱。', 'RECTIFIED',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:45:00'), '复查已通过，纸箱已清运。');
SET @hi102_bad = LAST_INSERT_ID();
INSERT INTO `dorm_hygiene_item_scores` (`inspection_id`, `item_code`, `score`, `deduct_reason`) VALUES
    (@hi102_bad, 'FLOOR', 9.00, '积水未清'), (@hi102_bad, 'DESK', 14.00, NULL), (@hi102_bad, 'BED', 15.00, NULL),
    (@hi102_bad, 'BATHROOM', 12.00, '垃圾未倒'), (@hi102_bad, 'BALCONY', 8.00, '堆放大量纸箱');

INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`, `rectified_at`, `rectification_note`) VALUES
    (@r102, @dorm, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:45:00'), 82.00, 'PASS', NULL, 'NORMAL', NULL, NULL);
SET @hi102_ok = LAST_INSERT_ID();
INSERT INTO `dorm_hygiene_item_scores` (`inspection_id`, `item_code`, `score`, `deduct_reason`) VALUES
    (@hi102_ok, 'FLOOR', 17.00, NULL), (@hi102_ok, 'DESK', 16.00, '书桌略乱'), (@hi102_ok, 'BED', 17.00, NULL),
    (@hi102_ok, 'BATHROOM', 16.00, NULL), (@hi102_ok, 'BALCONY', 16.00, NULL);

INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`, `rectified_at`, `rectification_note`) VALUES
    (@r103, @dorm, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '10:50:00'), 88.00, 'PASS', NULL, 'NORMAL', NULL, NULL);
SET @hi103 = LAST_INSERT_ID();
INSERT INTO `dorm_hygiene_item_scores` (`inspection_id`, `item_code`, `score`, `deduct_reason`) VALUES
    (@hi103, 'FLOOR', 18.00, NULL), (@hi103, 'DESK', 17.00, NULL), (@hi103, 'BED', 18.00, NULL),
    (@hi103, 'BATHROOM', 17.00, NULL), (@hi103, 'BALCONY', 18.00, NULL);

-- 检查任务：上周与前天的已完成；101 明天复查；今天四间待检
INSERT INTO `dorm_hygiene_tasks` (`room_id`, `task_type`, `plan_date`, `status`, `inspection_id`, `source_inspection_id`) VALUES
    (@r101, 'WEEKLY',  DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'DONE', @hi101_good, NULL),
    (@r102, 'WEEKLY',  DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'DONE', @hi102_bad,  NULL),
    (@r103, 'WEEKLY',  DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'DONE', @hi103,      NULL),
    (@r101, 'WEEKLY',  DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'DONE', @hi101_bad,  NULL),
    (@r102, 'RECHECK', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'DONE', @hi102_ok,   @hi102_bad),
    (@r101, 'RECHECK', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PENDING', NULL, @hi101_bad),
    (@r102,  'WEEKLY', CURDATE(), 'PENDING', NULL, NULL),
    (@r103,  'WEEKLY', CURDATE(), 'PENDING', NULL, NULL),
    (@r2101, 'WEEKLY', CURDATE(), 'PENDING', NULL, NULL),
    (@r2102, 'WEEKLY', CURDATE(), 'PENDING', NULL, NULL);

-- ---------------------------------------------------------------------------
-- 12. 宿舍公告（类型 / 范围 / 置顶各有代表；最后一条已过期，等定时任务下架）
-- ---------------------------------------------------------------------------
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`) VALUES
    ('DORM', '9 月 15 日（周二）下午全楼消防疏散演练',
     '为提高同学们的消防安全意识，桃园片区将于 9 月 15 日 15:00 组织消防疏散演练。届时楼内将拉响警报，请全体在寝同学听从楼层安全员指挥，沿疏散指示标志有序撤离至楼下空地集合。演练期间请勿使用电梯，行动不便的同学请提前告知值班室。',
     'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), @dorm);
SET @an1 = LAST_INSERT_ID();
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`) VALUES
    ('DORM', 'D1 热水系统检修通知：9 月 13 日 14:00–17:00 暂停供应热水',
     '因 D1 楼顶热水机组例行检修，9 月 13 日（周日）14:00 至 17:00 全楼暂停供应热水，冷水不受影响。请同学们合理安排洗浴时间，给大家带来不便敬请谅解。',
     'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), @dorm);
SET @an2 = LAST_INSERT_ID();
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`) VALUES
    ('DORM', '101 室卫生整改通知',
     '9 月 9 日周检中，101 室卫生评分 64 分，未达到 70 分整改线。主要问题：地面碎屑未清扫、书桌堆放杂物、卫生间水渍明显。请于 9 月 12 日复查前完成整改，复查通过后恢复本月评优资格。',
     'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), @dorm);
SET @an3 = LAST_INSERT_ID();
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`) VALUES
    ('DORM', '8 月水电费账单已出，请于 9 月 15 日前缴清',
     '8 月份各房间水电费账单已生成并按在住人数分摊到个人，请同学们在「水电账单」页面查看本人应缴金额，并于 9 月 15 日前通过校园卡余额完成缴纳。逾期未缴的房间将暂停用电额度充值。',
     'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), @dorm);
SET @an4 = LAST_INSERT_ID();
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`) VALUES
    ('DORM', '暑期留校住宿登记',
     '暑假期间需留校住宿的同学，请于 6 月 25 日前在本页面提交留校登记，留校期间统一调整至 D2 集中住宿。',
     'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 80 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), @dorm);
SET @an5 = LAST_INSERT_ID();

INSERT INTO `dorm_notice_extras` (`announcement_id`, `notice_type`, `scope_type`, `scope_building_id`, `scope_room_id`, `pinned`, `pinned_at`, `updated_by`) VALUES
    (@an1, 'URGENT',      'ALL',      NULL, NULL,  1, DATE_SUB(NOW(), INTERVAL 1 DAY), @dorm),
    (@an2, 'MAINTENANCE', 'BUILDING', @b1,  NULL,  0, NULL, @dorm),
    (@an3, 'HYGIENE',     'ROOM',     NULL, @r101, 0, NULL, @dorm),
    (@an4, 'GENERAL',     'ALL',      NULL, NULL,  1, DATE_SUB(NOW(), INTERVAL 8 DAY), @dorm),
    (@an5, 'GENERAL',     'ALL',      NULL, NULL,  0, NULL, @dorm);

DROP TEMPORARY TABLE IF EXISTS `sc_students`;
DROP TEMPORARY TABLE IF EXISTS `sc_rooms`;

-- ---------------------------------------------------------------------------
-- 自检：每一行都应该有数
-- ---------------------------------------------------------------------------
SELECT '在住学生' AS item, COUNT(*) AS n FROM `accommodation_records` WHERE `status` = 'ACTIVE'
UNION ALL SELECT '待审批住宿申请(应为3)', COUNT(*) FROM `accommodation_requests` WHERE `status` = 'PENDING'
UNION ALL SELECT '待审请假(应为2)', COUNT(*) FROM `leave_requests` WHERE `status` = 'PENDING'
UNION ALL SELECT '待审来访(应为2)', COUNT(*) FROM `dorm_visitor_registrations` WHERE `audit_status` = 'PENDING'
UNION ALL SELECT '报修工单(应为8)', COUNT(*) FROM `repair_orders`
UNION ALL SELECT '待出账读数(应为5)', COUNT(*) FROM `dorm_meter_readings` WHERE `bill_id` IS NULL
UNION ALL SELECT '未归预警(应为4)', COUNT(*) FROM `dorm_absence_warnings`
UNION ALL SELECT '未处理晚归(应为2)', COUNT(*) FROM `late_return_alerts` WHERE `status` = 'OPEN'
UNION ALL SELECT '今日待检卫生任务(应为4)', COUNT(*) FROM `dorm_hygiene_tasks` WHERE `status` = 'PENDING' AND `plan_date` = CURDATE()
UNION ALL SELECT '宿舍公告(应为5)', COUNT(*) FROM `announcements` WHERE `module_code` = 'DORM';
