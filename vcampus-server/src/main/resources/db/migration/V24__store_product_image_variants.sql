-- 商品图片独立存储：列表读取缩略图，详情点击后才读取原图。
CREATE TABLE IF NOT EXISTS `store_product_images` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `reference` VARCHAR(64) NOT NULL,
    `original_data` MEDIUMBLOB NOT NULL,
    `thumbnail_data` MEDIUMBLOB NULL,
    `is_primary` TINYINT(1) NOT NULL DEFAULT 1,
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_product_images_reference` (`reference`),
    KEY `idx_store_product_images_product` (`product_id`, `is_primary`, `sort_order`),
    CONSTRAINT `fk_store_product_images_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_store_product_images_reference`
        CHECK (`reference` REGEXP '^store-image:[0-9A-Fa-f-]{36}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `store_product_images`
    (`product_id`, `reference`, `original_data`, `is_primary`, `sort_order`)
SELECT `id`, `image_url`, `image_data`, 1, 0
FROM `products`
WHERE `image_url` REGEXP '^store-image:[0-9A-Fa-f-]{36}$'
  AND `image_data` IS NOT NULL;
