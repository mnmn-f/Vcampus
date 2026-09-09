-- 头像由客户端选择本地图片并压缩为 data URI，替代要求用户填写网络地址。
SET NAMES utf8mb4;

SET @vcampus_avatar_type = (
    SELECT DATA_TYPE FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users'
      AND column_name = 'avatar_url' LIMIT 1
);
SET @vcampus_avatar_sql = IF(@vcampus_avatar_type <> 'mediumtext',
    'ALTER TABLE users MODIFY COLUMN avatar_url MEDIUMTEXT NULL', 'SELECT 1');
PREPARE vc_avatar_stmt FROM @vcampus_avatar_sql;
EXECUTE vc_avatar_stmt;
DEALLOCATE PREPARE vc_avatar_stmt;
