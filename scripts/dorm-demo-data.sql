-- ============================================================================
-- 宿舍模块演示数据（可重复执行）
--
-- 目的是让宿管端那几个定时任务有东西可算。默认的演示库里只有一栋楼、一间房、
-- 一个学生，跑 absenceScan 扫出来是零条，跑 billGenerate 找不到待出账的读数，
-- 看不出任务到底干了什么。这里把每条规则的边界各铺一份数据：刚好不到阈值的、
-- 刚好到的、升成严重的、被请假豁免的，各来一个人。
--
-- 所有时间都相对 NOW() 算，所以什么时候跑都成立；带「演示数据：」标记的行会先
-- 删掉再重建，重复执行不会越堆越多。
--
-- 前置：先跑 scripts/apply-dorm-migrations.sql（里面有报修的 PENDING_REVIEW 状态，
-- 下面那张待审核工单要用到）。
--
-- 用法（mysql 客户端里，注意正斜杠）：
--     source D:/Vcampus/scripts/dorm-demo-data.sql
-- ============================================================================

-- Windows 的 mysql 客户端默认按控制台代码页（通常是 GBK）解释文件里的字节，
-- 而这个文件是 UTF-8，不声明的话所有中文都会变成乱码或直接报 1366/1267。
-- 放在最前面，后面所有语句都按 UTF-8 送给服务端。
SET NAMES utf8mb4;

SET @sys    = (SELECT `id` FROM `users` WHERE `username` = 'demo_system');
SET @dorm   = (SELECT `id` FROM `users` WHERE `username` = 'demo_dorm');
SET @worker = (SELECT `id` FROM `users` WHERE `username` = 'demo_repair');
SET @stu1   = (SELECT `id` FROM `users` WHERE `username` = 'demo_student');

