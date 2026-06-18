-- ============================================================
-- 仓库管理系统 - 演示/虚拟数据
-- 覆盖所有业务表，提供完整的功能演示数据
-- ============================================================
-- 密码说明：
--   admin / admin123 (预置管理员, 02-seed.sql 已包含)
--   以下新增用户的密码均为: test123
--   BCrypt(10轮): test123 = $2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO
-- ============================================================

USE warehouse;

-- ============================================================
-- 1. 产品类别 (2级树形结构, 4大类 + 12子类)
-- ============================================================
INSERT INTO t_product_category (category_id, category_name, parent_id, sort_order, status) VALUES
(1,  '电子元器件', NULL, 1, 1),
(2,  '机械零件',   NULL, 2, 1),
(3,  '化工材料',   NULL, 3, 1),
(4,  '包装材料',   NULL, 4, 1),
(5,  'IC芯片',     1,    1, 1),
(6,  '电阻电容',   1,    2, 1),
(7,  '连接器',     1,    3, 1),
(8,  '紧固件',     2,    1, 1),
(9,  '轴承',       2,    2, 1),
(10, '密封件',     2,    3, 1),
(11, '胶粘剂',     3,    1, 1),
(12, '润滑剂',     3,    2, 1),
(13, '清洁剂',     3,    3, 1),
(14, '纸箱',       4,    1, 1),
(15, '塑料袋',     4,    2, 1),
(16, '标签',       4,    3, 1);

-- ============================================================
-- 2. 库位信息 (3个库区, 每个8个库位)
-- ============================================================
INSERT INTO t_warehouse_location (location_id, location_code, location_name, zone, status) VALUES
-- A区 - 原材料区
(1,  'A-01-01', '原材料货架A01-01', 'A区-原材料', 1),
(2,  'A-01-02', '原材料货架A01-02', 'A区-原材料', 1),
(3,  'A-01-03', '原材料货架A01-03', 'A区-原材料', 1),
(4,  'A-01-04', '原材料货架A01-04', 'A区-原材料', 1),
(5,  'A-02-01', '原材料货架A02-01', 'A区-原材料', 1),
(6,  'A-02-02', '原材料货架A02-02', 'A区-原材料', 1),
(7,  'A-02-03', '原材料货架A02-03', 'A区-原材料', 1),
(8,  'A-02-04', '原材料货架A02-04', 'A区-原材料', 1),
-- B区 - 半成品区
(9,  'B-01-01', '半成品货架B01-01', 'B区-半成品', 1),
(10, 'B-01-02', '半成品货架B01-02', 'B区-半成品', 1),
(11, 'B-01-03', '半成品货架B01-03', 'B区-半成品', 1),
(12, 'B-01-04', '半成品货架B01-04', 'B区-半成品', 1),
(13, 'B-02-01', '半成品货架B02-01', 'B区-半成品', 1),
(14, 'B-02-02', '半成品货架B02-02', 'B区-半成品', 1),
(15, 'B-02-03', '半成品货架B02-03', 'B区-半成品', 1),
(16, 'B-02-04', '半成品货架B02-04', 'B区-半成品', 1),
-- C区 - 成品区
(17, 'C-01-01', '成品货架C01-01', 'C区-成品', 1),
(18, 'C-01-02', '成品货架C01-02', 'C区-成品', 1),
(19, 'C-01-03', '成品货架C01-03', 'C区-成品', 1),
(20, 'C-01-04', '成品货架C01-04', 'C区-成品', 1),
(21, 'C-02-01', '成品货架C02-01', 'C区-成品', 1),
(22, 'C-02-02', '成品货架C02-02', 'C区-成品', 1),
(23, 'C-02-03', '成品货架C02-03', 'C区-成品', 1),
(24, 'C-02-04', '成品货架C02-04', 'C区-成品', 1);

