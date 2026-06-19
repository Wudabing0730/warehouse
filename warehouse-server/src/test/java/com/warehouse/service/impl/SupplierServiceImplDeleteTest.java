/**
 * 复现 bug:SupplierServiceImpl.delete(id) 只把 status 置 0,行仍在 DB
 * 修复后:SupplierServiceImpl.delete(id) 必须真实删除该行
 *
 * 参照:UserServiceImpl.delete() / RoleServiceImpl.delete() 的硬删除实现
 */
package com.warehouse.service.impl;

import com.warehouse.entity.Supplier;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.service.SupplierService;
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

@SpringBootTest
@ActiveProfiles("supplier-delete-test")
class SupplierServiceImplDeleteTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_supplier");
            stmt.execute("CREATE TABLE t_supplier (" +
                    "  supplier_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  supplier_code VARCHAR(30) NOT NULL," +
                    "  supplier_name VARCHAR(100) NOT NULL," +
                    "  contact_person VARCHAR(50)," +
                    "  contact_phone VARCHAR(20)," +
                    "  contact_email VARCHAR(100)," +
                    "  address VARCHAR(255)," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("SupplierServiceImpl.delete(id) 必须真实删除该行")
    void delete_mustRemoveRowFromDb() {
        Supplier s = new Supplier();
        s.setSupplierCode("SUP001");
        s.setSupplierName("测试供应商");
        s.setStatus(1);
        supplierMapper.insert(s);
        Long supplierId = s.getSupplierId();
        assertNotNull(supplierId);

        supplierService.delete(supplierId);

        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_supplier WHERE supplier_id = ?",
                Long.class, supplierId);
        assertEquals(0L, countAfter,
                "BUG:删除后该行仍在 DB(count=" + countAfter + "),说明 delete() 还在走软禁用");
    }
}