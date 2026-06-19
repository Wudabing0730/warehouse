/**
 * 复现 bug:CustomerServiceImpl.delete(id) 只把 status 置 0,行仍在 DB
 * 修复后:CustomerServiceImpl.delete(id) 必须真实删除该行
 *
 * 参照:UserServiceImpl.delete() / RoleServiceImpl.delete() 的硬删除实现
 */
package com.warehouse.service.impl;

import com.warehouse.entity.Customer;
import com.warehouse.mapper.CustomerMapper;
import com.warehouse.service.CustomerService;
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
@ActiveProfiles("customer-delete-test")
class CustomerServiceImplDeleteTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_customer");
            stmt.execute("CREATE TABLE t_customer (" +
                    "  customer_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  customer_code VARCHAR(30) NOT NULL," +
                    "  customer_name VARCHAR(100) NOT NULL," +
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
    @DisplayName("CustomerServiceImpl.delete(id) 必须真实删除该行")
    void delete_mustRemoveRowFromDb() {
        Customer c = new Customer();
        c.setCustomerCode("CUS001");
        c.setCustomerName("测试客户");
        c.setStatus(1);
        customerMapper.insert(c);
        Long customerId = c.getCustomerId();
        assertNotNull(customerId);

        customerService.delete(customerId);

        Long countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_customer WHERE customer_id = ?",
                Long.class, customerId);
        assertEquals(0L, countAfter,
                "BUG:删除后该行仍在 DB(count=" + countAfter + "),说明 delete() 还在走软禁用");
    }
}