-- VCampus 宿舍模块扩展迁移 V5
--
-- 执行顺序：V1__baseline.sql -> V2__demo_data.sql -> V3__academic_insights.sql
-- -> V4__store_experience.sql -> 本脚本。编号取 V5 是因为教务和商店模块
-- 已经占用了 V3 和 V4；本脚本只新建 dorm_ 前缀的表，与它们没有依赖关系。
-- 本脚本只新增对象，不修改 V1 建立的任何表结构，可以重复执行。
--
-- 用 mysql 客户端执行：
--   source D:/Vcampus/vcampus-server/src/main/resources/db/migration/V3__dorm_extension.sql

USE `vcampus`;

SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';

-- ============================================================================
-- 水电抄表读数
--
-- 设计文档要求的 billGenerate 需要「上月抄表读数」作为输入，而 V1 只有账单
-- (utility_bills) 和分摊 (utility_allocations)，缺少读数来源，因此账单在现有
-- 实现里无法生成。本表补上这一环：宿管按房间和账期录入读数与单价，账单生成
-- 任务据此计算房间应缴总额，并回填 bill_id 把读数锁定。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_meter_readings` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `period_start` DATE NOT NULL,
    `period_end` DATE NOT NULL,
    `electricity_units` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '本账期用电量',
    `water_units` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '本账期用水量',
    `electricity_price` DECIMAL(10,4) NOT NULL COMMENT '电费单价',
    `water_price` DECIMAL(10,4) NOT NULL COMMENT '水费单价',
    `recorded_by` BIGINT UNSIGNED NOT NULL COMMENT '录入的宿管员',
    `recorded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `bill_id` BIGINT UNSIGNED NULL COMMENT '已生成账单时回填；非空表示读数已锁定',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_meter_readings_room_period` (`room_id`, `period_start`, `period_end`),
    KEY `idx_dorm_meter_readings_pending` (`bill_id`, `period_start`),
    CONSTRAINT `fk_dorm_meter_readings_room` FOREIGN KEY (`room_id`)
        REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_dorm_meter_readings_recorder` FOREIGN KEY (`recorded_by`)
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_dorm_meter_readings_bill` FOREIGN KEY (`bill_id`)
        REFERENCES `utility_bills` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_dorm_meter_readings_period` CHECK (`period_end` >= `period_start`),
    CONSTRAINT `ck_dorm_meter_readings_units`
        CHECK (`electricity_units` >= 0 AND `water_units` >= 0),
    CONSTRAINT `ck_dorm_meter_readings_price`
        CHECK (`electricity_price` > 0 AND `water_price` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 未归预警阈值配置
--
-- 设计文档要求 warningConfigSet(warnDays, notifyDays, exemptLeave)，即阈值可由
-- 宿管调整而不是写死在代码里。单行表，id 恒为 1。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_warning_configs` (
    `id` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `warn_days` INT UNSIGNED NOT NULL DEFAULT 3 COMMENT '连续未归几天开始预警',
    `notify_days` INT UNSIGNED NOT NULL DEFAULT 7 COMMENT '连续未归几天升为严重并通知辅导员',
    `exempt_on_leave` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '已批准的请假是否豁免预警',
    `updated_by` BIGINT UNSIGNED NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_dorm_warning_configs_actor` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_dorm_warning_configs_singleton` CHECK (`id` = 1),
    CONSTRAINT `ck_dorm_warning_configs_days` CHECK (`warn_days` > 0 AND `notify_days` >= `warn_days`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `dorm_warning_configs` (`id`, `warn_days`, `notify_days`, `exempt_on_leave`)
VALUES (1, 3, 7, 1);

-- ============================================================================
-- 连续未归预警
--
-- 与 V1 的 late_return_alerts 是两件事：那张表记的是「晚归」（回来了但超时），
-- 本表记的是「连续未归」（一直没回来）。语义不同，状态机也不同，因此独立建表，
-- 不改动也不复用既有表。
--
-- 每个学生每个扫描日至多一条，重复扫描按更新处理（幂等）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_absence_warnings` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `scan_date` DATE NOT NULL COMMENT '扫描日；与学生构成唯一键',
    `last_leave_at` DATETIME(3) NULL COMMENT '最后一次离宿时间',
    `absence_days` INT UNSIGNED NOT NULL COMMENT '连续未归天数',
    `warning_level` VARCHAR(16) NOT NULL COMMENT 'NORMAL 一般 / SEVERE 严重 / EXEMPT 已豁免',
    `handle_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待处理 / NOTIFIED 已通知 / VERIFIED 已核实',
    `notified_teacher_id` BIGINT UNSIGNED NULL,
    `notified_at` DATETIME(3) NULL,
    `note` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_absence_warnings_student_scan` (`student_user_id`, `scan_date`),
    KEY `idx_dorm_absence_warnings_triage` (`handle_status`, `warning_level`, `scan_date`),
    KEY `idx_dorm_absence_warnings_room` (`room_id`, `scan_date`),
    CONSTRAINT `fk_dorm_absence_warnings_student` FOREIGN KEY (`student_user_id`)
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_dorm_absence_warnings_room` FOREIGN KEY (`room_id`)
        REFERENCES `dorm_rooms` (`id`),
    -- 这里刻意不写 ON DELETE SET NULL：notified_teacher_id 同时出现在下面的
    -- ck_dorm_absence_warnings_notified 检查里，而 MySQL 禁止一个列既参与 CHECK
    -- 又带有引用动作的外键（ERROR 3823）。与 V1 里 utility_allocations 对
    -- paid_transaction_id 的处理保持一致：退回默认的 RESTRICT。
    CONSTRAINT `fk_dorm_absence_warnings_teacher` FOREIGN KEY (`notified_teacher_id`)
        REFERENCES `users` (`id`),
    CONSTRAINT `ck_dorm_absence_warnings_level`
        CHECK (`warning_level` IN ('NORMAL', 'SEVERE', 'EXEMPT')),
    CONSTRAINT `ck_dorm_absence_warnings_status`
        CHECK (`handle_status` IN ('PENDING', 'NOTIFIED', 'VERIFIED')),
    CONSTRAINT `ck_dorm_absence_warnings_notified`
        CHECK (`handle_status` <> 'NOTIFIED' OR (`notified_teacher_id` IS NOT NULL AND `notified_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 外来人员登记
--
-- 设计文档的 Registration 有四类：入住、退宿、离校、外来人员。前三类基线实现
-- 已分别落在 accommodation_requests（入住/调宿/退宿）和 leave_requests（离校）
-- 上，只有外来人员登记完全没有对应实现，本表补上。
--
-- room_id 由服务端按提交人的在住记录解析后写入，不接受客户端指定，避免学生
-- 把来访人登记到别人的房间。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_visitor_registrations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `student_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接待的学生',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '服务端按在住记录解析',
    `visitor_name` VARCHAR(60) NOT NULL,
    `visitor_id_card` VARCHAR(40) NOT NULL COMMENT '仅登记与核验用；查询接口只返回掩码',
    `visitor_phone` VARCHAR(32) NULL,
    `visit_reason` VARCHAR(500) NOT NULL,
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `audit_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `auditor_id` BIGINT UNSIGNED NULL,
    `audited_at` DATETIME(3) NULL,
    `audit_remark` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dorm_visitor_student` (`student_user_id`, `submitted_at`),
    KEY `idx_dorm_visitor_review` (`audit_status`, `start_at`),
    KEY `idx_dorm_visitor_room` (`room_id`, `start_at`),
    CONSTRAINT `fk_dorm_visitor_student` FOREIGN KEY (`student_user_id`)
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_dorm_visitor_room` FOREIGN KEY (`room_id`)
        REFERENCES `dorm_rooms` (`id`),
    -- 同 dorm_absence_warnings：auditor_id 参与下面的 CHECK，因此不能带
    -- ON DELETE 动作（MySQL ERROR 3823），退回默认的 RESTRICT。
    CONSTRAINT `fk_dorm_visitor_auditor` FOREIGN KEY (`auditor_id`)
        REFERENCES `users` (`id`),
    CONSTRAINT `ck_dorm_visitor_status`
        CHECK (`audit_status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT `ck_dorm_visitor_period` CHECK (`end_at` > `start_at`),
    CONSTRAINT `ck_dorm_visitor_audited`
        CHECK (`audit_status` IN ('PENDING', 'CANCELLED')
               OR (`auditor_id` IS NOT NULL AND `audited_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 卫生检查分项得分
--
-- V1 的 hygiene_inspections 只有一个总分，而设计文档要求五个检查项分别打分
-- （itemScores，JSON 格式）。这里没有把 JSON 塞进原表，而是用明细表：
-- 分项可以被 SQL 直接统计（比如「哪一项全楼扣分最多」），JSON 做不到。
-- 原表一列没动。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_hygiene_item_scores` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `inspection_id` BIGINT UNSIGNED NOT NULL,
    `item_code` VARCHAR(24) NOT NULL COMMENT 'FLOOR/DESK/BED/BATHROOM/BALCONY',
    `score` DECIMAL(5,2) NOT NULL COMMENT '单项 0~20',
    `deduct_reason` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_hygiene_item` (`inspection_id`, `item_code`),
    CONSTRAINT `fk_dorm_hygiene_item_inspection` FOREIGN KEY (`inspection_id`)
        REFERENCES `hygiene_inspections` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_dorm_hygiene_item_score` CHECK (`score` BETWEEN 0 AND 20),
    CONSTRAINT `ck_dorm_hygiene_item_code`
        CHECK (`item_code` IN ('FLOOR', 'DESK', 'BED', 'BATHROOM', 'BALCONY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 卫生检查任务
--
-- 对应设计文档的 hygieneTaskGenerate（每周按楼栋生成待检查清单）与
-- recheckTaskCreate（总分不合格时生成复查任务）。
-- 同一房间同一计划日期同一类型只留一条，重复生成按幂等处理。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_hygiene_tasks` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT UNSIGNED NOT NULL,
    `task_type` VARCHAR(12) NOT NULL COMMENT 'WEEKLY 周检查 / RECHECK 复查',
    `plan_date` DATE NOT NULL,
    `status` VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/SKIPPED',
    `inspection_id` BIGINT UNSIGNED NULL COMMENT '完成时回填本次检查记录',
    `source_inspection_id` BIGINT UNSIGNED NULL COMMENT '复查任务指向触发它的那次检查',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dorm_hygiene_task` (`room_id`, `plan_date`, `task_type`),
    KEY `idx_dorm_hygiene_task_queue` (`status`, `plan_date`),
    CONSTRAINT `fk_dorm_hygiene_task_room` FOREIGN KEY (`room_id`)
        REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_dorm_hygiene_task_inspection` FOREIGN KEY (`inspection_id`)
        REFERENCES `hygiene_inspections` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_dorm_hygiene_task_source` FOREIGN KEY (`source_inspection_id`)
        REFERENCES `hygiene_inspections` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_dorm_hygiene_task_type` CHECK (`task_type` IN ('WEEKLY', 'RECHECK')),
    CONSTRAINT `ck_dorm_hygiene_task_status` CHECK (`status` IN ('PENDING', 'DONE', 'SKIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 门禁策略（晚归判定用）
--
-- 设计文档在 AccessRecord 上要求一个 isLateReturn 标记。这里没有给 V1 的
-- access_records 加列：晚归与否完全由「归宿时刻」和「门禁时间」两者决定，
-- 存成列意味着门禁时间一改，历史记录的标记就与新策略对不上；改成按策略实时
-- 判定，历史记录永远自洽。本表只存那条可调的策略。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_access_policies` (
    `id` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `curfew_time` TIME NOT NULL DEFAULT '23:00:00' COMMENT '门禁时间，晚于此点归宿记晚归',
    `dawn_time` TIME NOT NULL DEFAULT '05:00:00' COMMENT '早于此点归宿同样记晚归（通宵未归）',
    `updated_by` BIGINT UNSIGNED NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_dorm_access_policies_actor` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_dorm_access_policies_singleton` CHECK (`id` = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `dorm_access_policies` (`id`) VALUES (1);

-- ============================================================================
-- 报修「不在场允许入内」许可
--
-- 设计文档 RepairOrder 上的 allowEnter。同样不给 V1 的 repair_orders 加列：
-- 这是学生对某一张工单的授权，与工单本身的处理流程解耦，旁挂一张一对一表
-- 既不影响原表，也让「谁在什么时候授的权」可以单独记录。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_repair_entry_permits` (
    `repair_order_id` BIGINT UNSIGNED NOT NULL,
    `allow_enter` TINYINT(1) NOT NULL DEFAULT 0,
    `note` VARCHAR(255) NULL,
    `updated_by` BIGINT UNSIGNED NOT NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`repair_order_id`),
    CONSTRAINT `fk_dorm_repair_permit_order` FOREIGN KEY (`repair_order_id`)
        REFERENCES `repair_orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dorm_repair_permit_actor` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 宿舍公告的类型、可见范围与置顶
--
-- 设计文档的 Notice 实体比 V1 的 announcements 多 noticeType / scopeType+scopeValue
-- / isTop 三组属性。announcements 是七个模块共用的表，给它加列会波及教务、图书馆
-- 等所有模块，所以同样走旁挂：一对一挂在公告上，只对 module_code='DORM' 的公告
-- 使用。没有扩展记录的旧公告读出来即默认值（普通类型 / 全体可见 / 不置顶），
-- main 已发布的公告不需要任何数据迁移。
--
-- 注意 scope_building_id 与 scope_room_id 同时出现在 CHECK 和外键里：MySQL 8
-- 不允许这样的列再带 ON DELETE 动作（ERROR 3823），因此这两个外键只写默认的
-- RESTRICT，与 V1 里 announcements 对 roles 的处理保持一致。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `dorm_notice_extras` (
    `announcement_id` BIGINT UNSIGNED NOT NULL,
    `notice_type` VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    `scope_type` VARCHAR(16) NOT NULL DEFAULT 'ALL',
    `scope_building_id` BIGINT UNSIGNED NULL,
    `scope_room_id` BIGINT UNSIGNED NULL,
    `pinned` TINYINT(1) NOT NULL DEFAULT 0,
    `pinned_at` DATETIME(3) NULL COMMENT '置顶时刻，多条置顶时按此倒序',
    `updated_by` BIGINT UNSIGNED NOT NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`announcement_id`),
    KEY `idx_dorm_notice_extras_pinned` (`pinned`, `pinned_at`),
    KEY `idx_dorm_notice_extras_scope` (`scope_type`, `scope_building_id`, `scope_room_id`),
    CONSTRAINT `fk_dorm_notice_extras_announcement` FOREIGN KEY (`announcement_id`)
        REFERENCES `announcements` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dorm_notice_extras_building` FOREIGN KEY (`scope_building_id`)
        REFERENCES `dorm_buildings` (`id`),
    CONSTRAINT `fk_dorm_notice_extras_room` FOREIGN KEY (`scope_room_id`)
        REFERENCES `dorm_rooms` (`id`),
    CONSTRAINT `fk_dorm_notice_extras_actor` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`),
    CONSTRAINT `ck_dorm_notice_extras_type` CHECK (`notice_type` IN
        ('GENERAL', 'MAINTENANCE', 'HYGIENE', 'SAFETY', 'URGENT')),
    CONSTRAINT `ck_dorm_notice_extras_scope` CHECK (`scope_type` IN
        ('ALL', 'BUILDING', 'ROOM')),
    CONSTRAINT `ck_dorm_notice_extras_scope_target` CHECK (
        (`scope_type` = 'ALL' AND `scope_building_id` IS NULL AND `scope_room_id` IS NULL)
     OR (`scope_type` = 'BUILDING' AND `scope_building_id` IS NOT NULL AND `scope_room_id` IS NULL)
     OR (`scope_type` = 'ROOM' AND `scope_room_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