-- ============================================================
-- 3. 产品信息 (20个产品, 覆盖所有类别)
-- ============================================================
INSERT INTO t_product (product_id, product_code, product_name, category_id, unit, spec, upper_limit, lower_limit, default_location_id, status) VALUES
-- 电子元器件
(1,  'P001', 'STM32F407VET6微控制器',    5,  '片',   'LQFP-100 32位ARM',    5000,  500,  1,  1),
(2,  'P002', 'ATmega328P-AU单片机',       5,  '片',   'TQFP-32 8位AVR',      3000,  300,  2,  1),
(3,  'P003', '0805贴片电阻 10KΩ ±1%',    6,  '盘',   '0805 1/8W 100ppm',    50000, 5000, 3,  1),
(4,  'P004', '0805贴片电容 100nF 50V',    6,  '盘',   'X7R 0805 ±10%',       50000, 5000, 3,  1),
(5,  'P005', 'USB Type-C 16P连接器',       7,  '个',   '16Pin 母座 SMT',       8000,  800,  4,  1),
(6,  'P006', '排针 2.54mm 1x40P 直插',    7,  '条',   '单排 镀金 1x40Pin',    3000,  300,  5,  1),
-- 机械零件
(7,  'P007', 'M3x10内六角螺丝 12.9级',    8,  '盒',   'M3x10 12.9级 发黑',    20000, 2000, 6,  1),
(8,  'P008', 'M4x16内六角螺丝 12.9级',    8,  '盒',   'M4x16 12.9级 镀锌',    15000, 1500, 6,  1),
(9,  'P009', '深沟球轴承 6205-2RS',        9,  '个',   '内径25外径52厚15 双密封', 2000,  200,  7,  1),
(10, 'P010', '直线轴承 LM8UU',             9,  '个',   '内径8外径15长24',       1500,  150,  8,  1),
(11, 'P011', 'O型密封圈 NBR φ20x2.5',     10, '个',   '丁腈橡胶 黑色',         10000, 1000, 9,  1),
(12, 'P012', '骨架油封 TC 25x40x7',        10, '个',   '氟橡胶 双唇',           3000,  300,  9,  1),
-- 化工材料
(13, 'P013', '环氧树脂AB胶 50ml',          11, '支',   '双组分 环氧 A+B 50ml',  2000,  200,  10, 1),
(14, 'P014', '瞬干胶 502 20g',             11, '支',   '氰基丙烯酸酯 20g/支',   3000,  300,  10, 1),
(15, 'P015', '锂基润滑脂 1kg装',           12, '桶',   '通用锂基 NLGI 2# 1kg',  800,   80,   11, 1),
(16, 'P016', '无水乙醇 500ml 分析纯',      13, '瓶',   'CH3CH2OH ≥99.7% 500ml', 1000,  100,  12, 1),
(17, 'P017', '异丙醇 IPA 1L 电子级',       13, '瓶',   '99.9% 电子级清洗剂',    600,   60,   12, 1),
-- 包装材料
(18, 'P018', '瓦楞纸箱 400x300x250mm',     14, '个',   '三层 B瓦 承重20kg',     5000,  500,  13, 1),
(19, 'P019', '防静电屏蔽袋 200x300mm',     15, '个',   '三层复合 银色',          10000, 1000, 14, 1),
(20, 'P020', 'PET不干胶标签 50x30mm',      16, '卷',   '白色亮面 2000张/卷',    500,   50,   15, 1);

-- ============================================================
-- 4. 演示用户 (每个角色各一人, 密码均为 test123)
--    BCrypt(10轮): test123
-- ============================================================
INSERT INTO t_user (user_id, username, password, real_name, phone, email, status) VALUES
(2, 'warehouse1', '$2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO', '李仓管',  '13800001101', 'warehouse1@demo.com', 1),
(3, 'purchaser1', '$2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO', '张采购',  '13800001102', 'purchaser1@demo.com', 1),
(4, 'sales1',     '$2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO', '王销售',  '13800001103', 'sales1@demo.com',     1),
(5, 'borrower1',  '$2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO', '赵借用',  '13800001104', 'borrower1@demo.com',  1),
(6, 'manager1',   '$2a$10$7U.jxMB7m0YPhRjF.hPjMejFQKjLcO1xLZ5kqLmCqP.CDaNUmC9GO', '陈经理',  '13800001105', 'manager1@demo.com',   1);

-- 用户-角色关联
INSERT INTO t_user_role (user_id, role_id) VALUES
(2, 2),  -- warehouse1 → 仓库管理员
(3, 3),  -- purchaser1 → 采购人员
(4, 4),  -- sales1     → 销售人员
(5, 5),  -- borrower1  → 借用人员
(6, 6);  -- manager1   → 管理层

