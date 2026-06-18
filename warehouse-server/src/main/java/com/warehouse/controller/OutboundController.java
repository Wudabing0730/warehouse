package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.OutboundConfirmDTO;
import com.warehouse.dto.request.OutboundCreateDTO;
import com.warehouse.dto.request.OutboundQueryDTO;
import com.warehouse.dto.response.OutboundOrderVO;
import com.warehouse.service.OutboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/outbound-orders")
@RequiredArgsConstructor
@Tag(name = "Outbound Order", description = "Outbound order creation, confirmation, and query APIs")
public class OutboundController {

    private final OutboundService outboundService;

    @GetMapping
    @Operation(summary = "List outbound orders", description = "Paginated query of outbound orders with optional filters")
    public Result<IPage<OutboundOrderVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                OutboundQueryDTO query) {
        IPage<OutboundOrderVO> result = outboundService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get outbound order by ID", description = "Retrieve a single outbound order with details by its unique identifier")
    public Result<OutboundOrderVO> getById(@PathVariable Long id) {
        OutboundOrderVO order = outboundService.getById(id);
        return Result.success(order);
    }

    @PostMapping
    @RequirePermission("outbound:create")
    @Operation(summary = "Create outbound order", description = "Create a new outbound order with detail items")
    public Result<OutboundOrderVO> create(@Valid @RequestBody OutboundCreateDTO dto) {
        OutboundOrderVO order = outboundService.create(dto);
        return Result.success(order);
    }

    @PutMapping("/{id}/confirm")
    @RequirePermission("outbound:confirm")
    @Operation(summary = "Confirm outbound order", description = "Confirm or reject a pending outbound order")
    public Result<Void> confirm(@PathVariable Long id, @Valid @RequestBody OutboundConfirmDTO dto) {
        outboundService.confirm(id, dto);
        return Result.success();
    }
}