-- ---------------------------------------------------------------------------
-- 1. 六个演示学生（口令都是 student123，和 demo_student 同一套开发约定）
-- ---------------------------------------------------------------------------
INSERT INTO `users` (`username`, `password_hash`, `display_name`, `email`, `status`) VALUES
    ('demo_stu2', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·钱二', 'demo.stu2@vcampus.local', 'ACTIVE'),
    ('demo_stu3', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·孙三', 'demo.stu3@vcampus.local', 'ACTIVE'),
    ('demo_stu4', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·李四', 'demo.stu4@vcampus.local', 'ACTIVE'),
    ('demo_stu5', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·周五', 'demo.stu5@vcampus.local', 'ACTIVE'),
    ('demo_stu6', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·吴六', 'demo.stu6@vcampus.local', 'ACTIVE'),
    ('demo_stu7', '$2a$10$XXapbEAQFSVxOLqHDkfWLuvSk3HlgskBuF9xxA3p4LFP5YpJ7gnAi', '演示学生·郑七', 'demo.stu7@vcampus.local', 'ACTIVE')
AS new ON DUPLICATE KEY UPDATE
    `password_hash` = new.password_hash, `display_name` = new.display_name, `status` = new.status;

INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT u.`id`, r.`id`, @sys FROM `users` u JOIN `roles` r ON r.`code` = 'STUDENT'
WHERE u.`username` IN ('demo_stu2','demo_stu3','demo_stu4','demo_stu5','demo_stu6','demo_stu7')
ON DUPLICATE KEY UPDATE `assigned_by` = @sys;

SET @stu2 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu2');
SET @stu3 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu3');
SET @stu4 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu4');
SET @stu5 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu5');
SET @stu6 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu6');
SET @stu7 = (SELECT `id` FROM `users` WHERE `username` = 'demo_stu7');

-- ---------------------------------------------------------------------------
-- 2. 第二栋楼与更多房间
-- 一栋楼一间房的时候，房间目录、平面图、卫生任务生成都只有一行，看不出排序和分组。
-- ---------------------------------------------------------------------------
INSERT INTO `dorm_buildings` (`building_code`, `building_name`, `address`, `gender_policy`, `status`)
VALUES ('DEMO-D2', '九龙湖学生公寓D2', '九龙湖校区', 'FEMALE', 'OPEN')
AS new ON DUPLICATE KEY UPDATE `building_name` = new.building_name, `status` = new.status;

SET @b1 = (SELECT `id` FROM `dorm_buildings` WHERE `building_code` = 'DEMO-D1');
SET @b2 = (SELECT `id` FROM `dorm_buildings` WHERE `building_code` = 'DEMO-D2');

INSERT INTO `dorm_rooms` (`building_id`, `room_no`, `floor_no`, `capacity`, `room_type`, `status`, `description`) VALUES
    (@b1, '102', 1, 4, 'STANDARD', 'AVAILABLE', '演示数据：住了两个人，还剩两张空床。'),
    (@b1, '103', 1, 4, 'STANDARD', 'AVAILABLE', '演示数据：住了一个人。'),
    (@b1, '201', 2, 4, 'STANDARD', 'AVAILABLE', '演示数据：整间空着，用来看「无在住学生，跳过出账」。'),
    (@b2, '101', 1, 4, 'STANDARD', 'AVAILABLE', '演示数据：另一栋楼，用来看按楼栋投放的公告。'),
    (@b2, '102', 1, 4, 'STANDARD', 'AVAILABLE', '演示数据：空房。')
AS new ON DUPLICATE KEY UPDATE `capacity` = new.capacity, `status` = new.status, `description` = new.description;

-- 每间房 A~D 四张床；已存在的不动，免得把已占用的床重置成空闲。
INSERT IGNORE INTO `dorm_beds` (`room_id`, `bed_no`, `status`)
SELECT r.`id`, l.`bed_no`, 'AVAILABLE'
FROM `dorm_rooms` r
CROSS JOIN (SELECT 'A' AS `bed_no` UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D') l
WHERE r.`building_id` IN (@b1, @b2);

SET @r101 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '101');
SET @r102 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '102');
SET @r103 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '103');
SET @r201 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b1 AND `room_no` = '201');
SET @r2101 = (SELECT `id` FROM `dorm_rooms` WHERE `building_id` = @b2 AND `room_no` = '101');

-- ---------------------------------------------------------------------------
-- 3. 住宿关系
--
-- 床位不写死成「B 床」：演示过程中床位会被调宿、退宿挪来挪去，写死的那张床可能
-- 已经有人了（uk_accommodation_active_bed 会直接拦下来）。改成「这间房里第一张
-- 没人住的床」，房间住满时 @bed 取到 NULL，那一条就整个跳过。
-- ---------------------------------------------------------------------------
SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r101
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu2, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu2 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r102
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu3, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu3 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r102
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu4, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu4 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r103
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu5, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu5 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r2101
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu6, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu6 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

SET @bed = (SELECT b.`id` FROM `dorm_beds` b WHERE b.`room_id` = @r2101
    AND NOT EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = b.`id` AND ar.`status` = 'ACTIVE')
    ORDER BY b.`bed_no` LIMIT 1);
INSERT INTO `accommodation_records` (`student_user_id`, `bed_id`, `start_date`, `status`, `created_by`)
SELECT @stu7, @bed, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', @dorm FROM DUAL
WHERE @bed IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `accommodation_records` WHERE `student_user_id` = @stu7 AND `status` = 'ACTIVE');
UPDATE `dorm_beds` SET `status` = 'OCCUPIED' WHERE `id` = @bed
  AND EXISTS (SELECT 1 FROM `accommodation_records` ar WHERE ar.`bed_id` = @bed AND ar.`status` = 'ACTIVE');

-- ---------------------------------------------------------------------------
-- 4. 门禁流水 —— absenceScan 的原料
--
-- 未归天数 = 最后一次 EXIT 到扫描日的天数，且此后没有 ENTRY。默认阈值是
-- 预警 3 天、通知 7 天，所以下面这几个人跑一次扫描会分别落在：
--   钱二 1 天  → 不到阈值，不生成预警
--   孙三 4 天  → 一般
--   李四 9 天  → 严重
--   周五 12 天 → 已豁免（下面给他批了假）
--   郑七 20 天 → 严重
--   吴六 昨晚回来了 → 在宿，同时是一条晚归
-- 每次重跑都按当前时间重铺，所以天数永远是上面这几个数。
-- ---------------------------------------------------------------------------
DELETE FROM `access_records` WHERE `note` LIKE '演示数据：%';

