/**
 * 复现 bug:出库管理"提交出库单"功能异常
 *
 * 根因(已修复):
 *   1) create() 返回的 VO.details 为空(原 convertToVO(order) 没回填刚 insert 的 details)。
 *   2) 前端 OutboundFormView.vue payload 不含 locationId,导致出库明细 locationId 全为 NULL,
 *      confirm 阶段只能用 product.defaultLocationId 兜底,业务上"指定库位"语义错误。
 *   3) OrderNoGenerator Redis 主路径 % 10000,fallback 随机 4 位 → 撞 uk_order_no → 500。
 *   4) OutboundCreateDTO.customerId 缺 @NotNull,后端无校验。
 *
 * 修复后必须满足:
 *   - create() 返回 VO.details 非空
 *   - DTO detail.locationId 必须真实落库
 *   - Customer 不存在时抛 BusinessException
 */
package com.warehouse.service.impl;

import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.OutboundCreateDTO;
import com.warehouse.dto.request.OutboundDetailDTO;
import com.warehouse.dto.response.OutboundOrderVO;
import com.warehouse.entity.Customer;
import com.warehouse.entity.OutboundOrder;
import com.warehouse.entity.Product;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.CustomerMapper;
import com.warehouse.mapper.OutboundOrderMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.service.OutboundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("outbound-create-test")
class OutboundServiceImplCreateTest {

    @Autowired
    private OutboundService outboundService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private WarehouseLocationMapper locationMapper;

    @Autowired
    private OutboundOrderMapper outboundOrderMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    private Long customerId;
    private Long productId;
    private Long locationId;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_outbound_order_detail");
            stmt.execute("DROP TABLE IF EXISTS t_outbound_order");
            stmt.execute("DROP TABLE IF EXISTS t_warehouse_location");
            stmt.execute("DROP TABLE IF EXISTS t_product");
            stmt.execute("DROP TABLE IF EXISTS t_customer");
            stmt.execute("DROP TABLE IF EXISTS t_user");
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
            stmt.execute("INSERT INTO t_product_category (category_id, category_name) VALUES (1, '默认分类')");
            stmt.execute("CREATE TABLE t_user (" +
                    "  user_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  username VARCHAR(50) NOT NULL," +
                    "  password VARCHAR(100) NOT NULL," +
                    "  real_name VARCHAR(50)," +
                    "  phone VARCHAR(20)," +
                    "  email VARCHAR(100)," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
            stmt.execute("INSERT INTO t_user (user_id, username, password, real_name) VALUES (1, '1', 'test', '测试用户')");
            stmt.execute("CREATE TABLE t_customer (" +
                    "  customer_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  customer_code VARCHAR(30) NOT NULL," +
                    "  customer_name VARCHAR(100) NOT NULL," +
                    "  contact_person VARCHAR(50)," +
                    "  contact_phone VARCHAR(20)," +
                    "  contact_email VARCHAR(100)," +
                    "  address VARCHAR(200)," +
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
            stmt.execute("CREATE TABLE t_outbound_order (" +
                    "  order_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  order_no VARCHAR(30) NOT NULL," +
                    "  customer_id BIGINT," +
                    "  department VARCHAR(100)," +
                    "  applicant VARCHAR(50)," +
                    "  operator_id BIGINT," +
                    "  confirm_operator_id BIGINT," +
                    "  order_time DATETIME NOT NULL," +
                    "  confirm_time DATETIME," +
                    "  status TINYINT NOT NULL DEFAULT 0," +
                    "  remark VARCHAR(500)," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
            stmt.execute("CREATE TABLE t_outbound_order_detail (" +
                    "  detail_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  order_id BIGINT NOT NULL," +
                    "  product_id BIGINT NOT NULL," +
                    "  quantity DECIMAL(12,2) NOT NULL," +
                    "  unit_price DECIMAL(12,2)," +
                    "  location_id BIGINT," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
        }
        jdbc = new JdbcTemplate(dataSource);

        Customer customer = new Customer();
        customer.setCustomerCode("C-001");
        customer.setCustomerName("测试客户");
        customer.setStatus(1);
        customerMapper.insert(customer);
        customerId = customer.getCustomerId();

        WarehouseLocation location = new WarehouseLocation();
        location.setLocationCode("L-001");
        location.setLocationName("A-01-01");
        location.setStatus(1);
        locationMapper.insert(location);
        locationId = location.getLocationId();

        Product product = new Product();
        product.setProductCode("P-001");
        product.setProductName("测试商品");
        product.setCategoryId(1L);
        product.setUnit("件");
        product.setStatus(1);
        productMapper.insert(product);
        productId = product.getProductId();

        // 测试环境无 Spring Security 登录态,显式注入 Authentication 以满足 SecurityUtils.getCurrentUserId()
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null,
                        java.util.Collections.emptyList()));
    }

