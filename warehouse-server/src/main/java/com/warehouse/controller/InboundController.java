package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.InboundConfirmDTO;
import com.warehouse.dto.request.InboundCreateDTO;
import com.warehouse.dto.request.InboundQueryDTO;
import com.warehouse.dto.response.InboundOrderVO;
import com.warehouse.service.InboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inbound-orders")
@RequiredArgsConstructor
@Tag(name = "Inbound Order", description = "Inbound order creation, confirmation, and query APIs")
public class InboundController {

    private final InboundService inboundService;

    @GetMapping
    @Operation(summary = "List inbound orders", description = "Paginated query of inbound orders with optional filters")
    public Result<IPage<InboundOrderVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               InboundQueryDTO query) {
        IPage<InboundOrderVO> result = inboundService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inbound order by ID", description = "Retrieve a single inbound order with details by its unique identifier")
    public Result<InboundOrderVO> getById(@PathVariable Long id) {
        InboundOrderVO order = inboundService.getById(id);
        return Result.success(order);
    }

    @PostMapping
    @RequirePermission("inbound:create")
    @Operation(summary = "Create inbound order", description = "Create a new inbound order with detail items")
    public Result<InboundOrderVO> create(@Valid @RequestBody InboundCreateDTO dto) {
        InboundOrderVO order = inboundService.create(dto);
        return Result.success(order);
    }

    @PutMapping("/{id}/confirm")
    @RequirePermission("inbound:confirm")
    @Operation(summary = "Confirm inbound order", description = "Confirm or reject a pending inbound order")
    public Result<Void> confirm(@PathVariable Long id, @Valid @RequestBody InboundConfirmDTO dto) {
        inboundService.confirm(id, dto);
        return Result.success();
    }
}
