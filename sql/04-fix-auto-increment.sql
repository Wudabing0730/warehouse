-- ============================================================
-- 修复 AUTO_INCREMENT 跳号问题
--
-- 根因: @Transactional 方法在 baseMapper.insert() 之后
--       抛出异常(如 SecurityUtils.getCurrentUserId() 的
--       NumberFormatException),导致事务回滚,但 MySQL InnoDB
--       的 AUTO_INCREMENT 计数器不会因回滚而减小。
--       多次试错后计数器远超实际数据量。
--
-- 修复: 将所有 19 张表的 AUTO_INCREMENT 重置为 1,
--       MySQL 会自动取 MAX(MAX(id)+1, N),确保下一个
--       插入的 ID 紧跟当前最大值。
--
-- 用法: mysql -u root -p warehouse < 04-fix-auto-increment.sql
-- ============================================================

USE warehouse;

ALTER TABLE t_role AUTO_INCREMENT = 1;
ALTER TABLE t_permission AUTO_INCREMENT = 1;
ALTER TABLE t_role_permission AUTO_INCREMENT = 1;
ALTER TABLE t_user AUTO_INCREMENT = 1;
ALTER TABLE t_user_role AUTO_INCREMENT = 1;
ALTER TABLE t_operation_log AUTO_INCREMENT = 1;
ALTER TABLE t_product_category AUTO_INCREMENT = 1;
ALTER TABLE t_warehouse_location AUTO_INCREMENT = 1;
ALTER TABLE t_product AUTO_INCREMENT = 1;
ALTER TABLE t_stock AUTO_INCREMENT = 1;
ALTER TABLE t_supplier AUTO_INCREMENT = 1;
ALTER TABLE t_customer AUTO_INCREMENT = 1;
ALTER TABLE t_inbound_order AUTO_INCREMENT = 1;
ALTER TABLE t_inbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE t_outbound_order AUTO_INCREMENT = 1;
ALTER TABLE t_outbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE t_borrow_record AUTO_INCREMENT = 1;
ALTER TABLE t_inventory_check AUTO_INCREMENT = 1;
ALTER TABLE t_refresh_token AUTO_INCREMENT = 1;

-- 验证: 查看 AUTO_INCREMENT 是否已重置
SELECT
    TABLE_NAME,
    AUTO_INCREMENT,
    TABLE_ROWS
FROM information_schema.tables
WHERE TABLE_SCHEMA = 'warehouse'
  AND TABLE_NAME IN (
    't_role','t_permission','t_role_permission','t_user','t_user_role',
    't_operation_log','t_product_category','t_warehouse_location','t_product',
    't_stock','t_supplier','t_customer','t_inbound_order',
    't_inbound_order_detail','t_outbound_order','t_outbound_order_detail',
    't_borrow_record','t_inventory_check','t_refresh_token'
  )
ORDER BY TABLE_NAME;