INSERT INTO `access_records` (`student_user_id`, `record_type`, `occurred_at`, `door_name`, `source`, `note`) VALUES
    (@stu2, 'EXIT',  DATE_SUB(NOW(), INTERVAL 1 DAY),  'D1-101门禁', 'MANUAL', '演示数据：离宿 1 天，够不上预警'),
    (@stu3, 'ENTRY', DATE_SUB(NOW(), INTERVAL 9 DAY),  'D1-102门禁', 'MANUAL', '演示数据：上一次归宿'),
    (@stu3, 'EXIT',  DATE_SUB(NOW(), INTERVAL 4 DAY),  'D1-102门禁', 'MANUAL', '演示数据：离宿 4 天，一般预警'),
    (@stu4, 'ENTRY', DATE_SUB(NOW(), INTERVAL 20 DAY), 'D1-102门禁', 'MANUAL', '演示数据：上一次归宿'),
    (@stu4, 'EXIT',  DATE_SUB(NOW(), INTERVAL 9 DAY),  'D1-102门禁', 'MANUAL', '演示数据：离宿 9 天，严重预警'),
    (@stu5, 'EXIT',  DATE_SUB(NOW(), INTERVAL 12 DAY), 'D1-103门禁', 'MANUAL', '演示数据：离宿 12 天，但请了假'),
    (@stu6, 'EXIT',  DATE_SUB(NOW(), INTERVAL 2 DAY),  'D2-101门禁', 'MANUAL', '演示数据：出门'),
    (@stu6, 'ENTRY', DATE_SUB(NOW(), INTERVAL 1 DAY),  'D2-101门禁', 'MANUAL', '演示数据：昨天深夜归宿，算晚归'),
    (@stu7, 'EXIT',  DATE_SUB(NOW(), INTERVAL 20 DAY), 'D2-101门禁', 'MANUAL', '演示数据：离宿 20 天，严重预警');

-- 吴六那条归宿落在门禁时段内（默认 23:00 落锁、次日 05:00 开门），才会被判成晚归。
UPDATE `access_records`
SET `occurred_at` = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '23:40:00')
WHERE `note` = '演示数据：昨天深夜归宿，算晚归';

-- 周五的请假：覆盖今天，用来验证「请假期间豁免」这个开关
INSERT INTO `leave_requests` (`student_user_id`, `leave_type`, `start_at`, `end_at`, `reason`, `status`, `reviewed_by`, `reviewed_at`, `review_remark`)
SELECT @stu5, 'OFF_CAMPUS', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
       '校外实习（演示数据）', 'APPROVED', @dorm, DATE_SUB(NOW(), INTERVAL 14 DAY), '演示数据：已批准'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `leave_requests` WHERE `student_user_id` = @stu5 AND `reason` = '校外实习（演示数据）');

-- 一条待审批的请假，给「申请与审批」页的合并待办表留个活儿
INSERT INTO `leave_requests` (`student_user_id`, `leave_type`, `start_at`, `end_at`, `reason`, `status`)
SELECT @stu2, 'PERSONAL', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
       '回家办事（演示数据，待审批）', 'PENDING'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `leave_requests` WHERE `student_user_id` = @stu2 AND `reason` = '回家办事（演示数据，待审批）');

-- 两条待处理的晚归，给「未归管理」页的晚归记录和「处理晚归」按钮用
DELETE FROM `late_return_alerts` WHERE `note` LIKE '演示数据：%';
INSERT INTO `late_return_alerts` (`student_user_id`, `alert_date`, `detected_at`, `status`, `note`) VALUES
    (@stu6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '23:40:00'), 'OPEN', '演示数据：23:40 归宿'),
    (@stu2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '23:55:00'), 'OPEN', '演示数据：23:55 归宿');

-- ---------------------------------------------------------------------------
-- 5. 抄表读数 —— billGenerate 的原料
--
-- 账期取上一个自然月，和月度出账任务算出来的区间完全一致。四条读数分别演示
-- 四种结果：正常出账、按人分摊、该账期已有账单所以跳过、房间没人住所以跳过。
-- ---------------------------------------------------------------------------
SET @pstart = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01');
SET @pend   = LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH));

INSERT INTO `dorm_meter_readings`
    (`room_id`, `period_start`, `period_end`, `electricity_units`, `water_units`, `electricity_price`, `water_price`, `recorded_by`, `bill_id`)
VALUES
    (@r102,  @pstart, @pend, 168.400, 22.500, 0.6000, 3.5000, @dorm, NULL),
    (@r103,  @pstart, @pend,  95.200, 11.800, 0.6000, 3.5000, @dorm, NULL),
    (@r2101, @pstart, @pend, 203.700, 31.400, 0.6000, 3.5000, @dorm, NULL),
    (@r201,  @pstart, @pend,  12.000,  1.500, 0.6000, 3.5000, @dorm, NULL)