-- ============================================================
-- 5. 库存台账 (初始化各产品的库存)
-- ============================================================
INSERT INTO t_stock (product_id, location_id, quantity, create_by) VALUES
-- 电子元器件在A区
(1,  1,  3200.00, 2),
(2,  2,  1800.00, 2),
(3,  3,  45000.00, 2),
(4,  3,  42000.00, 2),
(5,  4,  5200.00, 2),
(6,  5,  1800.00, 2),
-- 机械零件在A区
(7,  6,  15000.00, 2),
(8,  6,  11000.00, 2),
(9,  7,  1200.00, 2),
(10, 8,  850.00, 2),
(11, 9,  7500.00, 2),
(12, 9,  1800.00, 2),
-- 化工材料在A区
(13, 10, 1200.00, 2),
(14, 10, 2100.00, 2),
(15, 11, 520.00, 2),
(16, 12, 680.00, 2),
(17, 12, 380.00, 2),
-- 包装材料在B区
(18, 13, 3200.00, 2),
(19, 14, 6800.00, 2),
(20, 15, 320.00, 2);

-- ============================================================
-- 6. 入库单 (3张入库单 + 明细)
--    操作员: purchaser1(user_id=3), 审核员: warehouse1(user_id=2)
-- ============================================================
INSERT INTO t_inbound_order (order_id, order_no, supplier_id, operator_id, confirm_operator_id, order_time, confirm_time, status, remark) VALUES
(1, 'RK20250601001', 1, 3, 2, '2025-06-01 09:30:00', '2025-06-01 14:00:00', 1, '6月第一批电子料入库'),
(2, 'RK20250602001', 2, 3, 2, '2025-06-02 10:00:00', '2025-06-02 15:30:00', 1, '机械标准件补充库存'),
(3, 'RK20250603001', 3, 3, 2, '2025-06-03 11:00:00', '2025-06-03 16:00:00', 1, '化工辅料入库'),
(4, 'RK20250610001', 1, 3, NULL, '2025-06-10 08:30:00', NULL, 0, '待审核-紧急补货电子料');

-- 入库单1明细: 电子料
INSERT INTO t_inbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(1, 1,  1000.00, 28.50, 1),
(1, 2,  500.00,  12.00, 2),
(1, 3,  20000.00, 0.02, 3),
(1, 4,  20000.00, 0.05, 3),
(1, 5,  2000.00, 3.20,  4);

-- 入库单2明细: 机械件
INSERT INTO t_inbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(2, 7,  5000.00,  0.15, 6),
(2, 8,  3000.00,  0.20, 6),
(2, 9,  300.00,   15.00, 7),
(2, 11, 3000.00,  0.50, 9),
(2, 12, 500.00,   8.00, 9);

-- 入库单3明细: 化工料
INSERT INTO t_inbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(3, 13, 500.00,  12.00, 10),
(3, 14, 800.00,  3.00,  10),
(3, 15, 200.00,  45.00, 11),
(3, 16, 300.00,  18.00, 12),
(3, 17, 150.00,  35.00, 12);

-- 入库单4明细: 待审核
INSERT INTO t_inbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(4, 1,  500.00,  29.00, 1),
(4, 3,  10000.00, 0.02, 3),
(4, 6,  1000.00,  0.80, 5);

-- ============================================================
-- 7. 出库单 (2张出库单 + 明细)
--    操作员: sales1(user_id=4), 审核员: warehouse1(user_id=2)
-- ============================================================
INSERT INTO t_outbound_order (order_id, order_no, customer_id, department, applicant, operator_id, confirm_operator_id, order_time, confirm_time, status, remark) VALUES
(1, 'CK20250605001', 1, '生产部', '刘主管', 4, 2, '2025-06-05 09:00:00', '2025-06-05 11:00:00', 1, '生产领料-电子料'),
(2, 'CK20250606001', 2, '研发部', '周工',   4, 2, '2025-06-06 14:00:00', '2025-06-06 16:00:00', 1, '研发项目物料出库'),
(3, 'CK20250612001', 3, '售后部', '吴主管', 4, NULL, '2025-06-12 10:00:00', NULL, 0, '待审核-售后维修物料');

