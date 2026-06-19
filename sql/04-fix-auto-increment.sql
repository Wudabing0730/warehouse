-- ============================================================
-- 修复 AUTO_INCREMENT 跳号问题
--
-- 根因: @Transactional 方法在 baseMapper.insert() 之后
--       抛出异常(如 SecurityUtils.getCurrentUserId() 的
--       NumberFormatException),导致事务回滚,但 MySQL InnoDB
--       的 AUTO_INCREMENT 计数器不会因回滚而减小。
--       多次试错后计数器远超实际数据量。
--
-- 重要: MySQL 的 ALTER TABLE ... AUTO_INCREMENT = N
--       会受到 InnoDB 数据字典缓存影响,设置后需要
--       执行 ANALYZE TABLE 刷新元数据缓存才能看到正确值。
--       对于已被重命名过的表,可能需要使用重建表的方式重置。
--
-- 用法: mysql -u root -p warehouse < 04-fix-auto-increment.sql
-- ============================================================

USE warehouse;

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE t_role AUTO_INCREMENT = 1;
ALTER TABLE t_user AUTO_INCREMENT = 1;
ALTER TABLE t_permission AUTO_INCREMENT = 1;
ALTER TABLE t_product AUTO_INCREMENT = 1;
ALTER TABLE t_product_category AUTO_INCREMENT = 1;
ALTER TABLE t_supplier AUTO_INCREMENT = 1;
ALTER TABLE t_customer AUTO_INCREMENT = 1;
ALTER TABLE t_warehouse_location AUTO_INCREMENT = 1;
ALTER TABLE t_stock AUTO_INCREMENT = 1;
ALTER TABLE t_role_permission AUTO_INCREMENT = 1;
ALTER TABLE t_user_role AUTO_INCREMENT = 1;
ALTER TABLE t_refresh_token AUTO_INCREMENT = 1;
ALTER TABLE t_inbound_order AUTO_INCREMENT = 1;
ALTER TABLE t_inbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE t_outbound_order AUTO_INCREMENT = 1;
ALTER TABLE t_outbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE t_borrow_record AUTO_INCREMENT = 1;
ALTER TABLE t_inventory_check AUTO_INCREMENT = 1;
ALTER TABLE t_operation_log AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- 强制刷新元数据缓存,确保 AUTO_INCREMENT 显示正确
ANALYZE TABLE t_role, t_user, t_permission, t_product, t_product_category,
               t_supplier, t_customer, t_warehouse_location, t_stock,
               t_role_permission, t_user_role, t_refresh_token,
               t_inbound_order, t_inbound_order_detail,
               t_outbound_order, t_outbound_order_detail,
               t_borrow_record, t_inventory_check, t_operation_log;

-- 验证结果(手动对比 MAX(id) 和 AUTO_INCREMENT)
-- SELECT 't_role' AS tbl, COUNT(*) AS cnt, MAX(role_id) AS max_id FROM t_role
-- UNION ALL SELECT 't_user', COUNT(*), MAX(user_id) FROM t_user
-- ...