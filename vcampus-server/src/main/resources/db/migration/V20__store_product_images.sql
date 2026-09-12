-- 商品图片与商品一起保存；列表只传版本引用，图片通过受控命令按需读取。
SET NAMES utf8mb4;
USE vcampus;
SET @vcampus_image_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='products' AND column_name='image_data');
SET @vcampus_image_sql = IF(@vcampus_image_exists=0,
    'ALTER TABLE products ADD COLUMN image_data MEDIUMBLOB NULL', 'SELECT 1');
PREPARE vc_image_stmt FROM @vcampus_image_sql;
EXECUTE vc_image_stmt;
DEALLOCATE PREPARE vc_image_stmt;
