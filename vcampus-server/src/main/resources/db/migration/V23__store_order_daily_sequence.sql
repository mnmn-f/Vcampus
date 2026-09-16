-- 商店订单号使用 VC-yyyyMMdd-NNNN；此表只分配每天的并发安全流水号。
CREATE TABLE IF NOT EXISTS `store_order_daily_sequences` (
    `order_date` DATE NOT NULL,
    `last_sequence` INT UNSIGNED NOT NULL,
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`order_date`),
    CONSTRAINT `ck_store_order_daily_sequence`
        CHECK (`last_sequence` BETWEEN 1 AND 9999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
