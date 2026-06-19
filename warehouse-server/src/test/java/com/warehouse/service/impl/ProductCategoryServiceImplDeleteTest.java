/**
 * 为 ProductCategoryServiceImpl.delete() 补充硬删除回归测试。
 *
 * 背景:
 *   ProductCategoryServiceImpl.delete() 是 d10375b 修复时作为"参照实现"被保留的
 *   (子类别检查 + 产品引用检查 + baseMapper.deleteById 真删),但当时没有补测试守护。
 *   本测试对齐 d10375b 模式 (H2 MySQL 模式 + JdbcTemplate 直查 DB),
 *   覆盖 delete() 的 3 道业务防线:
 *     1) 无引用时:必须真实删除该行
 *     2) 有子类别引用时:必须抛 BusinessException 且不删除行
 *     3) 有产品引用时:必须抛 BusinessException 且不删除行
 *     4) 不存在时:必须抛 BusinessException (404)
 */
package com.warehouse.service.impl;

import com.warehouse.common.BusinessException;
import com.warehouse.entity.Product;
import com.warehouse.entity.ProductCategory;
import com.warehouse.mapper.ProductCategoryMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.service.ProductCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("category-delete-test")
class ProductCategoryServiceImplDeleteTest {

    @Autowired
    private ProductCategoryService categoryService;

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // 必须在 category 前 drop(子表先于父表)
            stmt.execute("DROP TABLE IF EXISTS t_product");
            stmt.execute("DROP TABLE IF EXISTS t_product_category");
            stmt.execute("CREATE TABLE t_product_category (" +
                    "  category_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  category_name VARCHAR(50) NOT NULL," +
                    "  parent_id BIGINT," +
                    "  sort_order INT," +
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
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("ProductCategoryServiceImpl.delete(id) 无引用时必须真实删除该行")
    void delete_mustRemoveRowFromDb_whenNoReferences() {
        ProductCategory cat = new ProductCategory();
        cat.setCategoryName("测试分类-无引用");
        cat.setStatus(1);
        categoryMapper.insert(cat);
        Long categoryId = cat.getCategoryId();
        assertNotNull(categoryId);

        categoryService.delete(categoryId);

        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_product_category WHERE category_id = ?",
                Long.class, categoryId);
        assertEquals(0L, countAfter,
                "BUG:删除后该行仍在 DB(count=" + countAfter + "),说明 delete() 还在走软禁用");
    }

    @Test
    @DisplayName("ProductCategoryServiceImpl.delete(id) 有子类别引用时必须抛 BusinessException 且不删除行")
    void delete_mustThrowAndPreserveRow_whenChildrenExist() {
        // 1. 准备父类别
        ProductCategory parent = new ProductCategory();
        parent.setCategoryName("父分类-有子");
        parent.setStatus(1);
        categoryMapper.insert(parent);
        Long parentId = parent.getCategoryId();
        assertNotNull(parentId);

        // 2. 插入子类别,parent_id 指向父
        ProductCategory child = new ProductCategory();
        child.setCategoryName("子分类-引用父");
        child.setParentId(parentId);
        child.setStatus(1);
        categoryMapper.insert(child);

        // 3. 调 delete 应抛 BusinessException (子类别引用检查)
        assertThrows(BusinessException.class, () -> categoryService.delete(parentId),
                "有子类别引用时,delete() 必须抛 BusinessException");

        // 4. 验证父类别行依然存在
        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_product_category WHERE category_id = ?",
                Long.class, parentId);
        assertEquals(1L, countAfter,
                "FK 检查命中后,该父类别必须仍在 DB(不应被删除)");
    }

    @Test
    @DisplayName("ProductCategoryServiceImpl.delete(id) 有产品引用时必须抛 BusinessException 且不删除行")
    void delete_mustThrowAndPreserveRow_whenProductsExist() {
        // 1. 准备类别
        ProductCategory cat = new ProductCategory();
        cat.setCategoryName("测试分类-有产品");
        cat.setStatus(1);
        categoryMapper.insert(cat);
        Long categoryId = cat.getCategoryId();
        assertNotNull(categoryId);

        // 2. 插入产品,引用该类别
        Product p = new Product();
        p.setProductCode("P-CAT-REF");
        p.setProductName("引用分类的产品");
        p.setCategoryId(categoryId);
        p.setUnit("件");
        p.setStatus(1);
        productMapper.insert(p);

        // 3. 调 delete 应抛 BusinessException (产品引用检查)
        assertThrows(BusinessException.class, () -> categoryService.delete(categoryId),
                "有产品引用时,delete() 必须抛 BusinessException");

        // 4. 验证类别行依然存在
        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_product_category WHERE category_id = ?",
                Long.class, categoryId);
        assertEquals(1L, countAfter,
                "FK 检查命中后,该类别必须仍在 DB(不应被删除)");
    }

    @Test
    @DisplayName("ProductCategoryServiceImpl.delete(id) 不存在时必须抛 BusinessException")
    void delete_mustThrow_whenCategoryNotFound() {
        assertThrows(BusinessException.class, () -> categoryService.delete(99999L),
                "删除不存在的类别必须抛 BusinessException(404)");
    }
}
