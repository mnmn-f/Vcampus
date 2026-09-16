-- 让教师端拥有与学生端一致的图书借还和自习室预约权限。
-- 本脚本可重复执行，适用于已经导入过 V2__demo_data.sql 的数据库。
SET NAMES utf8mb4;
USE `vcampus`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p
  ON p.`code` IN ('LIBRARY_READ', 'LIBRARY_BORROW', 'STUDY_ROOM_RESERVE')
WHERE r.`code` = 'TEACHER'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;
