USE `vcampus`;

-- 将旧版随机商品编码换成与现有商品一致的“SEU-分类-序号”格式。
UPDATE `products` p
LEFT JOIN `products` clash
  ON clash.`sku` = CONCAT(
      'SEU-',
      UPPER(COALESCE(NULLIF(p.`category_code`, ''), NULLIF(p.`category`, ''), 'OTHER')),
      '-', LPAD(p.`id`, 3, '0'))
 AND clash.`id` <> p.`id`
SET p.`sku` = CONCAT(
    'SEU-',
    UPPER(COALESCE(NULLIF(p.`category_code`, ''), NULLIF(p.`category`, ''), 'OTHER')),
    '-', LPAD(p.`id`, 3, '0'))
WHERE p.`sku` REGEXP '^VC-[0-9A-F]{32}$'
  AND clash.`id` IS NULL;
