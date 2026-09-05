-- ============================================================================
-- 宿舍模块约束补齐（可重复执行）
--
-- V7 和 V8 改的都是 CHECK 约束，而 ALTER ... DROP CHECK 在约束不存在时会直接报错，
-- 所以那两个文件只能干干净净地跑一次。这个脚本先查 information_schema 再决定要不要
-- 删，跑几遍结果都一样，用来确认库里的约束和当前代码对得上。
--
-- 用法（在 mysql 客户端里）：
--     source D:/Vcampus/scripts/apply-dorm-migrations.sql
-- ============================================================================

-- Windows 的 mysql 客户端默认按控制台代码页（通常是 GBK）解释文件里的字节，
-- 而这个文件是 UTF-8，不声明的话所有中文都会变成乱码或直接报 1366/1267。
-- 放在最前面，后面所有语句都按 UTF-8 送给服务端。
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- V7：报修工单要能进入「待宿管审核」
-- 维修员点「报完工」写的就是 PENDING_REVIEW；V1 的约束里没有这个取值，
-- 库里没跑过 V7 的话，这一步会被数据库拒掉，界面上只会看到「宿舍服务暂时不可用」。
-- ---------------------------------------------------------------------------
SET @drop_status = (SELECT IF(EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'repair_orders'
          AND CONSTRAINT_NAME = 'ck_repair_orders_status'),
    'ALTER TABLE `repair_orders` DROP CHECK `ck_repair_orders_status`',
    'DO 0'));
PREPARE s FROM @drop_status; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE `repair_orders` ADD CONSTRAINT `ck_repair_orders_status`
    CHECK (`status` IN ('SUBMITTED', 'ACCEPTED', 'IN_PROGRESS', 'PENDING_REVIEW',
                        'COMPLETED', 'CANCELLED'));

UPDATE `dorm_buildings` SET `gender_policy` = 'FEMALE' WHERE `gender_policy` = 'MIXED';

-- ---------------------------------------------------------------------------
-- V8：住宿申请不再要求学生自己填目标床位
-- 床位改由宿管在审批时指定，所以待审批期间 requested_bed_id 就是空的。
-- ---------------------------------------------------------------------------
SET @drop_bed = (SELECT IF(EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'accommodation_requests'
          AND CONSTRAINT_NAME = 'ck_accommodation_requests_bed'),
    'ALTER TABLE `accommodation_requests` DROP CHECK `ck_accommodation_requests_bed`',
    'DO 0'));
PREPARE s FROM @drop_bed; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE `accommodation_requests` ADD CONSTRAINT `ck_accommodation_requests_bed`
    CHECK (`request_type` <> 'CHECK_OUT' OR `requested_bed_id` IS NULL);

-- ---------------------------------------------------------------------------
-- 跑完对一眼：两行 definition 里应该分别看得到 PENDING_REVIEW 和 CHECK_OUT。
-- ---------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME IN ('ck_repair_orders_status', 'ck_accommodation_requests_bed');
