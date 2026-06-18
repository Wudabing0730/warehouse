package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.InventoryCheckConfirmDTO;
import com.warehouse.dto.request.InventoryCheckCreateDTO;
import com.warehouse.dto.request.InventoryCheckQueryDTO;
import com.warehouse.dto.response.InventoryCheckVO;
import com.warehouse.service.InventoryCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory-checks")
@RequiredArgsConstructor
@Tag(name = "Inventory Check", description = "Inventory check creation, confirmation, and query APIs")
public class InventoryCheckController {

    private final InventoryCheckService inventoryCheckService;

    @GetMapping
    @Operation(summary = "List inventory checks", description = "Paginated query of inventory check records with optional filters")
    public Result<IPage<InventoryCheckVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 InventoryCheckQueryDTO query) {
        IPage<InventoryCheckVO> result = inventoryCheckService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory check by ID", description = "Retrieve a single inventory check record by its unique identifier")
    public Result<InventoryCheckVO> getById(@PathVariable Long id) {
        InventoryCheckVO check = inventoryCheckService.getById(id);
        return Result.success(check);
    }

    @PostMapping
    @RequirePermission("inventory:create")
    @Operation(summary = "Create inventory check", description = "Create a new inventory check record")
    public Result<InventoryCheckVO> create(@Valid @RequestBody InventoryCheckCreateDTO dto) {
        InventoryCheckVO check = inventoryCheckService.create(dto);
        return Result.success(check);
    }

    @PutMapping("/{id}/confirm")
    @RequirePermission("inventory:confirm")
    @Operation(summary = "Confirm inventory check", description = "Confirm an inventory check and adjust stock accordingly")
    public Result<Void> confirm(@PathVariable Long id, @Valid @RequestBody InventoryCheckConfirmDTO dto) {
        inventoryCheckService.confirm(id, dto);
        return Result.success();
    }
}
