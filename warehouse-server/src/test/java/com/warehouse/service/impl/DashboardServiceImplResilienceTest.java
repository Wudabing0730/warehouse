/**
 * Bug 复现验证:DashboardServiceImpl 必须对每个 mapper 调用做 try/catch 降级
 *
 * Bug 复现:
 *   用户截图显示 dashboard 全 0 + ECharts 空白。
 *   真实根因:DashboardServiceImpl.summary() 后端返回 500。
 *   DashboardServiceImpl 有 7 个数据源调用,任何一个抛异常就会让整个 API 返回 500,
 *   前端 catch 显示 ElMessage 但用户看不到真实数据。
 *
 * 修复:每个 mapper 调用包 try/catch,失败时:
 *   1. log.error("DashboardServiceImpl 某 mapper 失败:{}", e.getMessage())
 *   2. 返回降级值(0/空数组),不抛异常
 *   3. summary() 整体仍返回完整 DashboardSummaryVO,HTTP 200
 */
package com.warehouse.service.impl;

import com.warehouse.dto.response.DashboardSummaryVO;
import com.warehouse.entity.OperationLog;
import com.warehouse.entity.Stock;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.OperationLogMapper;
import com.warehouse.mapper.OutboundOrderMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DashboardServiceImpl 弹性测试:任一 mapper 抛异常都不能让整个 API 500。
 * 每个测试独立 setUp,避免 stubbing 冲突。
 */
class DashboardServiceImplResilienceTest {

    private ProductMapper productMapper;
    private StockMapper stockMapper;
    private InboundOrderMapper inboundOrderMapper;
    private OutboundOrderMapper outboundOrderMapper;
    private OperationLogMapper operationLogMapper;
    private StringRedisTemplate stringRedisTemplate;
    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        stockMapper = mock(StockMapper.class);
        inboundOrderMapper = mock(InboundOrderMapper.class);
        outboundOrderMapper = mock(OutboundOrderMapper.class);
        operationLogMapper = mock(OperationLogMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);

