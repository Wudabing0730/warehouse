/**
 * 复现 bug:仓位管理"新增仓位"功能异常
 *
 * 根因(已修复):
 *   1) LocationCreateDTO 缺 status 字段,前端 form.status 默认 1 被 Jackson 静默丢弃,
 *      依赖 Service 硬编码 setStatus(1),但 DTO/前端不一致会埋隐患。
 *   2) page() 手动 selectCount + wrapper.last("LIMIT ...") 与全局 PaginationInnerInterceptor
 *      双重拼接 LIMIT,触发 BadSqlGrammarException。
 *   3) 重复 location_code 提交触发 DuplicateKeyException,被 Exception 兜底吞成 500。
 *
 * 修复后必须满足:
 *   - DTO 含 status;Service null 兜底 1;DTO 传 0 也正确落库
 *   - page() 依赖全局拦截器,不重复拼接 LIMIT,total/records 正常
 *   - 重复编码抛 BusinessException(400)
 */
package com.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.LocationCreateDTO;
import com.warehouse.dto.request.LocationQueryDTO;
import com.warehouse.dto.response.LocationVO;
import com.warehouse.entity.WarehouseLocation;
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
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("location-create-test")
class WarehouseLocationServiceImplCreateTest {

    @Autowired
    private WarehouseLocationService locationService;

    @Autowired
    private WarehouseLocationMapper locationMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS t_warehouse_location");
            stmt.execute("CREATE TABLE t_warehouse_location (" +
                    "  location_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  location_code VARCHAR(30) NOT NULL," +
                    "  location_name VARCHAR(50) NOT NULL," +
                    "  zone VARCHAR(30)," +
                    "  status TINYINT NOT NULL DEFAULT 1," +
                    "  create_by BIGINT, update_by BIGINT," +
                    "  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time DATETIME)");
        }
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("create() 必须插入一行,DTO 不传 status 时 Service 兜底为 1(启用)")
    void create_shouldInsertRow_whenDtoValid() {
        LocationCreateDTO dto = new LocationCreateDTO();
        dto.setLocationCode("LOC-001");
        dto.setLocationName("A-01-01");
        dto.setZone("A");
        // status 不设置 → Service 应兜底 1

        LocationVO vo = locationService.create(dto);

        assertNotNull(vo.getLocationId(), "locationId 必须回填");
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_warehouse_location WHERE location_id = ?",
                Long.class, vo.getLocationId());
        assertEquals(1L, count, "必须真实插入 1 行");
        Integer statusInDb = jdbc.queryForObject(
                "SELECT status FROM t_warehouse_location WHERE location_id = ?",
                Integer.class, vo.getLocationId());
        assertEquals(1, statusInDb, "DTO 不传 status 时,Service 必须兜底为 1");
    }

    @Test
    @DisplayName("create() 必须尊重 DTO 显式传入的 status(0=禁用)")
    void create_shouldRespectStatusFromDto_whenDtoProvidesStatus() {
        LocationCreateDTO dto = new LocationCreateDTO();
        dto.setLocationCode("LOC-002");
        dto.setLocationName("A-01-02");
        dto.setZone("A");
        dto.setStatus(0);  // 显式禁用

        LocationVO vo = locationService.create(dto);

        Integer statusInDb = jdbc.queryForObject(
                "SELECT status FROM t_warehouse_location WHERE location_id = ?",
                Integer.class, vo.getLocationId());
        assertEquals(0, statusInDb, "DTO 传 status=0 必须落库为 0,不能被 Service 强行覆盖为 1");
    }

    @Test
    @DisplayName("create() 重复编码必须抛 BusinessException(400)而不是 500")
    void create_shouldThrowBusinessException_whenCodeDuplicate() {
        LocationCreateDTO dto = new LocationCreateDTO();
        dto.setLocationCode("LOC-DUP");
        dto.setLocationName("A-01-DUP");
        dto.setStatus(1);
        locationService.create(dto);

        // 第二次相同编码必须被业务层挡住
        BusinessException ex = assertThrows(BusinessException.class,
                () -> locationService.create(dto),
                "重复编码必须抛 BusinessException(不能落到 DuplicateKeyException → 500)");
        assertEquals(400, ex.getCode(), "业务异常 code 应为 400");
        assertTrue(ex.getMessage().contains("库位编码已存在"),
                "异常 message 必须可读,实际: " + ex.getMessage());
    }

    /**
     * 复现 bug:旧 page() 手动 selectCount + wrapper.last("LIMIT ...") 与全局
     * PaginationInnerInterceptor 双重拼接 LIMIT,触发 BadSqlGrammarException。
     * 修复后必须依赖全局拦截器,total 和 records 都正确。
     */
    @Test
    @DisplayName("page() 必须依赖全局 PaginationInnerInterceptor,不抛 BadSqlGrammarException")
    void page_shouldNotThrowBadSql_whenInterceptorActive() {
        // 插入 3 条数据
        for (int i = 1; i <= 3; i++) {
            WarehouseLocation loc = new WarehouseLocation();
            loc.setLocationCode("P-" + i);
            loc.setLocationName("page-test-" + i);
            loc.setStatus(1);
            locationMapper.insert(loc);
        }

        LocationQueryDTO query = new LocationQueryDTO();
        Page<WarehouseLocation> page = new Page<>(1, 2);
        var voPage = locationService.page(page, query);

        assertEquals(3L, voPage.getTotal(),
                "BUG:total 必须等于 3(全局拦截器 COUNT(*) + LIMIT 协同工作),实际: " + voPage.getTotal());
        assertEquals(2, voPage.getRecords().size(),
                "第 1 页 2 条记录必须正确返回,实际: " + voPage.getRecords().size());
    }
}
