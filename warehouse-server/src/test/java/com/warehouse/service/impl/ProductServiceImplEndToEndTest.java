/**
 * 端到端集成测试:跑完 ProductServiceImpl.delete() 完整路径,看真实数据库行
 *
 * 用户报告:"禁用 P001 等所有数据更改操作都无法正常持久化"
 *
 * 之前的 MyBatis-PlusLogicalDeleteConfigConsistencyTest 修复了全局 logical-delete 配置。
 * ProductUpdateSqlIntegrationTest 验证了底层 baseMapper.updateById 能正确持久化。
 *
 * 现在要验证 service 层完整路径(ProductServiceImpl.delete/update)是否真的把 status 写入 DB
 *
 * 怀疑点:
 *   - @Transactional 实际生效吗?没生效时,DB 可能回滚
 *   - BeanUtils.copyProperties(DTO, entity, ...) 是否真的把 status 字段从 DTO 拷到 entity
 *   - AuditMetaObjectHandler.updateFill 是否抛异常导致事务回滚
 */
package com.warehouse.service.impl;

import com.warehouse.dto.request.ProductUpdateDTO;
import com.warehouse.dto.response.ProductVO;
import com.warehouse.entity.Product;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("product-test")
class ProductServiceImplEndToEndTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_product_category");
            stmt.execute("DROP TABLE IF EXISTS t_warehouse_location");
            stmt.execute("DROP TABLE IF EXISTS t_product");
            // t_product_category 必须有(因为 ProductServiceImpl.update 会校验 categoryId)
            stmt.execute(
                "CREATE TABLE t_product_category (" +
                "  category_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  category_name VARCHAR(50) NOT NULL," +
                "  parent_id BIGINT," +
                "  sort_order INT," +
                "  status TINYINT NOT NULL DEFAULT 1," +
                "  create_by BIGINT," +
                "  update_by BIGINT," +
                "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  update_time DATETIME" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE t_warehouse_location (" +
                "  location_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  location_code VARCHAR(30) NOT NULL," +
                "  location_name VARCHAR(50) NOT NULL," +
                "  zone VARCHAR(30)," +
                "  status TINYINT NOT NULL DEFAULT 1," +
                "  create_by BIGINT," +
                "  update_by BIGINT," +
                "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  update_time DATETIME" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE t_product (" +
                "  product_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  product_code VARCHAR(30) NOT NULL," +
                "  product_name VARCHAR(100) NOT NULL," +
                "  category_id BIGINT NOT NULL," +
                "  unit VARCHAR(20) NOT NULL," +
                "  spec VARCHAR(100)," +
                "  upper_limit DECIMAL(12,2)," +
                "  lower_limit DECIMAL(12,2)," +
                "  default_location_id BIGINT," +
                "  status TINYINT NOT NULL DEFAULT 1," +
                "  create_by BIGINT," +
                "  update_by BIGINT," +
                "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  update_time DATETIME" +
                ")"
            );
            // 插入一个 category 满足外键
            stmt.execute("INSERT INTO t_product_category (category_id, category_name) VALUES (1, '默认分类')");
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("端到端:ProductServiceImpl.delete(id) 必须把数据库里 product.status 改为 0")
    void delete_mustPersistStatusChangeInDb() {
        // 1. 准备:插入 P001
        Product p = new Product();
        p.setProductCode("P001");
        p.setProductName("测试产品");
        p.setCategoryId(1L);
        p.setUnit("件");
        p.setStatus(1);
        productMapper.insert(p);
        Long productId = p.getProductId();
        assertNotNull(productId);
        System.out.println(">>> [delete 测试] INSERT P001, productId=" + productId);

        // 2. 调用 service.delete
        productService.delete(productId);
        System.out.println(">>> [delete 测试] productService.delete() 返回");

        // 3. 直接查 DB(JdbcTemplate 走独立连接,绕开 service 的任何缓存)
        Integer statusInDb = jdbc.queryForObject(
                "SELECT status FROM t_product WHERE product_id = ?",
                Integer.class, productId);
        System.out.println(">>> [delete 测试] DB 中 P001 的 status = " + statusInDb);
        assertEquals(0, statusInDb,
                "BUG 复现:ProductServiceImpl.delete() 调用后,DB 中 status 应为 0,"
                        + " 但实际为 " + statusInDb);
    }

    @Test
    @DisplayName("端到端:ProductServiceImpl.update(id, dto) 必须把 DTO.status=0 持久化")
    void update_mustPersistStatusChangeInDb() {
        // 1. 准备
        Product p = new Product();
        p.setProductCode("P002");
        p.setProductName("测试产品2");
        p.setCategoryId(1L);
        p.setUnit("件");
        p.setStatus(1);
        productMapper.insert(p);
        Long productId = p.getProductId();

        // 2. 调 service.update 设 status=0
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setProductName("测试产品2");
        dto.setCategoryId(1L);
        dto.setUnit("件");
        dto.setSpec("标准");
        dto.setUpperLimit(BigDecimal.valueOf(100));
        dto.setLowerLimit(BigDecimal.valueOf(10));
        dto.setStatus(0);  // ← 禁用
        ProductVO vo = productService.update(productId, dto);
        System.out.println(">>> [update 测试] service.update 返回, vo.status=" + vo.getStatus());

        // 3. 直查 DB
        Integer statusInDb = jdbc.queryForObject(
                "SELECT status FROM t_product WHERE product_id = ?",
                Integer.class, productId);
        System.out.println(">>> [update 测试] DB 中 P002 的 status = " + statusInDb);
        assertEquals(0, statusInDb,
                "BUG 复现:ProductServiceImpl.update() 设 status=0 后,DB 应持久化 0");
    }
}