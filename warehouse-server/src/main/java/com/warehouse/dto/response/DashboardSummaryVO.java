package com.warehouse.dto.response;

import lombok.Data;

import java.util.List;

/**
 * P0-8 修复:DashboardView 之前所有统计都是写死假数据。
 * 此 VO 作为仪表盘汇总数据的契约,前端通过 GET /api/v1/reports/dashboard-summary
 * 拿到真实数据(从 t_product / t_stock / t_inbound_order / t_outbound_order /
 * t_operation_log 实时聚合)。
 */
@Data
public class DashboardSummaryVO {

    /** 当前启用商品数(t_product.status=1) */
    private Long productCount;

    /** 当前所有库位库存总量(SUM t_stock.quantity) */
    private Long totalStock;

    /** 今日入库单数(t_inbound_order.create_time >= 今日 00:00) */
    private Long todayInbound;

    /** 今日出库单数(t_outbound_order.create_time >= 今日 00:00) */
    private Long todayOutbound;

    /** 库存预警消息列表(Redis 队列 stock:alert:queue 取前 10 条) */
    private List<String> alerts;

    /** 最近 5 条操作记录(从 t_operation_log 按 create_time DESC 取) */
    private List<RecentOperationVO> recentOps;

    /** P3-2: 最近 7 天每日入库单数(从 6 天前到今天,共 7 个数),供 ECharts 折线图 */
    private List<Long> inboundTrend;

    /** P3-2: 最近 7 天每日出库单数,供 ECharts 折线图 */
    private List<Long> outboundTrend;

    /** P3-2: 最近 7 天的日期字符串 yyyy-MM-dd,与上方两个数组一一对应 */
    private List<String> trendDates;

    @Data
    public static class RecentOperationVO {
        private String time;
        private String description;
        private String user;
    }
}