-- 出库单1明细
INSERT INTO t_outbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(1, 1,  200.00,  30.00, 1),
(1, 3,  5000.00, 0.03, 3),
(1, 4,  3000.00, 0.06, 3),
(1, 5,  500.00,  3.50,  4),
(1, 6,  300.00,  1.00,  5);

-- 出库单2明细
INSERT INTO t_outbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(2, 9,  50.00,  18.00, 7),
(2, 10, 100.00, 12.00, 8),
(2, 11, 200.00, 0.60,  9),
(2, 13, 50.00,  15.00, 10);

-- 出库单3明细: 待审核
INSERT INTO t_outbound_order_detail (order_id, product_id, quantity, unit_price, location_id) VALUES
(3, 2, 100.00, 13.00, 2),
(3, 7, 500.00, 0.18,  6);

-- ============================================================
-- 8. 借出还库记录 (4条记录)
--    操作员: borrower1(user_id=5)
-- ============================================================
INSERT INTO t_borrow_record (record_id, record_no, product_id, quantity, borrower, borrow_date, expected_return_date, actual_return_date, return_quantity, operator_id, status, remark) VALUES
(1, 'JJ20250601001', 1,  50.00,  '李工程师', '2025-06-01', '2025-06-15', '2025-06-12', 50.00, 5, 1, '项目调试用MCU, 已全部归还'),
(2, 'JJ20250608001', 9,  30.00,  '王技术员', '2025-06-08', '2025-06-22', '2025-06-20', 25.00, 5, 2, '设备维修用轴承, 部分归还(损坏5个)'),
(3, 'JJ20250610001', 15, 10.00,  '张维护',   '2025-06-10', '2025-07-10', NULL,        NULL,  5, 0, '日常润滑保养领用, 尚未归还'),
(4, 'JJ20250615001', 16, 20.00,  '陈质检',   '2025-06-15', '2025-06-30', NULL,        NULL,  5, 0, '实验室清洗用, 预计月底归还');

-- ============================================================
-- 9. 盘点记录 (3条盘点记录)
--    操作员: warehouse1(user_id=2)
-- ============================================================
INSERT INTO t_inventory_check (check_id, check_no, product_id, book_quantity, actual_quantity, diff_quantity, operator_id, check_date, status, remark) VALUES
(1, 'PD20250601001', 1,  3250.00, 3200.00, -50.00,  2, '2025-06-01', 1, 'MCU芯片盘点亏损50片, 已调整'),
(2, 'PD20250601002', 3,  46000.00, 45000.00, -1000.00, 2, '2025-06-01', 1, '贴片电阻盘点亏损1000片, 已调整(疑似散落)'),
(3, 'PD20250601003', 7,  15200.00, 15000.00, -200.00, 2, '2025-06-01', 1, 'M3螺丝盘点亏损200盒, 已调整'),
(4, 'PD20250601004', 14, 2050.00, 2100.00, 50.00,   2, '2025-06-01', 1, '502胶水盘盈50支, 已调整(供应商多发货)'),
(5, 'PD20250620001', 5,  5180.00, 5200.00, 20.00,   2, '2025-06-20', 0, 'USB连接器盘点, 盘盈20个待确认来源');

