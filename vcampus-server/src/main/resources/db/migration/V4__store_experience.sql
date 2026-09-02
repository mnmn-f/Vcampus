-- 商店体验增强：图片/评分、稳定分类、促销优惠券、评价和好友代付。
-- 在 V1/V2 之后执行；所有金额继续由服务端事务计算。
USE `vcampus`;

ALTER TABLE `products`
    ADD COLUMN IF NOT EXISTS `image_url` VARCHAR(1000) NULL AFTER `status`,
    ADD COLUMN IF NOT EXISTS `rating_average` DECIMAL(4,2) NOT NULL DEFAULT 0.00 AFTER `image_url`,
    ADD COLUMN IF NOT EXISTS `rating_count` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `rating_average`,
    ADD COLUMN IF NOT EXISTS `category_code` VARCHAR(40) NULL AFTER `category`;

SET @vcampus_category_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'products'
      AND index_name = 'idx_products_category_code'
);
SET @vcampus_category_index_sql = IF(@vcampus_category_index_exists = 0,
    'CREATE INDEX idx_products_category_code ON products (category_code)',
    'SELECT 1');
PREPARE vc_category_index_stmt FROM @vcampus_category_index_sql;
EXECUTE vc_category_index_stmt;
DEALLOCATE PREPARE vc_category_index_stmt;

CREATE TABLE IF NOT EXISTS `store_categories` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(40) NOT NULL,
    `name` VARCHAR(80) NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_categories_code` (`code`),
    CONSTRAINT `ck_store_categories_code` CHECK (CHAR_LENGTH(TRIM(`code`)) > 0),
    CONSTRAINT `ck_store_categories_name` CHECK (CHAR_LENGTH(TRIM(`name`)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_promotions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(64) NOT NULL,
    `name` VARCHAR(120) NOT NULL,
    `promotion_type` VARCHAR(16) NOT NULL,
    `threshold_amount` DECIMAL(12,2) NULL,
    `discount_value` DECIMAL(12,2) NOT NULL,
    `product_scope` VARCHAR(16) NOT NULL DEFAULT 'ALL',
    `product_id` BIGINT UNSIGNED NULL,
    `category_code` VARCHAR(40) NULL,
    `starts_at` DATETIME(3) NOT NULL,
    `ends_at` DATETIME(3) NULL,
    `stackable` BOOLEAN NOT NULL DEFAULT FALSE,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_promotions_code` (`code`),
    KEY `idx_store_promotions_active_time` (`active`,`starts_at`,`ends_at`),
    CONSTRAINT `fk_store_promotions_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
    CONSTRAINT `fk_store_promotions_category` FOREIGN KEY (`category_code`) REFERENCES `store_categories` (`code`),
    CONSTRAINT `ck_store_promotions_type` CHECK (`promotion_type` IN ('THRESHOLD','PERCENT','FIXED')),
    CONSTRAINT `ck_store_promotions_scope` CHECK (`product_scope` IN ('ALL','PRODUCT','CATEGORY')),
    CONSTRAINT `ck_store_promotions_threshold` CHECK (`threshold_amount` IS NULL OR `threshold_amount` >= 0),
    CONSTRAINT `ck_store_promotions_value` CHECK (`discount_value` > 0),
    CONSTRAINT `ck_store_promotions_scope_target` CHECK (
        (`product_scope` = 'ALL' AND `product_id` IS NULL AND `category_code` IS NULL)
        OR (`product_scope` = 'PRODUCT' AND `product_id` IS NOT NULL AND `category_code` IS NULL)
        OR (`product_scope` = 'CATEGORY' AND `product_id` IS NULL AND `category_code` IS NOT NULL)),
    CONSTRAINT `ck_store_promotions_percent` CHECK (`promotion_type` <> 'PERCENT' OR `discount_value` <= 100),
    CONSTRAINT `ck_store_promotions_dates` CHECK (`ends_at` IS NULL OR `ends_at` > `starts_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_coupons` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(64) NOT NULL,
    `name` VARCHAR(120) NOT NULL,
    `threshold_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `discount_amount` DECIMAL(12,2) NOT NULL,
    `expires_at` DATETIME(3) NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_coupons_code` (`code`),
    CONSTRAINT `ck_store_coupons_amount` CHECK (`threshold_amount` >= 0 AND `discount_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_user_coupons` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `coupon_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `used_at` DATETIME(3) NULL,
    `order_id` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_user_coupon` (`coupon_id`,`user_id`),
    KEY `idx_store_user_coupon_user` (`user_id`,`used_at`),
    CONSTRAINT `fk_store_user_coupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `store_coupons` (`id`),
    CONSTRAINT `fk_store_user_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_store_user_coupon_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_product_reviews` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `score` TINYINT UNSIGNED NOT NULL,
    `content` VARCHAR(1000) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_review_order_product` (`order_id`,`product_id`),
    KEY `idx_store_review_product_time` (`product_id`,`created_at`),
    CONSTRAINT `fk_store_review_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
    CONSTRAINT `fk_store_review_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`),
    CONSTRAINT `fk_store_review_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_store_review_score` CHECK (`score` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `store_friend_payments` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `buyer_id` BIGINT UNSIGNED NOT NULL,
    `payer_id` BIGINT UNSIGNED NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `message` VARCHAR(500) NULL,
    `expires_at` DATETIME(3) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `decided_at` DATETIME(3) NULL,
    `pending_order_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `status` = 'PENDING' THEN `order_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_store_friend_payment_order_pending` (`pending_order_id`),
    KEY `idx_store_friend_payment_payer_status` (`payer_id`,`status`,`created_at`),
    KEY `idx_store_friend_payment_buyer_status` (`buyer_id`,`status`,`created_at`),
    CONSTRAINT `fk_store_friend_payment_order` FOREIGN KEY (`order_id`) REFERENCES `store_orders` (`id`),
    CONSTRAINT `fk_store_friend_payment_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_store_friend_payment_payer` FOREIGN KEY (`payer_id`) REFERENCES `users` (`id`),
    CONSTRAINT `ck_store_friend_payment_users` CHECK (`buyer_id` <> `payer_id`),
    CONSTRAINT `ck_store_friend_payment_status` CHECK (`status` IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN','EXPIRED')),
    CONSTRAINT `ck_store_friend_payment_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `store_categories` (`code`,`name`,`active`) VALUES
    ('DAILY','日用百货',TRUE), ('FOOD','食品饮料',TRUE), ('STATIONERY','文具用品',TRUE),
    ('CULTURE','校园文创',TRUE), ('OTHER','其他',TRUE)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `active`=VALUES(`active`);

UPDATE `products`
SET `category_code` = CASE TRIM(`category`)
    WHEN '文创' THEN 'CULTURE'
    WHEN '日用' THEN 'DAILY'
    WHEN '日用品' THEN 'DAILY'
    WHEN '食品' THEN 'FOOD'
    WHEN '饮料' THEN 'FOOD'
    WHEN '文具' THEN 'STATIONERY'
    ELSE 'OTHER' END
WHERE `category_code` IS NULL;

SET @vcampus_category_fk_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'products'
      AND constraint_name = 'fk_products_category_code'
);
SET @vcampus_category_fk_sql = IF(@vcampus_category_fk_exists = 0,
    'ALTER TABLE products ADD CONSTRAINT fk_products_category_code FOREIGN KEY (category_code) REFERENCES store_categories(code)',
    'SELECT 1');
