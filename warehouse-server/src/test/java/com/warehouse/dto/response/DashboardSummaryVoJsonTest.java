package com.warehouse.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-8 修复验证:DashboardSummaryVO 的 JSON 契约必须包含前端 DashboardView 消费的 4 个统计字段
 * + alerts + recentOps,前端不再依赖硬编码假数据。
 */
class DashboardSummaryVoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("DashboardSummaryVO JSON 必须包含 productCount / totalStock / todayInbound / todayOutbound / alerts / recentOps")
    void jsonContractContainsAllDashboardFields() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setProductCount(156L);
        vo.setTotalStock(8420L);
        vo.setTodayInbound(12L);
        vo.setTodayOutbound(8L);
        vo.setAlerts(Arrays.asList("商品A库存低于下限", "商品B库存超上限"));
        DashboardSummaryVO.RecentOperationVO op = new DashboardSummaryVO.RecentOperationVO();
        op.setTime("14:30");
        op.setDescription("入库 - 电子元器件");
        op.setUser("张三");
        vo.setRecentOps(Arrays.asList(op));

        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("productCount"));
        assertNotNull(json.get("totalStock"));
        assertNotNull(json.get("todayInbound"));
        assertNotNull(json.get("todayOutbound"));
        assertNotNull(json.get("alerts"));
        assertNotNull(json.get("recentOps"));
        assertTrue(json.get("productCount").asLong() == 156L);
        assertTrue(json.get("totalStock").asLong() == 8420L);
        assertTrue(json.get("alerts").isArray() && json.get("alerts").size() == 2);
        assertTrue(json.get("recentOps").isArray() && json.get("recentOps").size() == 1);
        assertTrue(json.get("recentOps").get(0).get("description").asText().equals("入库 - 电子元器件"));
    }

    @Test
    @DisplayName("DashboardSummaryVO 字段默认为零值/null,前端不会显示 NaN 或 undefined")
    void defaultValuesAreSafe() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        // 不设任何字段,默认应是 null/0
        JsonNode json = mapper.valueToTree(vo);
        assertTrue(json.get("productCount") == null || json.get("productCount").isNull()
                || json.get("productCount").asLong() == 0);
        assertTrue(json.get("totalStock") == null || json.get("totalStock").isNull()
                || json.get("totalStock").asLong() == 0);
    }
}