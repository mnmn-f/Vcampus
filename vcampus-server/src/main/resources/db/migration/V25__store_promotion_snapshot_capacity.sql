-- 一个订单可叠加多个促销；价格快照必须容纳所有实际生效的促销编码。
ALTER TABLE `store_orders`
    MODIFY COLUMN `promotion_code` TEXT NULL;