AS new ON DUPLICATE KEY UPDATE
    `electricity_units` = new.electricity_units, `water_units` = new.water_units,
    `electricity_price` = new.electricity_price, `water_price` = new.water_price;

-- ---------------------------------------------------------------------------
-- 6. 公告 —— noticeExpireScan 的原料
--
-- 第一条已经过期但状态还是「已发布」，跑一次下架任务就会变成「已过期」，
-- 学生端也会同时看不到它——这是唯一一个肉眼可见结果的定时任务。
-- ---------------------------------------------------------------------------
INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `expire_at`, `publisher_id`)
SELECT 'DORM', '【演示】暑期停水通知（已过期）',
       '7 月 20 日 08:00 至 18:00 D1 楼停水检修。这条公告的过期时间已经到了，但状态还停在「已发布」——在「设置」里执行一次 noticeExpireScan，它会被自动下架。',
       'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), @dorm
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `announcements` WHERE `module_code` = 'DORM' AND `title` = '【演示】暑期停水通知（已过期）');

-- 已经被下架过的话，重跑本脚本时恢复成「已发布」，方便再演示一次
UPDATE `announcements` SET `status` = 'PUBLISHED', `expire_at` = DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE `module_code` = 'DORM' AND `title` = '【演示】暑期停水通知（已过期）';

INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publish_at`, `publisher_id`)
SELECT 'DORM', '【演示】D2 楼电路检修，请勿使用大功率电器',
       '本周六 09:00-12:00 D2 楼分区停电检修。检修期间请关闭电脑、断开充电设备。给大家带来不便，敬请谅解。',
       'ALL', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), @dorm
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `announcements` WHERE `module_code` = 'DORM' AND `title` = '【演示】D2 楼电路检修，请勿使用大功率电器');

INSERT INTO `announcements` (`module_code`, `title`, `content`, `visible_scope`, `status`, `publisher_id`)
SELECT 'DORM', '【演示】草稿：期末宿舍卫生大检查安排',
       '这条是草稿，只有宿管端看得到，学生端列表里不该出现它。',
       'ALL', 'DRAFT', @dorm
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `announcements` WHERE `module_code` = 'DORM' AND `title` = '【演示】草稿：期末宿舍卫生大检查安排');

-- 类型、投放范围与置顶
INSERT IGNORE INTO `dorm_notice_extras` (`announcement_id`, `notice_type`, `scope_type`, `scope_building_id`, `pinned`, `pinned_at`, `updated_by`)
SELECT a.`id`, 'MAINTENANCE', 'BUILDING', @b2, 1, NOW(), @dorm FROM `announcements` a
WHERE a.`module_code` = 'DORM' AND a.`title` = '【演示】D2 楼电路检修，请勿使用大功率电器';

UPDATE `dorm_notice_extras` x JOIN `announcements` a ON a.`id` = x.`announcement_id`
SET x.`notice_type` = 'MAINTENANCE', x.`scope_type` = 'BUILDING', x.`scope_building_id` = @b2,
    x.`scope_room_id` = NULL, x.`pinned` = 1, x.`pinned_at` = NOW()
WHERE a.`module_code` = 'DORM' AND a.`title` = '【演示】D2 楼电路检修，请勿使用大功率电器';

INSERT IGNORE INTO `dorm_notice_extras` (`announcement_id`, `notice_type`, `scope_type`, `updated_by`)
SELECT a.`id`, 'SAFETY', 'ALL', @dorm FROM `announcements` a
WHERE a.`module_code` = 'DORM' AND a.`title` = '【演示】暑期停水通知（已过期）';

-- ---------------------------------------------------------------------------
-- 7. 报修工单 —— 每种状态各一张，宿管端和维修员端都能直接点
-- ---------------------------------------------------------------------------
INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `submitted_at`)
SELECT @r102, @stu3, 'WATER', '洗手池下水管漏水，地上一直有积水。（演示数据·待派单）', 'HIGH', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `description` LIKE '%演示数据·待派单%');

INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `handler_id`, `submitted_at`, `accepted_at`)
SELECT @r103, @stu5, '门窗', '阳台推拉门轨道卡住，关不严。（演示数据·已派单）', 'NORMAL', 'ACCEPTED', @worker,
       DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `description` LIKE '%演示数据·已派单%');

INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `handler_id`, `submitted_at`, `accepted_at`)
SELECT @r2101, @stu6, 'LIGHTING', '书桌台灯插座没电，整排都不通。（演示数据·处理中）', 'NORMAL', 'IN_PROGRESS', @worker,
       DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `description` LIKE '%演示数据·处理中%');

-- 这一张已经报了完工，登录宿管端就能直接点「审核通过」或「打回重修」
INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `handler_id`, `submitted_at`, `accepted_at`)
SELECT @r101, @stu2, '空调', '空调不制冷，出风是常温。（演示数据·待宿管审核）', 'HIGH', 'PENDING_REVIEW', @worker,
       DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `description` LIKE '%演示数据·待宿管审核%');

INSERT INTO `repair_orders` (`room_id`, `reporter_id`, `category`, `description`, `priority`, `status`, `handler_id`, `submitted_at`, `accepted_at`, `completed_at`, `evaluation_score`, `evaluation_note`)
SELECT @r102, @stu4, '桌椅', '椅子腿松动，坐上去晃。（演示数据·已完成）', 'LOW', 'COMPLETED', @worker,
       DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY),
       5, '师傅来得很快，已经修好了。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `repair_orders` WHERE `description` LIKE '%演示数据·已完成%');

-- 入内授权：一张给了、一张没给，维修员端「能否入内」那一列才看得出差别
INSERT IGNORE INTO `dorm_repair_entry_permits` (`repair_order_id`, `allow_enter`, `note`, `updated_by`)
SELECT o.`id`, 1, '钥匙在宿管处，白天随时可以进。', o.`reporter_id` FROM `repair_orders` o
WHERE o.`description` LIKE '%演示数据·处理中%';

INSERT IGNORE INTO `dorm_repair_entry_permits` (`repair_order_id`, `allow_enter`, `note`, `updated_by`)
SELECT o.`id`, 0, NULL, o.`reporter_id` FROM `repair_orders` o
WHERE o.`description` LIKE '%演示数据·待派单%';

-- ---------------------------------------------------------------------------
-- 8. 卫生：一条不合格的检查 + 它的复查任务
-- 复查任务排在周检查前面，卫生页的任务顺序就是待办顺序，这条能验证排序。
-- ---------------------------------------------------------------------------
INSERT INTO `hygiene_inspections` (`room_id`, `inspector_id`, `inspected_at`, `score`, `result`, `issue_description`, `status`)
SELECT @r102, @dorm, DATE_SUB(NOW(), INTERVAL 5 DAY), 58.00, 'FAIL', '演示数据：地面积水、垃圾未清、阳台堆放杂物。', 'RECTIFICATION_REQUIRED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `hygiene_inspections` WHERE `issue_description` LIKE '演示数据：%');

SET @bad = (SELECT `id` FROM `hygiene_inspections` WHERE `issue_description` LIKE '演示数据：%' ORDER BY `id` LIMIT 1);

INSERT INTO `dorm_hygiene_tasks` (`room_id`, `task_type`, `plan_date`, `status`, `source_inspection_id`)
VALUES (@r102, 'RECHECK', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PENDING', @bad)
AS new ON DUPLICATE KEY UPDATE `status` = new.status, `source_inspection_id` = new.source_inspection_id;

-- 周检查任务刻意不在这里铺：它正是 hygieneTaskGenerate 要生成的东西。留空着，
-- 在「卫生管理」页点一下「补生成本周任务」，或在「设置」里执行一次这个任务，
-- 就能看到它按房间数一次性建出来（再点一次会报「已存在 N 条」，那是幂等）。

-- ---------------------------------------------------------------------------
-- 对一眼：跑完应该看到 7 个在住学生、11 间左右的房、4 条待出账读数、5 张工单。
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM `accommodation_records` WHERE `status` = 'ACTIVE') AS active_residents,
    (SELECT COUNT(*) FROM `dorm_rooms`) AS rooms,
    (SELECT COUNT(*) FROM `dorm_beds` WHERE `status` = 'AVAILABLE') AS free_beds,
    (SELECT COUNT(*) FROM `dorm_meter_readings` WHERE `bill_id` IS NULL) AS pending_readings,
    (SELECT COUNT(*) FROM `repair_orders`) AS repair_orders,
    (SELECT COUNT(*) FROM `dorm_hygiene_tasks` WHERE `status` = 'PENDING') AS hygiene_tasks;