PREPARE vc_category_fk_stmt FROM @vcampus_category_fk_sql;
EXECUTE vc_category_fk_stmt;
DEALLOCATE PREPARE vc_category_fk_stmt;

INSERT INTO `store_coupons` (`code`,`name`,`threshold_amount`,`discount_amount`,`expires_at`,`active`)
VALUES ('WELCOME10','新生优惠券',50.00,10.00,CURRENT_TIMESTAMP(3)+INTERVAL 90 DAY,TRUE)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `active`=VALUES(`active`);

ALTER TABLE `store_orders`
    ADD COLUMN IF NOT EXISTS `original_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER `total_amount`,
    ADD COLUMN IF NOT EXISTS `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER `original_amount`,
    ADD COLUMN IF NOT EXISTS `promotion_code` VARCHAR(64) NULL AFTER `discount_amount`,
    ADD COLUMN IF NOT EXISTS `coupon_code` VARCHAR(64) NULL AFTER `promotion_code`,
    ADD COLUMN IF NOT EXISTS `payment_mode` VARCHAR(16) NOT NULL DEFAULT 'SELF' AFTER `coupon_code`;

SET @vcampus_payment_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'store_orders'
      AND constraint_name = 'ck_store_orders_payment_mode'
);
SET @vcampus_payment_check_sql = IF(@vcampus_payment_check_exists = 0,
    'ALTER TABLE store_orders ADD CONSTRAINT ck_store_orders_payment_mode CHECK (payment_mode IN (''SELF'',''FRIEND''))',
    'SELECT 1');
PREPARE vc_payment_check_stmt FROM @vcampus_payment_check_sql;
EXECUTE vc_payment_check_stmt;
DEALLOCATE PREPARE vc_payment_check_stmt;

UPDATE `store_orders` SET `original_amount`=`total_amount`
WHERE `original_amount`=0 AND `total_amount`>0;

SET @vcampus_order_price_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'store_orders'
      AND constraint_name = 'ck_store_orders_price_snapshot'
);
SET @vcampus_order_price_check_sql = IF(@vcampus_order_price_check_exists = 0,
    'ALTER TABLE store_orders ADD CONSTRAINT ck_store_orders_price_snapshot CHECK (original_amount >= 0 AND discount_amount >= 0 AND discount_amount <= original_amount AND total_amount = original_amount - discount_amount)',
    'SELECT 1');
PREPARE vc_order_price_check_stmt FROM @vcampus_order_price_check_sql;
EXECUTE vc_order_price_check_stmt;
DEALLOCATE PREPARE vc_order_price_check_stmt;
