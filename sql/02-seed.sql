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
VALUES ('admin', '$2b$10$7YgJw5khn76f0wSr4P.65OYlwEoG1VbkZeixDgBAi1xnNa1I0SfI2', '系统管理员', 1);

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

-- ============================================================
-- P2-4: 预置权限(完整RBAC权限树)
-- 所有非顶级权限的 parent_id 改用 SELECT 解析,不再依赖 AUTO_INCREMENT 顺序
-- 即使后续在中间插入新权限行,所有父子引用都不会错位
-- ============================================================

-- 第 1 步:插入所有顶级菜单(无 parent)
INSERT INTO t_permission (permission_code, permission_name, resource_type, parent_id) VALUES
('dashboard', '仪表盘', 'menu', NULL),
('system', '系统管理', 'menu', NULL),
('base', '基础资料', 'menu', NULL),
('inbound', '入库管理', 'menu', NULL),
('outbound', '出库管理', 'menu', NULL),
('borrow', '借还管理', 'menu', NULL),
('inventory', '盘库管理', 'menu', NULL),
('report', '查询报表', 'menu', NULL);

-- 第 2 步:插入二级菜单(挂在顶级菜单下,parent_id 用 permission_code 解析)
INSERT INTO t_permission (permission_code, permission_name, resource_type, parent_id) VALUES
('dashboard:view', '查看仪表盘', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'dashboard')),

('system:user', '用户管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'system')),
('system:role', '角色管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'system')),
('system:permission', '权限管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'system')),
('system:log', '操作日志', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'system')),

('base:product', '产品管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),
('base:category', '产品类别', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),
('base:location', '库位管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),
('base:supplier', '供应商管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),
('base:customer', '客户管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),
('base:stock', '库存管理', 'menu', (SELECT permission_id FROM t_permission WHERE permission_code = 'base')),

('inbound:create', '填写入库单', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inbound')),
('inbound:audit', '入库审核', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inbound')),
('inbound:query', '入库查询', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inbound')),

('outbound:create', '填写出库单', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'outbound')),
('outbound:audit', '出库审核', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'outbound')),
('outbound:query', '出库查询', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'outbound')),

('borrow:create', '借条登记', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'borrow')),
('borrow:return', '归还登记', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'borrow')),
('borrow:query', '借还查询', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'borrow')),

('inventory:create', '创建盘点', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inventory')),
('inventory:confirm', '盘点审核', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inventory')),
('inventory:query', '盘点查询', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'inventory')),

('report:inbound', '入库报表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'report')),
('report:outbound', '出库报表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'report')),
('report:stock', '库存报表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'report')),
('report:comprehensive', '综合报表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'report'));

-- 第 3 步:插入三级菜单(按钮权限,挂在二级菜单下)
INSERT INTO t_permission (permission_code, permission_name, resource_type, parent_id) VALUES
('system:user:list', '用户列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:user')),
('system:user:create', '创建用户', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:user')),
('system:user:edit', '编辑用户', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:user')),
('system:user:delete', '删除用户', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:user')),
-- P1-5: 管理员重置密码专用权限(与 PUT /users/{id}/password/reset 端点对应)
('system:user:resetPassword', '重置密码', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:user')),

('system:role:list', '角色列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:role')),
('system:role:create', '创建角色', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:role')),
('system:role:edit', '编辑角色', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:role')),
('system:role:delete', '删除角色', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:role')),
('system:role:assign', '分配权限', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:role')),

('system:permission:list', '权限列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:permission')),
('system:log:list', '日志列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'system:log')),

('base:product:list', '产品列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:product')),
('base:product:create', '创建产品', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:product')),
('base:product:edit', '编辑产品', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:product')),
('base:product:delete', '删除产品', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:product')),

('base:category:list', '类别列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:category')),
('base:category:create', '创建类别', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:category')),
('base:category:edit', '编辑类别', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:category')),
('base:category:delete', '删除类别', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:category')),

('base:location:list', '库位列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:location')),
('base:location:create', '创建库位', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:location')),
('base:location:edit', '编辑库位', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:location')),
('base:location:delete', '删除库位', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:location')),

('base:supplier:list', '供应商列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:supplier')),
('base:supplier:create', '创建供应商', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:supplier')),
('base:supplier:edit', '编辑供应商', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:supplier')),
('base:supplier:delete', '删除供应商', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:supplier')),

('base:customer:list', '客户列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:customer')),
('base:customer:create', '创建客户', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:customer')),
('base:customer:edit', '编辑客户', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:customer')),
('base:customer:delete', '删除客户', 'button', (SELECT permission_id FROM (SELECT permission_id FROM t_permission WHERE permission_code = 'base:customer') AS p)),

('base:stock:list', '库存列表', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:stock')),
('base:stock:init', '库存初始化', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:stock')),
('base:stock:alert', '库存预警', 'button', (SELECT permission_id FROM t_permission WHERE permission_code = 'base:stock'));

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
