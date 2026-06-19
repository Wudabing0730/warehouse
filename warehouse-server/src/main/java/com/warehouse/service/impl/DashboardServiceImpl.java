package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.dto.response.DashboardSummaryVO;
import com.warehouse.dto.response.DashboardSummaryVO.RecentOperationVO;
import com.warehouse.entity.InboundOrder;
import com.warehouse.entity.OperationLog;
import com.warehouse.entity.OutboundOrder;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.OperationLogMapper;
import com.warehouse.mapper.OutboundOrderMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P0-8 修复:DashboardService 替代原 DashboardView 硬编码假数据,
 * 实时从 MySQL/Redis 聚合统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OperationLogMapper operationLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();

        // 1. 启用商品数
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );
        vo.setProductCount(productCount == null ? 0L : productCount);

        // 2. 总库存(SUM quantity)
        List<Stock> allStocks = stockMapper.selectList(null);
        long total = 0L;
        for (Stock s : allStocks) {
            BigDecimal q = s.getQuantity();
            if (q != null) {
                total += q.longValue();
            }
        }
        vo.setTotalStock(total);

        // 3. 今日入库单数(00:00 之后创建)
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        Long todayInbound = inboundOrderMapper.selectCount(
                new LambdaQueryWrapper<InboundOrder>().ge(InboundOrder::getCreateTime, startOfToday)
        );
        vo.setTodayInbound(todayInbound == null ? 0L : todayInbound);

        // 4. 今日出库单数
        Long todayOutbound = outboundOrderMapper.selectCount(
                new LambdaQueryWrapper<OutboundOrder>().ge(OutboundOrder::getCreateTime, startOfToday)
        );
        vo.setTodayOutbound(todayOutbound == null ? 0L : todayOutbound);

        // 5. 库存预警(从 Redis 队列取前 10 条)
        try {
            List<String> alerts = stringRedisTemplate.opsForList().range("stock:alert:queue", 0, 9);
            vo.setAlerts(alerts == null ? Collections.emptyList() : alerts);
        } catch (Exception e) {
            log.warn("Redis 不可用,预警列表置空: {}", e.getMessage());
            vo.setAlerts(Collections.emptyList());
        }

        // 6. 最近 5 条操作
        List<OperationLog> recentLogs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getCreateTime)
                        .last("LIMIT 5")
        );
        List<RecentOperationVO> recentOps = recentLogs.stream().map(this::toRecentOp).collect(Collectors.toList());
        vo.setRecentOps(recentOps);

        // P3-2: 7 天出入库趋势
        vo.setTrendDates(buildLast7Days());
        vo.setInboundTrend(build7DayCounts(vo.getTrendDates(), true));
        vo.setOutboundTrend(build7DayCounts(vo.getTrendDates(), false));

        return vo;
    }

    /** 构造最近 7 天的日期字符串(从 6 天前到今天) */
    private List<String> buildLast7Days() {
        DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).format(FMT));
        }
        return dates;
    }

    /** 对每个日期统计单据数(true=入库,false=出库) */
    private List<Long> build7DayCounts(List<String> dates, boolean inbound) {
        List<Long> counts = new ArrayList<>(7);
        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            Long c;
            if (inbound) {
                c = inboundOrderMapper.selectCount(
                        new LambdaQueryWrapper<InboundOrder>()
                                .ge(InboundOrder::getCreateTime, start)
                                .lt(InboundOrder::getCreateTime, end));
            } else {
                c = outboundOrderMapper.selectCount(
                        new LambdaQueryWrapper<OutboundOrder>()
                                .ge(OutboundOrder::getCreateTime, start)
                                .lt(OutboundOrder::getCreateTime, end));
            }
            counts.add(c == null ? 0L : c);
        }
        return counts;
    }

    private RecentOperationVO toRecentOp(OperationLog log) {
        RecentOperationVO vo = new RecentOperationVO();
        vo.setTime(log.getCreateTime() != null ? log.getCreateTime().format(HHMM) : "");
        // P0-8:OperationLog entity 用 operation 字段,不是 description
        vo.setDescription(log.getOperation() != null ? log.getOperation() : "");
        vo.setUser(log.getUsername() != null ? log.getUsername() : "");
        return vo;
    }
}