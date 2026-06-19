/**
 * 真实集成测试:实际执行 UPDATE 并断言数据库行被修改
 *
 * 用户报告:"禁用 P001 等所有数据更改操作都无法正常持久化,前端显示成功但数据不变"
 *
 * 之前怀疑 1:application.yml 全局 logic-delete-field 配置导致 SQL 报 Unknown column 'deleted'
 *   - 已通过 MybatisPlusLogicalDeleteConfigConsistencyTest 验证并修复
 *   - 但用户反馈"还是不行"
 *
 * 现在要排除其他可能:
 *   可能 2:Product 实体 @TableId 在 productId 上,但 baseMapper.updateById 用的 SQL
 *          是 WHERE product_id = ? 还是 WHERE id = ? —— 列名错配导致更新 0 行
 *   可能 3:@Transactional 没生效,事务回滚
 *   可能 4:操作日志 AOP 异常回滚业务事务
 *   可能 5:BeanUtils.copyProperties 没把 status 字段从 DTO 拷到 entity
 *   可能 6:实体字段没有 @TableField,导致默认 column 名推断错误
 *   可能 7:unique key 冲突或外键约束失败
 *   可能 8:MyBatis-Plus 默认 updateStrategy / insertStrategy 字段策略导致字段被排除
 *
 * 策略:用 H2 真实数据库 + @SpringBootTest,创建与生产相同的 t_product schema,
 *       INSERT 一条 P001,UPDATE status=0,再 SELECT 验证 —— 直接观察真实 SQL。
 */
package com.warehouse.sql;

import com.warehouse.entity.Product;
import com.warehouse.mapper.ProductMapper;
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

/**
 * 不带 @Transactional,确保每次测试干净
 */
@SpringBootTest
@ActiveProfiles("product-test")
class ProductUpdateSqlIntegrationTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        // 每次测试前重建表
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_product");
            // 与生产 sql/01-schema.sql 一致的 t_product 结构
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
            // 创建 t_product_category 满足外键(其实上面没加 FK,但防止其他配置加)
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("真实 UPDATE 测试:MyBatis-Plus updateById 是否真的把 status=0 持久化到数据库")
    void updateByIdMustActuallyPersistToDatabase() {
        // 1. 直接 INSERT 一条 P001
        Product p = new Product();
        p.setProductCode("P001");
        p.setProductName("测试产品");
        p.setCategoryId(1L);
        p.setUnit("件");
        p.setSpec("标准");
        p.setUpperLimit(BigDecimal.valueOf(100));
        p.setLowerLimit(BigDecimal.valueOf(10));
        p.setStatus(1);
        int inserted = productMapper.insert(p);
        assertEquals(1, inserted, "INSERT 应返回 1");
        assertNotNull(p.getProductId(), "INSERT 后应回填 productId");
        Long productId = p.getProductId();
        System.out.println(">>> INSERT 成功,productId = " + productId);

        // 2. SELECT 验证初始数据
        Product loaded = productMapper.selectById(productId);
        assertNotNull(loaded, "SELECT 应能找到刚插入的产品");
        assertEquals(1, loaded.getStatus(), "初始 status 应为 1");
        System.out.println(">>> SELECT 初始: status = " + loaded.getStatus());

        // 3. UPDATE status=0(模拟"禁用"操作)
        loaded.setStatus(0);
        int updated = productMapper.updateById(loaded);
        System.out.println(">>> UPDATE 影响行数: " + updated);
        // MyBatis-Plus 在某些情况下返回 0(如果行没有变化),所以不强制断言 updated == 1
        // 但要断言它"执行了 SQL",所以后面 SELECT 验证

        // 4. 重新 SELECT 验证 status 真的变为 0
        Product after = productMapper.selectById(productId);
        assertNotNull(after, "UPDATE 后 SELECT 应能找到产品");
        System.out.println(">>> UPDATE 后 status = " + after.getStatus());
        System.out.println(">>> UPDATE 后 updateTime = " + after.getUpdateTime());
        assertEquals(0, after.getStatus(),
                "BUG 复现:UPDATE status=0 后,数据库应持久化。但当前 status=" + after.getStatus());
    }

    @Test
    @DisplayName("直接用 JdbcTemplate 验证 SQL 是否真的执行")
    void rawJdbcUpdateWorksAsControl() {
        jdbc.update("INSERT INTO t_product (product_code, product_name, category_id, unit, status) " +
                "VALUES (?, ?, ?, ?, ?)", "P002", "控制组产品", 1L, "件", 1);
        Long id = jdbc.queryForObject("SELECT product_id FROM t_product WHERE product_code = 'P002'", Long.class);
        assertNotNull(id);

        jdbc.update("UPDATE t_product SET status = 0 WHERE product_id = ?", id);

        Integer status = jdbc.queryForObject(
                "SELECT status FROM t_product WHERE product_id = ?", Integer.class, id);
        assertEquals(0, status, "原生 JDBC UPDATE 应正常工作(对照组)");
    }
}