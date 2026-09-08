USE `vcampus`;

ALTER TABLE `store_orders`
  ADD COLUMN `shipping_status` VARCHAR(32) NULL AFTER `status`,
  ADD COLUMN `tracking_no` VARCHAR(80) NULL AFTER `shipping_status`,
  ADD COLUMN `shipping_remark` VARCHAR(500) NULL AFTER `tracking_no`,
  ADD CONSTRAINT `ck_store_orders_shipping_status` CHECK
    (`shipping_status` IS NULL OR `shipping_status` IN
      ('PREPARING','SHIPPED','IN_TRANSIT','READY_FOR_PICKUP','DELIVERED'));