    /**
     * 复现 Bug 3 核心症状:create() 返回的 VO.details 为空。
     */
    @Test
    @DisplayName("P3-1 复现 + 修复:create() 返回的 VO.details 必须非空")
    void create_shouldReturnVoWithNonEmptyDetails() {
        OutboundCreateDTO dto = newOutboundDto(customerId, productId, locationId);

        OutboundOrderVO vo = outboundService.create(dto);

        assertNotNull(vo.getDetails(), "BUG:VO.details 为 null → 前端拿不到明细");
        assertEquals(1, vo.getDetails().size(),
                "BUG:VO.details.size()=" + vo.getDetails().size() + ",应为 1");
        assertEquals(productId, vo.getDetails().get(0).getProductId());
    }

    /**
     * 复现 Bug 3 独立根因:前端 payload 缺 locationId → 明细 locationId 为 NULL →
     * confirm 阶段只能兜底 default_location_id。修复后 DTO locationId 必须落库。
     */
    @Test
    @DisplayName("P3-2 复现 + 修复:create() 后明细 locationId 必须真实落库(不再丢字段)")
    void create_shouldPersistLocationIdInDetailRow() {
        OutboundCreateDTO dto = newOutboundDto(customerId, productId, locationId);

        OutboundOrderVO vo = outboundService.create(dto);

        Long actualLocationId = jdbc.queryForObject(
                "SELECT location_id FROM t_outbound_order_detail WHERE order_id = ?",
                Long.class, vo.getOrderId());
        assertEquals(locationId, actualLocationId,
                "BUG:locationId 未落库(前端缺 locationId 字段导致)");
    }

    @Test
    @DisplayName("create() 必须真实插入头表 + 明细表(同一事务)")
    void create_shouldPersistOrderAndDetailsInTransaction() {
        OutboundCreateDTO dto = newOutboundDto(customerId, productId, locationId);
        dto.setDepartment("仓储部");
        dto.setApplicant("张三");

        OutboundOrderVO vo = outboundService.create(dto);

        Long orderCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_outbound_order WHERE order_id = ?",
                Long.class, vo.getOrderId());
        assertEquals(1L, orderCount);
        Long detailCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_outbound_order_detail WHERE order_id = ?",
                Long.class, vo.getOrderId());
        assertEquals(1L, detailCount);
        String dept = jdbc.queryForObject(
                "SELECT department FROM t_outbound_order WHERE order_id = ?",
                String.class, vo.getOrderId());
        assertEquals("仓储部", dept);
        String applicant = jdbc.queryForObject(
                "SELECT applicant FROM t_outbound_order WHERE order_id = ?",
                String.class, vo.getOrderId());
        assertEquals("张三", applicant);
    }

    @Test
    @DisplayName("create() Product 不存在时必须抛 BusinessException(400),不能落入 500")
    void create_shouldThrowBusinessException_whenProductNotExists() {
        OutboundCreateDTO dto = newOutboundDto(customerId, 99999L, locationId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> outboundService.create(dto),
                "product 不存在必须抛 BusinessException");
        assertEquals(400, ex.getCode());
    }

    /**
     * 复现 Bug 3 修复:出库明细 locationId 为 NULL 时必须被 Service 拒绝,
     * 避免前端漏选库位时静默落到 DB NULL(原 confirm 阶段兜底扣错库位)。
     */
    @Test
    @DisplayName("create() 明细 locationId 为 NULL 时必须抛 BusinessException(400)")
    void create_shouldThrowBusinessException_whenDetailLocationIdNull() {
        OutboundCreateDTO dto = newOutboundDto(customerId, productId, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> outboundService.create(dto),
                "locationId 缺失必须抛 BusinessException(不能落库 NULL)");
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("库位"),
                "message 应说明是库位问题,实际: " + ex.getMessage());
    }

    /**
     * 复现 Bug 3 共同根因:OrderNoGenerator fallback 撞 uk_order_no。
     * 修复后 create() 内部重试,即使有同号订单也能成功创建。
     */
    @Test
    @DisplayName("P3-3 复现 + 修复:订单号撞号时 create() 必须自动重试,不抛 500")
    void create_shouldRetryOrderNo_whenCollisionDetected() {
        OutboundOrder existing = new OutboundOrder();
        existing.setOrderNo("CK19990101" + "000001");
        existing.setCustomerId(customerId);
        existing.setOperatorId(1L);
        existing.setOrderTime(LocalDateTime.now());
        existing.setStatus(0);
        outboundOrderMapper.insert(existing);

        OutboundCreateDTO dto = newOutboundDto(customerId, productId, locationId);

        OutboundOrderVO vo = outboundService.create(dto);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_outbound_order", Long.class);
        assertEquals(2L, total, "不应有 DuplicateKeyException 导致失败");
        assertNotNull(vo.getOrderNo());
    }

    private OutboundCreateDTO newOutboundDto(Long customerId, Long productId, Long locationId) {
        OutboundCreateDTO dto = new OutboundCreateDTO();
        dto.setCustomerId(customerId);
        dto.setOrderTime(LocalDateTime.now());
        dto.setRemark("test");
        OutboundDetailDTO d1 = new OutboundDetailDTO();
        d1.setProductId(productId);
        d1.setQuantity(new BigDecimal("5"));
        d1.setUnitPrice(new BigDecimal("9.99"));
        d1.setLocationId(locationId);
        dto.setDetails(List.of(d1));
        return dto;
    }
}
