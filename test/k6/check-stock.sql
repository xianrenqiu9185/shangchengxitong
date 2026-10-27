-- ============================================================
-- 邻里鲜生 · 库存一致性核对
-- 用法：
--   docker exec -i linli-mysql mysql -uroot -proot123456 linli_fresh < test/k6/check-stock.sql
-- 判定：两条查询都返回空结果集 = 通过
-- 依据：设计文档 ADR-003，库存恒等式 总 = 可用 + 锁定 + 已售
-- ============================================================

SELECT 'negative_or_inconsistent' AS check_name,
       product_id,
       total,
       locked,
       sold,
       (total - locked - sold) AS available
FROM t_inventory
WHERE total < 0
   OR locked < 0
   OR sold < 0
   OR (total - locked - sold) < 0;

SELECT 'locked_exceeds_paid' AS check_name,
       i.product_id,
       i.locked,
       i.sold
FROM t_inventory i
WHERE i.sold > i.total;