-- ============================================================
-- 10. 操作日志 (示例日志，按时间降序)
-- ============================================================
INSERT INTO t_operation_log (user_id, username, operation, module, request_method, request_url, request_params, response_result, execution_time, ip_address, status, operate_time) VALUES
(1, 'admin',      '用户登录',              '系统-认证', 'POST', '/api/v1/auth/login',      '{"username":"admin"}',            '{"code":200}',  45,  '127.0.0.1', 1, '2025-06-20 08:30:00'),
(4, 'sales1',     '填写出库单',            '出库管理', 'POST', '/api/v1/outbound-orders',  '{"customerId":3}',                '{"code":200}',  120, '127.0.0.1', 1, '2025-06-12 10:00:00'),
(3, 'purchaser1', '填写入库单',            '入库管理', 'POST', '/api/v1/inbound-orders',  '{"supplierId":1}',                '{"code":200}',  135, '127.0.0.1', 1, '2025-06-10 08:30:00'),
(5, 'borrower1',  '借条登记',              '借还管理', 'POST', '/api/v1/borrow-records',   '{"productId":15,"quantity":10}',  '{"code":200}',  95,  '127.0.0.1', 1, '2025-06-10 09:00:00'),
(2, 'warehouse1', '审核出库单 CK20250606001', '出库管理', 'PUT', '/api/v1/outbound-orders/2/confirm', '{"status":1}', '{"code":200}', 80, '127.0.0.1', 1, '2025-06-06 16:00:00'),
(2, 'warehouse1', '审核出库单 CK20250605001', '出库管理', 'PUT', '/api/v1/outbound-orders/1/confirm', '{"status":1}', '{"code":200}', 75, '127.0.0.1', 1, '2025-06-05 11:00:00'),
(2, 'warehouse1', '审核入库单 RK20250603001', '入库管理', 'PUT', '/api/v1/inbound-orders/3/confirm', '{"status":1}', '{"code":200}', 90, '127.0.0.1', 1, '2025-06-03 16:00:00'),
(2, 'warehouse1', '审核入库单 RK20250602001', '入库管理', 'PUT', '/api/v1/inbound-orders/2/confirm', '{"status":1}', '{"code":200}', 85, '127.0.0.1', 1, '2025-06-02 15:30:00'),
(2, 'warehouse1', '审核入库单 RK20250601001', '入库管理', 'PUT', '/api/v1/inbound-orders/1/confirm', '{"status":1}', '{"code":200}', 78, '127.0.0.1', 1, '2025-06-01 14:00:00'),
(2, 'warehouse1', '盘点确认 PD20250601001',   '盘点管理', 'PUT', '/api/v1/inventory-checks/1/confirm', '{"actualQuantity":3200}', '{"code":200}', 110, '127.0.0.1', 1, '2025-06-01 13:00:00'),
(2, 'warehouse1', '盘点确认 PD20250601002',   '盘点管理', 'PUT', '/api/v1/inventory-checks/2/confirm', '{"actualQuantity":45000}', '{"code":200}', 105, '127.0.0.1', 1, '2025-06-01 13:10:00'),
(2, 'warehouse1', '盘点确认 PD20250601003',   '盘点管理', 'PUT', '/api/v1/inventory-checks/3/confirm', '{"actualQuantity":15000}', '{"code":200}', 98, '127.0.0.1', 1, '2025-06-01 13:20:00'),
(2, 'warehouse1', '盘点确认 PD20250601004',   '盘点管理', 'PUT', '/api/v1/inventory-checks/4/confirm', '{"actualQuantity":2100}', '{"code":200}', 112, '127.0.0.1', 1, '2025-06-01 13:30:00'),
(1, 'admin',      '创建用户 warehouse1',   '系统-用户', 'POST', '/api/v1/users',          '{"username":"warehouse1"}',        '{"code":200}',  65,  '127.0.0.1', 1, '2025-05-20 10:00:00'),
(6, 'manager1',   '查看入库报表',          '查询报表', 'GET',  '/api/v1/reports/inbound',  '{"startDate":"2025-06-01","endDate":"2025-06-30"}', '{"code":200}', 250, '127.0.0.1', 1, '2025-06-20 14:00:00');

-- ============================================================
-- 完成提示
-- ============================================================
SELECT 'Demo data loaded successfully!' AS message;
SELECT 'Total categories:' AS item, COUNT(*) AS count FROM t_product_category
UNION ALL SELECT 'Total locations:', COUNT(*) FROM t_warehouse_location
UNION ALL SELECT 'Total products:', COUNT(*) FROM t_product
UNION ALL SELECT 'Total users:', COUNT(*) FROM t_user
UNION ALL SELECT 'Total stock records:', COUNT(*) FROM t_stock
UNION ALL SELECT 'Total inbound orders:', COUNT(*) FROM t_inbound_order
UNION ALL SELECT 'Total inbound details:', COUNT(*) FROM t_inbound_order_detail
UNION ALL SELECT 'Total outbound orders:', COUNT(*) FROM t_outbound_order
UNION ALL SELECT 'Total outbound details:', COUNT(*) FROM t_outbound_order_detail
UNION ALL SELECT 'Total borrow records:', COUNT(*) FROM t_borrow_record
UNION ALL SELECT 'Total inventory checks:', COUNT(*) FROM t_inventory_check
UNION ALL SELECT 'Total operation logs:', COUNT(*) FROM t_operation_log;
