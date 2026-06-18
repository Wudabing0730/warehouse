package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.Result;
import com.warehouse.dto.request.StockQueryDTO;
import com.warehouse.dto.response.StockVO;
import com.warehouse.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock Management", description = "Stock query, initialization, and alert APIs")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "List stocks", description = "Paginated query of stock records with optional filters")
    public Result<IPage<StockVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        StockQueryDTO query) {
        IPage<StockVO> result = stockService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @PostMapping("/init")
    @Operation(summary = "Initialize stock", description = "Initialize stock quantity for a product at a specific location")
    public Result<Void> init(@RequestBody Map<String, Object> body) {
        Long productId = body.get("productId") != null
                ? Long.valueOf(body.get("productId").toString()) : null;
        Long locationId = body.get("locationId") != null
                ? Long.valueOf(body.get("locationId").toString()) : null;
        BigDecimal quantity = body.get("quantity") != null
                ? new BigDecimal(body.get("quantity").toString()) : null;
        stockService.init(productId, locationId, quantity);
        return Result.success();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get stock alerts", description = "Retrieve all stock records that exceed or fall below configured thresholds")
    public Result<List<StockVO>> getAlerts() {
        List<StockVO> alerts = stockService.getAlerts();
        return Result.success(alerts);
    }
}
