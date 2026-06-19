/**
 * P3-2 仪表盘 7 天趋势数据字段契约
 *
 * 修复:为 DashboardSummaryVO 扩展 7 天出入库趋势数据,供前端 ECharts 折线图渲染
 *   - inboundTrend: number[] 最近 7 天每日入库单数(从 6 天前到今天,共 7 个数)
 *   - outboundTrend: number[] 最近 7 天每日出库单数(同上)
 *   - trendDates: string[] 最近 7 天的日期字符串 yyyy-MM-dd
 *
 * 测试方法:序列化 DashboardSummaryVO → 校验 JSON 包含这 3 个字段
 */
package com.warehouse.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardTrendFieldsTest {

    @Test
    @DisplayName("DashboardSummaryVO 必须包含 inboundTrend / outboundTrend / trendDates 字段")
    void summaryVo_mustHaveTrendFields() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setProductCount(10L);
        vo.setTotalStock(1000L);
        vo.setTodayInbound(3L);
        vo.setTodayOutbound(2L);
        // 构造 7 天数据
        List<Long> inboundTrend = new ArrayList<>();
        List<Long> outboundTrend = new ArrayList<>();
        List<String> trendDates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trendDates.add(date);
            inboundTrend.add((long) i);
            outboundTrend.add((long) (6 - i));
        }
        vo.setInboundTrend(inboundTrend);
        vo.setOutboundTrend(outboundTrend);
        vo.setTrendDates(trendDates);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.valueToTree(vo);

        assertTrue(json.has("inboundTrend"), "必须含 inboundTrend 字段");
        assertTrue(json.has("outboundTrend"), "必须含 outboundTrend 字段");
        assertTrue(json.has("trendDates"), "必须含 trendDates 字段");
    }

    @Test
    @DisplayName("inboundTrend / outboundTrend 必须都是长度为 7 的数组")
    void trends_mustBe7DaysLong() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        List<Long> inbound = new ArrayList<>();
        List<Long> outbound = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            inbound.add(0L);
            outbound.add(0L);
            dates.add(LocalDate.now().minusDays(6 - i).toString());
        }
        vo.setInboundTrend(inbound);
        vo.setOutboundTrend(outbound);
        vo.setTrendDates(dates);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.valueToTree(vo);

        assertEquals(7, json.get("inboundTrend").size(), "inboundTrend 应为 7 个数");
        assertEquals(7, json.get("outboundTrend").size(), "outboundTrend 应为 7 个数");
        assertEquals(7, json.get("trendDates").size(), "trendDates 应为 7 个字符串");
    }

    @Test
    @DisplayName("trendDates 数组中所有日期必须形如 yyyy-MM-dd")
    void trendDates_mustBeIsoFormat() throws Exception {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        vo.setTrendDates(dates);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.valueToTree(vo);

        for (int i = 0; i < 7; i++) {
            String d = json.get("trendDates").get(i).asText();
            assertNotNull(d);
            assertTrue(d.matches("\\d{4}-\\d{2}-\\d{2}"),
                    "日期格式应为 yyyy-MM-dd,实际: " + d);
        }
    }
}
