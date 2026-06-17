-- 同步 SKU 图片 → SPU 主图
-- 版本：V1.0.5

UPDATE `mall_product_sku` s
JOIN `mall_product_spu` p ON s.`spu_id` = p.`id`
SET s.`image` = p.`main_image`
WHERE s.`image` LIKE 'https://cdn.example.com/sku/%';
