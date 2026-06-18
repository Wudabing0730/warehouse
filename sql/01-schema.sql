-- ============================================================
-- 仓库管理系统 - 数据库建表脚本 (19张表)
-- InnoDB + utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS warehouse DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE warehouse;

-- ============================================================
-- 系统管理相关
-- ============================================================

-- 1. 角色表
CREATE TABLE t_role (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_desc VARCHAR(200),
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 权限表
CREATE TABLE t_permission (
    permission_id BIGINT NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(80) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL COMMENT 'menu/button/api',
    parent_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 角色-权限关联表
CREATE TABLE t_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 用户表
CREATE TABLE t_user (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密,含盐值',
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 用户-角色关联表
CREATE TABLE t_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 操作日志表
CREATE TABLE t_operation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) COMMENT '冗余用户名，方便查询',
    operation VARCHAR(200) NOT NULL COMMENT '操作描述',
    module VARCHAR(50) NOT NULL COMMENT '操作模块',
    request_method VARCHAR(10) COMMENT 'GET/POST/PUT/DELETE',
    request_url VARCHAR(255) COMMENT '请求API路径',
    request_params TEXT COMMENT '请求参数(JSON)',
    response_result TEXT COMMENT '响应结果(JSON,截断)',
    execution_time INT COMMENT '执行耗时(毫秒)',
    ip_address VARCHAR(50),
    user_agent VARCHAR(255) COMMENT '浏览器/设备信息',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-成功 0-失败',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_user_id (user_id),
    KEY idx_module (module),
    KEY idx_operate_time (operate_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 基础资料相关
-- ============================================================

-- 7. 产品类别表
CREATE TABLE t_product_category (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    sort_order INT,
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_name (category_name),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 库位信息表
CREATE TABLE t_warehouse_location (
    location_id BIGINT NOT NULL AUTO_INCREMENT,
    location_code VARCHAR(30) NOT NULL,
    location_name VARCHAR(50) NOT NULL,
    zone VARCHAR(30),
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (location_id),
    UNIQUE KEY uk_location_code (location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 产品信息表
CREATE TABLE t_product (
    product_id BIGINT NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(30) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    unit VARCHAR(20) NOT NULL,
    spec VARCHAR(100),
    upper_limit DECIMAL(12,2),
    lower_limit DECIMAL(12,2),
    default_location_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category_id (category_id),
    KEY idx_default_location_id (default_location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 库存台账表
CREATE TABLE t_stock (
    stock_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL COMMENT '库位非空，一个产品可在多个库位',
    quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_id),
    UNIQUE KEY uk_product_location (product_id, location_id),
    KEY idx_location_id (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 供应商表
CREATE TABLE t_supplier (
    supplier_id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_code VARCHAR(30) NOT NULL,
    supplier_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    address VARCHAR(200),
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (supplier_id),
    UNIQUE KEY uk_supplier_code (supplier_code),
    KEY idx_supplier_name (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 客户表
CREATE TABLE t_customer (
    customer_id BIGINT NOT NULL AUTO_INCREMENT,
    customer_code VARCHAR(30) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    address VARCHAR(200),
    status TINYINT NOT NULL DEFAULT 1,
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uk_customer_code (customer_code),
    KEY idx_customer_name (customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 核心业务：入库（头表+明细表）
-- ============================================================

-- 13a. 入库单头表
CREATE TABLE t_inbound_order (
    order_id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL COMMENT 'RK+yyyyMMdd+流水号',
    supplier_id BIGINT COMMENT '关联供应商',
    operator_id BIGINT NOT NULL,
    confirm_operator_id BIGINT,
    order_time DATETIME NOT NULL,
    confirm_time DATETIME,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已审核 2-已拒绝',
    remark VARCHAR(500),
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_operator_id (operator_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_status (status),
    KEY idx_order_time (order_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13b. 入库单明细表
CREATE TABLE t_inbound_order_detail (
    detail_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    unit_price DECIMAL(12,2),
    location_id BIGINT COMMENT '目标库位',
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (detail_id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id),
    CONSTRAINT fk_ind_order FOREIGN KEY (order_id) REFERENCES t_inbound_order(order_id),
    CONSTRAINT fk_ind_prod FOREIGN KEY (product_id) REFERENCES t_product(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 核心业务：出库（头表+明细表）
-- ============================================================

-- 14a. 出库单头表
CREATE TABLE t_outbound_order (
    order_id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL COMMENT 'CK+yyyyMMdd+流水号',
    customer_id BIGINT COMMENT '关联客户',
    department VARCHAR(100) COMMENT '领用部门',
    applicant VARCHAR(50) COMMENT '领用人',
    operator_id BIGINT NOT NULL,
    confirm_operator_id BIGINT,
    order_time DATETIME NOT NULL,
    confirm_time DATETIME,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已审核 2-已拒绝',
    remark VARCHAR(500),
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_operator_id (operator_id),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status),
    KEY idx_order_time (order_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14b. 出库单明细表
CREATE TABLE t_outbound_order_detail (
    detail_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    unit_price DECIMAL(12,2),
    location_id BIGINT COMMENT '拣货库位',
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (detail_id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id),
    CONSTRAINT fk_outd_order FOREIGN KEY (order_id) REFERENCES t_outbound_order(order_id),
    CONSTRAINT fk_outd_prod FOREIGN KEY (product_id) REFERENCES t_product(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 扩展业务
-- ============================================================

-- 15. 借出还库记录表
CREATE TABLE t_borrow_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    record_no VARCHAR(30) NOT NULL COMMENT 'JJ+yyyyMMdd+流水号',
    product_id BIGINT NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    borrower VARCHAR(50) NOT NULL,
    borrow_date DATETIME NOT NULL COMMENT 'P0-4: 改为 DATETIME 以保留借出时分秒',
    expected_return_date DATETIME NOT NULL COMMENT 'P0-4: 改为 DATETIME',
    actual_return_date DATETIME COMMENT 'P0-4: 改为 DATETIME 以保留归还时分秒',
    return_quantity DECIMAL(12,2),
    operator_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-借出中 1-已归还 2-部分归还',
    remark VARCHAR(200),
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_product_id (product_id),
    KEY idx_status (status),
    KEY idx_borrow_date (borrow_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 盘点记录表
CREATE TABLE t_inventory_check (
    check_id BIGINT NOT NULL AUTO_INCREMENT,
    check_no VARCHAR(30) NOT NULL COMMENT 'PD+yyyyMMdd+流水号',
    product_id BIGINT NOT NULL,
    book_quantity DECIMAL(12,2) NOT NULL COMMENT '账面数量',
    actual_quantity DECIMAL(12,2) NOT NULL COMMENT '实盘数量',
    diff_quantity DECIMAL(12,2) NOT NULL COMMENT '盈亏数量',
    operator_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已调整 2-已取消',
    remark VARCHAR(200),
    create_by BIGINT,
    update_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (check_id),
    UNIQUE KEY uk_check_no (check_no),
    KEY idx_product_id (product_id),
    KEY idx_status (status),
    KEY idx_check_date (check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 系统支撑
-- ============================================================

-- 17. 刷新令牌表（Redis不可用时的fallback）
CREATE TABLE t_refresh_token (
    token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL COMMENT 'refresh token的SHA-256哈希',
    issued_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_user_id (user_id),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 外键约束
-- ============================================================
ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(user_id);
ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(role_id);
ALTER TABLE t_role_permission ADD CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES t_role(role_id);
ALTER TABLE t_role_permission ADD CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES t_permission(permission_id);
ALTER TABLE t_operation_log ADD CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES t_user(user_id);
ALTER TABLE t_product_category ADD CONSTRAINT fk_cat_parent FOREIGN KEY (parent_id) REFERENCES t_product_category(category_id);
ALTER TABLE t_product ADD CONSTRAINT fk_prod_cat FOREIGN KEY (category_id) REFERENCES t_product_category(category_id);
ALTER TABLE t_product ADD CONSTRAINT fk_prod_loc FOREIGN KEY (default_location_id) REFERENCES t_warehouse_location(location_id);
ALTER TABLE t_stock ADD CONSTRAINT fk_stock_prod FOREIGN KEY (product_id) REFERENCES t_product(product_id);
ALTER TABLE t_stock ADD CONSTRAINT fk_stock_loc FOREIGN KEY (location_id) REFERENCES t_warehouse_location(location_id);
ALTER TABLE t_inbound_order ADD CONSTRAINT fk_in_oper FOREIGN KEY (operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_inbound_order ADD CONSTRAINT fk_in_confirm FOREIGN KEY (confirm_operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_inbound_order ADD CONSTRAINT fk_in_supplier FOREIGN KEY (supplier_id) REFERENCES t_supplier(supplier_id);
ALTER TABLE t_outbound_order ADD CONSTRAINT fk_out_oper FOREIGN KEY (operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_outbound_order ADD CONSTRAINT fk_out_confirm FOREIGN KEY (confirm_operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_outbound_order ADD CONSTRAINT fk_out_customer FOREIGN KEY (customer_id) REFERENCES t_customer(customer_id);
ALTER TABLE t_borrow_record ADD CONSTRAINT fk_bor_prod FOREIGN KEY (product_id) REFERENCES t_product(product_id);
ALTER TABLE t_borrow_record ADD CONSTRAINT fk_bor_oper FOREIGN KEY (operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_inventory_check ADD CONSTRAINT fk_chk_prod FOREIGN KEY (product_id) REFERENCES t_product(product_id);
ALTER TABLE t_inventory_check ADD CONSTRAINT fk_chk_oper FOREIGN KEY (operator_id) REFERENCES t_user(user_id);
ALTER TABLE t_refresh_token ADD CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES t_user(user_id);
