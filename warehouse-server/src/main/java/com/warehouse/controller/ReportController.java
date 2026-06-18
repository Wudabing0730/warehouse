package com.warehouse.controller;

import com.warehouse.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report data APIs for analytics and dashboards")
public class ReportController {

    @GetMapping("/inbound")
    @Operation(summary = "Inbound report", description = "Retrieve inbound summary data for the given date range")
    public Result<List<Map<String, Object>>> inboundReport(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate) {
        return Result.success(new ArrayList<>());
    }

    @GetMapping("/outbound")
    @Operation(summary = "Outbound report", description = "Retrieve outbound summary data for the given date range")
    public Result<List<Map<String, Object>>> outboundReport(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) String endDate) {
        return Result.success(new ArrayList<>());
    }

    @GetMapping("/stock")
    @Operation(summary = "Stock report", description = "Retrieve stock summary data for dashboards")
    public Result<List<Map<String, Object>>> stockReport() {
        return Result.success(new ArrayList<>());
    }
}
