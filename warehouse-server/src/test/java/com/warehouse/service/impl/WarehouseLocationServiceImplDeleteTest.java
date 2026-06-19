/**
 * 复现 bug:WarehouseLocationServiceImpl.delete(id) 只把 status 置 0,行仍在 DB
 * 修复后:
 *   1) 没有产品引用时:LocationServiceImpl.delete(id) 必须真实删除该行
 *   2) 有产品引用时:LocationServiceImpl.delete(id) 必须抛 BusinessException 且不删除行(FK 引用检查保留)
 *
 * 参照:ProductCategoryServiceImpl.delete() 的硬删除 + FK 检查模式
 */
package com.warehouse.service.impl;

import com.warehouse.common.BusinessException;
import com.warehouse.entity.Product;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.service.WarehouseLocationService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("location-delete-test")
class WarehouseLocationServiceImplDeleteTest {

    @Autowired
    private WarehouseLocationService locationService;

    @Autowired
    private WarehouseLocationMapper locationMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_product_category");
            stmt.execute("DROP TABLE IF EXISTS t_product");
            stmt.execute("DROP TABLE IF EXISTS t_warehouse_location");
            stmt.execute("CREATE TABLE t_product_category (" +
                    "  category_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  category_name VARCHAR(50) NOT NULL," +
                    "  parent_id BIGINT," +
                    "  sort_order INT," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
            stmt.execute("CREATE TABLE t_warehouse_location (" +
                    "  location_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  location_code VARCHAR(30) NOT NULL," +
                    "  location_name VARCHAR(50) NOT NULL," +
                    "  zone VARCHAR(30)," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
            stmt.execute("CREATE TABLE t_product (" +
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
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
            // 默认 category,供 product FK 校验使用
            stmt.execute("INSERT INTO t_product_category (category_id, category_name) VALUES (1, '默认分类')");
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("WarehouseLocationServiceImpl.delete(id) 无引用时必须真实删除该行")
    void delete_mustRemoveRowFromDb_whenNoProductReferences() {
        WarehouseLocation loc = new WarehouseLocation();
        loc.setLocationCode("LOC001");
        loc.setLocationName("A-01-01");
        loc.setZone("A");
        loc.setStatus(1);
        locationMapper.insert(loc);
        Long locationId = loc.getLocationId();
        assertNotNull(locationId);

        locationService.delete(locationId);

        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_warehouse_location WHERE location_id = ?",
                Long.class, locationId);
        assertEquals(0L, countAfter,
                "BUG:删除后该行仍在 DB(count=" + countAfter + "),说明 delete() 还在走软禁用");
    }

    @Test
    @DisplayName("WarehouseLocationServiceImpl.delete(id) 有产品引用时必须抛 BusinessException 且不删除行")
    void delete_mustThrowAndPreserveRow_whenProductReferencesLocation() {
        // 1. 准备库位
        WarehouseLocation loc = new WarehouseLocation();
        loc.setLocationCode("LOC002");
        loc.setLocationName("A-01-02");
        loc.setZone("A");
        loc.setStatus(1);
        locationMapper.insert(loc);
        Long locationId = loc.getLocationId();

        // 2. 插入一个产品,引用该库位
        Product p = new Product();
        p.setProductCode("P-LOC-REF");
        p.setProductName("引用库位的产品");
        p.setCategoryId(1L);
        p.setUnit("件");
        p.setDefaultLocationId(locationId);
        p.setStatus(1);
        productMapper.insert(p);

        // 3. 调 delete 应抛 BusinessException(FK 引用检查保留)
        assertThrows(BusinessException.class, () -> locationService.delete(locationId),
                "有产品引用时,delete() 必须抛 BusinessException");

        // 4. 验证行依然存在
        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_warehouse_location WHERE location_id = ?",
                Long.class, locationId);
        assertEquals(1L, countAfter,
                "FK 检查命中后,该库位必须仍在 DB(不应被删除)");
    }

    @Test
    @DisplayName("WarehouseLocationServiceImpl.delete(id) 不存在时必须抛 BusinessException")
    void delete_mustThrow_whenLocationNotFound() {
        assertThrows(BusinessException.class, () -> locationService.delete(99999L),
                "删除不存在的库位必须抛 BusinessException(404)");
    }
}