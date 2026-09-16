-- 宿舍演示数据修补（2026-09-13）
--
-- 1. demo_student 的手机号被改成了乱码，维修员端「联系电话」一列跟着显示乱码，换回正常的演示号码。
-- 2. 房间床位重复：早先 dorm-demo-data.sql 给每间房建了 A/B/C/D 四张床，后来 dorm-showcase-data.sql
--    只把 101 的 A~D 改名成 1~4，其余房间又补了一套 1~4，于是 102/103/201/D2-101/D2-102 各有 8 张床，
--    平面图只画四人寝，这些房间就画不出来。这里按 A→1、B→2、C→3、D→4 合并成一套：
--      - 字母床没被任何住宿记录/申请引用 → 直接删掉字母床；
--      - 字母床有引用、对应数字床没引用 → 删掉数字床，把字母床改名成数字；
--      - 两边都有引用 → 两张都保留，最后一段查询会把这种房间列出来，人工处理。
--
-- 可重复执行；只动 DEMO-D1 / DEMO-D2 两栋演示楼。
--   mysql> source D:/Vcampus/scripts/fix-dorm-data-20260913.sql

SET NAMES utf8mb4;
USE vcampus;

-- ---------------------------------------------------------------------------
-- 1. 演示学生手机号
-- ---------------------------------------------------------------------------
UPDATE `users` SET `phone` = '13812340001'
WHERE `username` = 'demo_student'
  AND NOT EXISTS (SELECT 1 FROM (SELECT `id` FROM `users` WHERE `phone` = '13812340001' AND `username` <> 'demo_student') AS taken);

-- ---------------------------------------------------------------------------
-- 2. 合并重复床位
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `fix_bed_pairs`;
CREATE TEMPORARY TABLE `fix_bed_pairs` (
    `room_id`     BIGINT UNSIGNED NOT NULL,
    `letter_id`   BIGINT UNSIGNED NOT NULL,
    `letter_no`   VARCHAR(20) NOT NULL,
    `number_id`   BIGINT UNSIGNED NULL,
    `number_no`   VARCHAR(20) NOT NULL,
    `letter_refs` INT NOT NULL DEFAULT 0,
    `number_refs` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`letter_id`)
);

INSERT INTO `fix_bed_pairs` (`room_id`, `letter_id`, `letter_no`, `number_id`, `number_no`)
SELECT l.`room_id`, l.`id`, l.`bed_no`, n.`id`,
       CASE l.`bed_no` WHEN 'A' THEN '1' WHEN 'B' THEN '2' WHEN 'C' THEN '3' ELSE '4' END
FROM `dorm_beds` l
JOIN `dorm_rooms` r ON r.`id` = l.`room_id`
JOIN `dorm_buildings` b ON b.`id` = r.`building_id`
LEFT JOIN `dorm_beds` n ON n.`room_id` = l.`room_id`
     AND n.`bed_no` = CASE l.`bed_no` WHEN 'A' THEN '1' WHEN 'B' THEN '2' WHEN 'C' THEN '3' ELSE '4' END
WHERE b.`building_code` IN ('DEMO-D1', 'DEMO-D2')
  AND l.`bed_no` IN ('A', 'B', 'C', 'D');

UPDATE `fix_bed_pairs` p
SET `letter_refs` = (SELECT COUNT(*) FROM `accommodation_records` ar WHERE ar.`bed_id` = p.`letter_id`)
                  + (SELECT COUNT(*) FROM `accommodation_requests` aq WHERE aq.`requested_bed_id` = p.`letter_id`),
    `number_refs` = CASE WHEN p.`number_id` IS NULL THEN 0 ELSE
                    (SELECT COUNT(*) FROM `accommodation_records` ar WHERE ar.`bed_id` = p.`number_id`)
                  + (SELECT COUNT(*) FROM `accommodation_requests` aq WHERE aq.`requested_bed_id` = p.`number_id`) END;

-- 2a. 字母床没人引用：删掉字母床（数字床已经在，或者后面 2c 会补）
DELETE d FROM `dorm_beds` d
JOIN `fix_bed_pairs` p ON p.`letter_id` = d.`id`
WHERE p.`letter_refs` = 0 AND p.`number_id` IS NOT NULL;

-- 2b. 字母床有引用、数字床没引用：删数字床，再把字母床改名
DELETE d FROM `dorm_beds` d
JOIN `fix_bed_pairs` p ON p.`number_id` = d.`id`
WHERE p.`letter_refs` > 0 AND p.`number_refs` = 0;

UPDATE `dorm_beds` d
JOIN `fix_bed_pairs` p ON p.`letter_id` = d.`id`
SET d.`bed_no` = p.`number_no`
WHERE p.`letter_refs` > 0 AND p.`number_refs` = 0;

-- 2c. 房间里只有字母床、没有数字床（没跑过 showcase 的库）：直接改名
UPDATE `dorm_beds` d
JOIN `fix_bed_pairs` p ON p.`letter_id` = d.`id`
SET d.`bed_no` = p.`number_no`
WHERE p.`number_id` IS NULL;

-- 2d. 两边都有引用的，列出来人工看（正常情况下这里应该是空的）
SELECT b.`building_code`, r.`room_no`, p.`letter_no`, p.`letter_refs`, p.`number_no`, p.`number_refs`
FROM `fix_bed_pairs` p
JOIN `dorm_rooms` r ON r.`id` = p.`room_id`
JOIN `dorm_buildings` b ON b.`id` = r.`building_id`
WHERE p.`letter_refs` > 0 AND p.`number_refs` > 0;

DROP TEMPORARY TABLE IF EXISTS `fix_bed_pairs`;

-- 结果核对：每间演示房现在应该正好 4 张床
SELECT b.`building_code`, r.`room_no`, COUNT(d.`id`) AS beds,
       GROUP_CONCAT(d.`bed_no` ORDER BY d.`bed_no`) AS bed_nos
FROM `dorm_rooms` r
JOIN `dorm_buildings` b ON b.`id` = r.`building_id`
LEFT JOIN `dorm_beds` d ON d.`room_id` = r.`id`
WHERE b.`building_code` IN ('DEMO-D1', 'DEMO-D2')
GROUP BY b.`building_code`, r.`room_no`
ORDER BY b.`building_code`, r.`room_no`;

SELECT `username`, `phone` FROM `users` WHERE `username` = 'demo_student';
