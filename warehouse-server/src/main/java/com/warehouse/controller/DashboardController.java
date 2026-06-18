package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.dto.response.DashboardSummaryVO;
import com.warehouse.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P0-8 修复:DashboardController 替代 DashboardView 之前的硬编码假数据。
 * GET /api/v1/dashboard/summary 返回实时聚合统计。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard summary statistics APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary",
            description = "Aggregate real-time stats: product count, total stock, "
                    + "today inbound/outbound order counts, alerts, and recent 5 operations")
    public Result<DashboardSummaryVO> summary() {
        return Result.success(dashboardService.summary());
    }
}