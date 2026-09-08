USE `vcampus`;

-- 仅续期随演示数据提供且已经过期的欢迎券；不影响教师自行创建的优惠券。
UPDATE `store_coupons`
SET `expires_at` = CURRENT_TIMESTAMP(3) + INTERVAL 90 DAY,
    `active` = TRUE
WHERE `code` = 'WELCOME10'
  AND `expires_at` <= CURRENT_TIMESTAMP(3);
