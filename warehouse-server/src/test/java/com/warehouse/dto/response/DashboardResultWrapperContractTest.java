/**
 * Bug 复现验证:DashboardController 返回的 Result<DashboardSummaryVO> 包装 JSON 契约
 *
 * 现象:前端 DashboardView 显示所有数据为 0 + ECharts 图表空白。
 *      根因排查需要确认:前端 fetchDashboardData 拿到的 res.data 是完整的 VO 对象。
 *      Result.success(vo) 序列化为 {code, message, data, timestamp},
 *      其中 data 字段必须含全部前端消费字段(productCount/totalStock/.../trendDates)。
 *
 * 修复:确认后端契约正确;前端 catch 静默吞错是真正的 bug(单独测试覆盖)。
 */
package com.warehouse.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 DashboardController 返回的 Result.success(summary) 序列化结构。
 * 这是前后端 API 契约的最终一道关卡 — 前端从 response.data.data 读取 VO。
 */
class DashboardResultWrapperContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Result.success(summary) 序列化的 JSON 顶层必须有 code/message/data/timestamp")
    void resultWrapper_topLevelFields() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setProductCount(10L);
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode json = mapper.valueToTree(result);

        assertNotNull(json.get("code"), "Result 必须含 code 字段");
        assertNotNull(json.get("message"), "Result 必须含 message 字段");
        assertNotNull(json.get("data"), "Result 必须含 data 字段");
        assertNotNull(json.get("timestamp"), "Result 必须含 timestamp 字段");
        assertEquals(200, json.get("code").asInt(), "成功响应 code 应为 200");
    }

    @Test
    @DisplayName("Result.data 内层 VO 必须包含前端消费的全部统计字段(productCount/totalStock/todayInbound/todayOutbound)")
    void resultWrapper_data_containsAllStatFields() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setProductCount(156L);
        vo.setTotalStock(8420L);
        vo.setTodayInbound(12L);
        vo.setTodayOutbound(8L);
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode json = mapper.valueToTree(result);
        JsonNode data = json.get("data");
        assertNotNull(data, "Result.data 不能为 null");
        assertTrue(data.has("productCount"), "前端需要 productCount");
        assertTrue(data.has("totalStock"), "前端需要 totalStock");
        assertTrue(data.has("todayInbound"), "前端需要 todayInbound");
        assertTrue(data.has("todayOutbound"), "前端需要 todayOutbound");
        assertEquals(156L, data.get("productCount").asLong());
        assertEquals(8420L, data.get("totalStock").asLong());
        assertEquals(12L, data.get("todayInbound").asLong());
        assertEquals(8L, data.get("todayOutbound").asLong());
    }

    @Test
    @DisplayName("Result.data.alerts 和 recentOps 必须是数组(可空)")
    void resultWrapper_data_alertsAndRecentOpsAreArrays() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setAlerts(Arrays.asList("商品A库存低于下限"));
        DashboardSummaryVO.RecentOperationVO op = new DashboardSummaryVO.RecentOperationVO();
        op.setTime("14:30");
        op.setDescription("入库");
        op.setUser("张三");
        vo.setRecentOps(Arrays.asList(op));
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode data = mapper.valueToTree(result).get("data");

        assertTrue(data.get("alerts").isArray(), "alerts 应为数组");
        assertEquals(1, data.get("alerts").size());
        assertTrue(data.get("recentOps").isArray(), "recentOps 应为数组");
        assertEquals(1, data.get("recentOps").size());
        assertEquals("14:30", data.get("recentOps").get(0).get("time").asText());
    }

    @Test
    @DisplayName("Result.data.alerts 和 recentOps 字段缺失/为 null 时不能使 JSON 抛错")
    void resultWrapper_data_missingArrayFieldsSafe() throws Exception {
        // 模拟空数据库情况:DashboardServiceImpl 可能没设这些字段
        DashboardSummaryVO vo = new DashboardSummaryVO();
        // 不设 alerts / recentOps
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode data = mapper.valueToTree(result).get("data");
        assertNotNull(data);
        // 即使为 null/缺失,前端 res?.data?.alerts ?? [] 也要能兜底
        boolean alertsIsNullOrMissing =
                !data.has("alerts") || data.get("alerts").isNull();
        assertTrue(alertsIsNullOrMissing,
                "alerts 字段缺失/null 时前端能 fallback,后端不应硬塞空数组");
    }

    @Test
    @DisplayName("Result.data.trendDates 必须是长度为 7 的数组(供 ECharts x 轴)")
    void resultWrapper_data_trendDatesLengthIs7() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        List<String> dates = new ArrayList<>();
        DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).format(FMT));
        }
        vo.setTrendDates(dates);
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode data = mapper.valueToTree(result).get("data");
        assertTrue(data.has("trendDates"), "前端 ECharts 需要 trendDates");
        assertEquals(7, data.get("trendDates").size(),
                "trendDates 必须恰好 7 个日期,缺一 ECharts x 轴标签错位");
    }

    @Test
    @DisplayName("Result.data.inboundTrend / outboundTrend 必须是长度为 7 的数组(供 ECharts series)")
    void resultWrapper_data_trendCountsLengthIs7() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        List<Long> inbound = Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L);
        List<Long> outbound = Arrays.asList(6L, 5L, 4L, 3L, 2L, 1L, 0L);
        vo.setInboundTrend(inbound);
        vo.setOutboundTrend(outbound);
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode data = mapper.valueToTree(result).get("data");
        assertEquals(7, data.get("inboundTrend").size());
        assertEquals(7, data.get("outboundTrend").size());
        assertEquals(6L, data.get("inboundTrend").get(6).asLong(),
                "ECharts 最后一个数据点应对应今天");
    }

    @Test
    @DisplayName("前端解包路径 res?.data ?? res 在 Result 包装下能正确取到 VO")
    void resultWrapper_simulatesFrontendUnwrap() throws Exception {
        // 模拟前端 DashboardView.vue 第 184 行: const summary = res?.data ?? res
        // 流程: axios response.data = Result JSON,前端 res = Result,res.data = VO
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setProductCount(99L);
        vo.setTotalStock(1234L);
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode res = mapper.valueToTree(result);             // axios 解包后
        JsonNode summary = res.has("data") && !res.get("data").isNull()
                ? res.get("data") : res;                       // 前端逻辑
        assertNotNull(summary);
        assertTrue(summary.has("productCount"),
                "前端 res?.data ?? res 必须能取到 VO,否则 stats 全 0");
        assertEquals(99L, summary.get("productCount").asLong());
    }

    @Test
    @DisplayName("Result.success 返回后 message 字段是中文成功提示,便于前端调试")
    void resultWrapper_successMessageIsHumanReadable() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        Result<DashboardSummaryVO> result = Result.success(vo);

        JsonNode json = mapper.valueToTree(result);
        assertNotNull(json.get("message"));
        assertFalse(json.get("message").asText().isEmpty(),
                "成功 message 不能为空,前端无 message 时排错困难");
    }
}