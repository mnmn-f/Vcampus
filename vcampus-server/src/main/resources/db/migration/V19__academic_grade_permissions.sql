USE vcampus;

INSERT INTO permissions (`code`, `display_name`, `description`)
VALUES ('SCORE_AUDIT', '核对成绩', '教务管理员核对课程成绩')
ON DUPLICATE KEY UPDATE
    `display_name` = VALUES(`display_name`),
    `description` = VALUES(`description`);

DELETE rp
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.`code` = 'REGISTRAR' AND p.`code` = 'SCORE_RECORD';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.`code` = 'SCORE_AUDIT'
WHERE r.`code` = 'ACADEMIC_ADMIN';
