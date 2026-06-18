package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.SupplierCreateDTO;
import com.warehouse.dto.request.SupplierUpdateDTO;
import com.warehouse.dto.response.SupplierVO;
import com.warehouse.entity.Supplier;
import com.warehouse.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "Supplier CRUD and query APIs")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @Operation(summary = "List suppliers", description = "Paginated query of suppliers")
    public Result<IPage<SupplierVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        IPage<SupplierVO> result = supplierService.page(new Page<>(page, size));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID", description = "Retrieve a single supplier by its unique identifier")
    public Result<SupplierVO> getById(@PathVariable Long id) {
        SupplierVO supplier = supplierService.getById(id);
        return Result.success(supplier);
    }

    @PostMapping
    @RequirePermission("base:supplier:create")
    @Operation(summary = "Create supplier", description = "Create a new supplier")
    public Result<SupplierVO> create(@Valid @RequestBody SupplierCreateDTO dto) {
        SupplierVO supplier = supplierService.create(dto);
        return Result.success(supplier);
    }

    @PutMapping("/{id}")
    @RequirePermission("base:supplier:edit")
    @Operation(summary = "Update supplier", description = "Update an existing supplier's information")
    public Result<SupplierVO> update(@PathVariable Long id, @Valid @RequestBody SupplierUpdateDTO dto) {
        SupplierVO supplier = supplierService.update(id, dto);
        return Result.success(supplier);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("base:supplier:delete")
    @Operation(summary = "Delete supplier", description = "Delete a supplier by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.success();
    }

    @GetMapping("/enabled")
    @Operation(summary = "List enabled suppliers", description = "Retrieve all enabled suppliers without pagination")
    public Result<List<Supplier>> listEnabled() {
        List<Supplier> suppliers = supplierService.listEnabled();
        return Result.success(suppliers);
    }
}
