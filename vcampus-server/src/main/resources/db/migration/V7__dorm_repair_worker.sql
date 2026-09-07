-- ============================================================================
-- V7 维修员角色
--
-- 报修工单本身在 V1 就已经齐了：repair_orders 有 handler_id、accepted_at、
-- completed_at 和 SUBMITTED/ACCEPTED/IN_PROGRESS/COMPLETED 四态。缺的只是「谁来
-- 当处理人」——V1 没有维修员这个角色，工单只能派给宿管自己。所以这个迁移不建表，
-- 只补角色、权限和一个演示账号。
--
-- 新建 dorm_repair_workers 之类的档案表被刻意放弃了：维修员的身份就是 users 上挂
-- 一个角色，工种、班次这些属性目前没有任何功能会读，建出来就是一张空表。
-- ============================================================================

INSERT INTO `roles` (`code`, `display_name`, `description`)
VALUES ('REPAIR_WORKER', '维修员', '接单、上报维修进度并查看入内授权')
AS new
ON DUPLICATE KEY UPDATE
    `display_name` = new.display_name,
    `description` = new.description;

INSERT INTO `permissions` (`code`, `display_name`, `description`)
VALUES ('DORM_REPAIR_WORK', '宿舍报修处理', '接单、推进和完成宿舍报修工单')
AS new
ON DUPLICATE KEY UPDATE
    `display_name` = new.display_name,
    `description` = new.description;

-- 维修员只拿这一条宿舍权限：他要进学生宿舍干活，但不该顺带看到住宿名册、
-- 水电账单和卫生检查。派单需要的房间号和联系方式随工单一起下发。
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
  ON p.`code` IN ('PROFILE_READ', 'PROFILE_UPDATE', 'DORM_REPAIR_WORK')
WHERE r.`code` = 'REPAIR_WORKER'
ON DUPLICATE KEY UPDATE `granted_at` = `granted_at`;

-- 演示维修员账号；口令与其他 demo 账号同一套开发约定：repair123
INSERT INTO `users` (`username`, `password_hash`, `display_name`, `email`, `status`)
VALUES ('demo_repair', '$2a$10$nzRizN//GQ1wCYMq23.O4OdHV6kHrOCSKOa3t3QIpPyZXW5h.xhFm',
        '演示维修员', 'demo.repair@vcampus.local', 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE
    `password_hash` = new.password_hash,
    `display_name` = new.display_name,
    `email` = new.email;

SET @repair_worker_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_repair');
SET @granting_admin_id = (SELECT `id` FROM `users` WHERE `username` = 'demo_system');

INSERT INTO `user_roles` (`user_id`, `role_id`, `assigned_by`)
SELECT @repair_worker_id, `id`, @granting_admin_id FROM `roles` WHERE `code` = 'REPAIR_WORKER'
ON DUPLICATE KEY UPDATE `assigned_by` = @granting_admin_id;

-- 演示学生留个电话，否则维修员端「联系电话」一列永远是空的，看不出这个字段有没有接通。
-- NOT EXISTS 套一层派生表是 MySQL 的写法要求：不能在 UPDATE 的子查询里直接引用目标表。
-- 加这层是因为 phone 上有唯一约束，号码万一已被占用，这条语句会让整个迁移中途报错。
UPDATE `users` SET `phone` = '13900000001'
WHERE `username` = 'demo_student'
  AND (`phone` IS NULL OR `phone` = '')
  AND NOT EXISTS (SELECT 1 FROM (SELECT `id` FROM `users` WHERE `phone` = '13900000001') AS taken);
