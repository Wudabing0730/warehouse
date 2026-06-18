-- ============================================================
-- 仓库管理系统 - 初始数据
-- ============================================================

USE warehouse;

-- 预置6种角色
INSERT INTO t_role (role_name, role_desc) VALUES
('系统管理员', '拥有全部权限'),
('仓库管理员', '管理出入库、盘点、库存'),
('采购人员', '填写入库单、查询入库记录'),
('销售人员', '填写出库单、查询出库记录'),
('借用人员', '借出归还操作'),
('管理层', '查看报表和统计数据');

-- 预置管理员账号 (密码: admin123, BCrypt加密)
-- BCrypt(10轮): admin123
INSERT INTO t_user (username, password, real_name, status)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1);

INSERT INTO t_user_role (user_id, role_id) VALUES (1, 1);

-- 预置示例供应商
INSERT INTO t_supplier (supplier_code, supplier_name, contact_person, contact_phone) VALUES
('GYS001', '北京工业原料有限公司', '张经理', '13800001001'),
('GYS002', '上海精密仪器有限公司', '李经理', '13800001002'),
('GYS003', '深圳电子材料有限公司', '王经理', '13800001003');

-- 预置示例客户
INSERT INTO t_customer (customer_code, customer_name, contact_person, contact_phone) VALUES
('KH001', '某制造工厂A', '赵主管', '13900002001'),
('KH002', '某科研机构B', '钱博士', '13900002002'),
('KH003', '某销售公司C', '孙经理', '13900002003');

-- 预置权限（完整RBAC权限树）
INSERT INTO t_permission (permission_code, permission_name, resource_type, parent_id) VALUES
('dashboard', '仪表盘', 'menu', NULL),
('dashboard:view', '查看仪表盘', 'button', 1),
('system', '系统管理', 'menu', NULL),
('system:user', '用户管理', 'menu', 3),
('system:user:list', '用户列表', 'button', 4),
('system:user:create', '创建用户', 'button', 4),
('system:user:edit', '编辑用户', 'button', 4),
('system:user:delete', '删除用户', 'button', 4),
('system:role', '角色管理', 'menu', 3),
('system:role:list', '角色列表', 'button', 9),
('system:role:create', '创建角色', 'button', 9),
('system:role:edit', '编辑角色', 'button', 9),
('system:role:delete', '删除角色', 'button', 9),
('system:role:assign', '分配权限', 'button', 9),
('system:permission', '权限管理', 'menu', 3),
('system:permission:list', '权限列表', 'button', 15),
('system:log', '操作日志', 'menu', 3),
('system:log:list', '日志列表', 'button', 17),
('base', '基础资料', 'menu', NULL),
('base:product', '产品管理', 'menu', 19),
('base:product:list', '产品列表', 'button', 20),
('base:product:create', '创建产品', 'button', 20),
('base:product:edit', '编辑产品', 'button', 20),
('base:product:delete', '删除产品', 'button', 20),
('base:category', '产品类别', 'menu', 19),
('base:category:list', '类别列表', 'button', 25),
('base:category:create', '创建类别', 'button', 25),
('base:category:edit', '编辑类别', 'button', 25),
('base:category:delete', '删除类别', 'button', 25),
('base:location', '库位管理', 'menu', 19),
('base:location:list', '库位列表', 'button', 30),
('base:location:create', '创建库位', 'button', 30),
('base:location:edit', '编辑库位', 'button', 30),
('base:location:delete', '删除库位', 'button', 30),
('base:supplier', '供应商管理', 'menu', 19),
('base:supplier:list', '供应商列表', 'button', 35),
('base:supplier:create', '创建供应商', 'button', 35),
('base:supplier:edit', '编辑供应商', 'button', 35),
('base:supplier:delete', '删除供应商', 'button', 35),
('base:customer', '客户管理', 'menu', 19),
('base:customer:list', '客户列表', 'button', 40),
('base:customer:create', '创建客户', 'button', 40),
('base:customer:edit', '编辑客户', 'button', 40),
('base:customer:delete', '删除客户', 'button', 40),
('base:stock', '库存管理', 'menu', 19),
('base:stock:list', '库存列表', 'button', 45),
('base:stock:init', '库存初始化', 'button', 45),
('base:stock:alert', '库存预警', 'button', 45),
('inbound', '入库管理', 'menu', NULL),
('inbound:create', '填写入库单', 'button', 49),
('inbound:audit', '入库审核', 'button', 49),
('inbound:query', '入库查询', 'button', 49),
('outbound', '出库管理', 'menu', NULL),
('outbound:create', '填写出库单', 'button', 53),
('outbound:audit', '出库审核', 'button', 53),
('outbound:query', '出库查询', 'button', 53),
('borrow', '借还管理', 'menu', NULL),
('borrow:create', '借条登记', 'button', 57),
('borrow:return', '归还登记', 'button', 57),
('borrow:query', '借还查询', 'button', 57),
('inventory', '盘库管理', 'menu', NULL),
('inventory:create', '创建盘点', 'button', 61),
('inventory:confirm', '盘点审核', 'button', 61),
('inventory:query', '盘点查询', 'button', 61),
('report', '查询报表', 'menu', NULL),
('report:inbound', '入库报表', 'button', 65),
('report:outbound', '出库报表', 'button', 65),
('report:stock', '库存报表', 'button', 65),
('report:comprehensive', '综合报表', 'button', 65);