        service = new DashboardServiceImpl(
                productMapper, stockMapper, inboundOrderMapper,
                outboundOrderMapper, operationLogMapper, stringRedisTemplate);
    }

    /** 模拟 6 个数据源全部正常工作 — 用于 happy path 测试 */
    private void stubAllHealthy() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        OperationLog log = new OperationLog();
        log.setOperation("测试操作");
        log.setUsername("admin");
        log.setCreateTime(java.time.LocalDateTime.now());
        when(operationLogMapper.selectList(any())).thenReturn(Collections.singletonList(log));
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(any(), anyLong(), anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("happy path:全部 mapper 正常时,summary() 返回真实数据")
    void happyPath() {
        stubAllHealthy();
        DashboardSummaryVO vo = service.summary();
        assertNotNull(vo);
        assertEquals(20L, vo.getProductCount());
        assertEquals(100L, vo.getTotalStock());
        assertEquals(4L, vo.getTodayInbound());
        assertEquals(3L, vo.getTodayOutbound());
        assertNotNull(vo.getRecentOps());
        assertEquals(1, vo.getRecentOps().size());
    }

    @Test
    @DisplayName("productMapper 抛异常时,productCount 降级为 0,不抛异常给前端")
    void productMapperFailureShouldDegrade() {
        when(productMapper.selectCount(any())).thenThrow(new RuntimeException("table t_product not found"));
        stubOthersHealthy_exceptProduct();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertEquals(0L, vo.getProductCount(),
                "productMapper 失败时 productCount 应降级为 0");
        assertEquals(100L, vo.getTotalStock(), "其他统计不受影响");
    }

    @Test
    @DisplayName("stockMapper 抛异常时,totalStock 降级为 0")
    void stockMapperFailureShouldDegrade() {
        when(stockMapper.selectList(any())).thenThrow(new RuntimeException("t_stock OOM"));
        stubOthersHealthy_exceptStock();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertEquals(0L, vo.getTotalStock());
        assertEquals(20L, vo.getProductCount());
    }

    @Test
    @DisplayName("inboundOrderMapper 抛异常时,todayInbound 降级为 0")
    void inboundOrderMapperFailureShouldDegrade() {
        when(inboundOrderMapper.selectCount(any())).thenThrow(new RuntimeException("inbound timeout"));
        stubOthersHealthy_exceptInbound();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertEquals(0L, vo.getTodayInbound());
        assertEquals(3L, vo.getTodayOutbound());
    }

    @Test
    @DisplayName("outboundOrderMapper 抛异常时,todayOutbound 降级为 0")
    void outboundOrderMapperFailureShouldDegrade() {
        when(outboundOrderMapper.selectCount(any())).thenThrow(new RuntimeException("outbound timeout"));
        stubOthersHealthy_exceptOutbound();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertEquals(0L, vo.getTodayOutbound());
        assertEquals(4L, vo.getTodayInbound());
    }

    @Test
    @DisplayName("operationLogMapper 抛异常时,recentOps 降级为空数组")
    void operationLogMapperFailureShouldDegrade() {
        when(operationLogMapper.selectList(any())).thenThrow(new RuntimeException("operation_log missing"));
        stubOthersHealthy_exceptOpLog();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertNotNull(vo.getRecentOps());
        assertTrue(vo.getRecentOps().isEmpty(),
                "operationLogMapper 失败时 recentOps 降级为空");
    }

    @Test
    @DisplayName("Redis 连接失败时,alerts 降级为空数组(防御回归)")
    void redisFailureShouldDegrade() {
        when(stringRedisTemplate.opsForList()).thenThrow(new RedisConnectionFailureException("Redis down"));
        stubOthersHealthy_exceptRedis();

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertNotNull(vo.getAlerts());
        assertTrue(vo.getAlerts().isEmpty());
    }

    @Test
    @DisplayName("全部 mapper 抛异常时,summary() 仍返回完整 VO,所有 stat=0,trend 长度=7 全为 0")
    void allMappersFailureShouldStillReturnValidVO() {
        when(productMapper.selectCount(any())).thenThrow(new RuntimeException("a"));
        when(stockMapper.selectList(any())).thenThrow(new RuntimeException("b"));
        when(inboundOrderMapper.selectCount(any())).thenThrow(new RuntimeException("c"));
        when(outboundOrderMapper.selectCount(any())).thenThrow(new RuntimeException("d"));
        when(operationLogMapper.selectList(any())).thenThrow(new RuntimeException("e"));
        when(stringRedisTemplate.opsForList()).thenThrow(new RedisConnectionFailureException("g"));

        DashboardSummaryVO vo = assertDoesNotThrow(() -> service.summary());
        assertNotNull(vo);
        assertEquals(0L, vo.getProductCount());
        assertEquals(0L, vo.getTotalStock());
        assertEquals(0L, vo.getTodayInbound());
        assertEquals(0L, vo.getTodayOutbound());
        assertNotNull(vo.getRecentOps());
        assertTrue(vo.getRecentOps().isEmpty());
        assertNotNull(vo.getAlerts());
        assertTrue(vo.getAlerts().isEmpty());
        assertNotNull(vo.getTrendDates());
        assertEquals(7, vo.getTrendDates().size());
        assertNotNull(vo.getInboundTrend());
        assertEquals(7, vo.getInboundTrend().size());
        assertNotNull(vo.getOutboundTrend());
        assertEquals(7, vo.getOutboundTrend().size());
        for (Long v : vo.getInboundTrend()) {
            assertEquals(0L, v);
        }
        for (Long v : vo.getOutboundTrend()) {
            assertEquals(0L, v);
        }
    }

    // === 辅助:除指定 mapper 外其他都 stub 正常 ===

    private void stubOthersHealthy_exceptProduct() {
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        stubOpLogAndRedis();
    }

    private void stubOthersHealthy_exceptStock() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        stubOpLogAndRedis();
    }

    private void stubOthersHealthy_exceptInbound() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        stubOpLogAndRedis();
    }

    private void stubOthersHealthy_exceptOutbound() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        stubOpLogAndRedis();
    }

    private void stubOthersHealthy_exceptOpLog() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(any(), anyLong(), anyLong())).thenReturn(Collections.emptyList());
    }

    private void stubOthersHealthy_exceptRedis() {
        when(productMapper.selectCount(any())).thenReturn(20L);
        Stock s = new Stock();
        s.setQuantity(new BigDecimal("100"));
        when(stockMapper.selectList(any())).thenReturn(Collections.singletonList(s));
        when(inboundOrderMapper.selectCount(any())).thenReturn(4L);
        when(outboundOrderMapper.selectCount(any())).thenReturn(3L);
        OperationLog log = new OperationLog();
        log.setOperation("测试操作");
        log.setUsername("admin");
        log.setCreateTime(java.time.LocalDateTime.now());
        when(operationLogMapper.selectList(any())).thenReturn(Collections.singletonList(log));
    }

    private void stubOpLogAndRedis() {
        OperationLog log = new OperationLog();
        log.setOperation("测试操作");
        log.setUsername("admin");
        log.setCreateTime(java.time.LocalDateTime.now());
        when(operationLogMapper.selectList(any())).thenReturn(Collections.singletonList(log));
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(any(), anyLong(), anyLong())).thenReturn(Collections.emptyList());
    }
}