-- 给系统管理员(role_id=1)授予所有权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 1, permission_id FROM t_permission;

-- 给仓库管理员(role_id=2)授予仓库相关权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 2, permission_id FROM t_permission WHERE permission_code IN (
    'dashboard', 'dashboard:view',
    'base', 'base:product', 'base:product:list', 'base:product:create', 'base:product:edit',
    'base:category', 'base:category:list', 'base:category:create', 'base:category:edit',
    'base:location', 'base:location:list', 'base:location:create', 'base:location:edit',
    'base:supplier', 'base:supplier:list', 'base:supplier:create', 'base:supplier:edit',
    'base:customer', 'base:customer:list', 'base:customer:create', 'base:customer:edit',
    'base:stock', 'base:stock:list', 'base:stock:init', 'base:stock:alert',
    'inbound', 'inbound:create', 'inbound:audit', 'inbound:query',
    'outbound', 'outbound:create', 'outbound:audit', 'outbound:query',
    'borrow', 'borrow:create', 'borrow:return', 'borrow:query',
    'inventory', 'inventory:create', 'inventory:confirm', 'inventory:query',
    'report', 'report:inbound', 'report:outbound', 'report:stock', 'report:comprehensive',
    'system:log', 'system:log:list'
);

-- 给采购人员(role_id=3)授予入库相关权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 3, permission_id FROM t_permission WHERE permission_code IN (
    'dashboard', 'dashboard:view',
    'base:product', 'base:product:list',
    'base:supplier', 'base:supplier:list',
    'inbound', 'inbound:create', 'inbound:query'
);

-- 给销售人员(role_id=4)授予出库相关权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 4, permission_id FROM t_permission WHERE permission_code IN (
    'dashboard', 'dashboard:view',
    'base:product', 'base:product:list',
    'base:customer', 'base:customer:list',
    'outbound', 'outbound:create', 'outbound:query'
);

-- 给借用人员(role_id=5)授予借还相关权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 5, permission_id FROM t_permission WHERE permission_code IN (
    'dashboard', 'dashboard:view',
    'base:product', 'base:product:list',
    'borrow', 'borrow:create', 'borrow:return', 'borrow:query'
);

-- 给管理层(role_id=6)授予报表查看权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT 6, permission_id FROM t_permission WHERE permission_code IN (
    'dashboard', 'dashboard:view',
    'report', 'report:inbound', 'report:outbound', 'report:stock', 'report:comprehensive'
);
