package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.LocationCreateDTO;
import com.warehouse.dto.request.LocationUpdateDTO;
import com.warehouse.dto.response.LocationVO;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.service.WarehouseLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Warehouse Location", description = "Warehouse location CRUD and query APIs")
public class WarehouseLocationController {

    private final WarehouseLocationService warehouseLocationService;

    @GetMapping
    @Operation(summary = "List locations", description = "Paginated query of warehouse locations")
    public Result<IPage<LocationVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        IPage<LocationVO> result = warehouseLocationService.page(new Page<>(page, size));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieve a single warehouse location by its unique identifier")
    public Result<LocationVO> getById(@PathVariable Long id) {
        LocationVO location = warehouseLocationService.getById(id);
        return Result.success(location);
    }

    @PostMapping
    @RequirePermission("location:create")
    @Operation(summary = "Create location", description = "Create a new warehouse location")
    public Result<LocationVO> create(@Valid @RequestBody LocationCreateDTO dto) {
        LocationVO location = warehouseLocationService.create(dto);
        return Result.success(location);
    }

    @PutMapping("/{id}")
    @RequirePermission("location:update")
    @Operation(summary = "Update location", description = "Update an existing warehouse location's information")
    public Result<LocationVO> update(@PathVariable Long id, @Valid @RequestBody LocationUpdateDTO dto) {
        LocationVO location = warehouseLocationService.update(id, dto);
        return Result.success(location);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("location:delete")
    @Operation(summary = "Delete location", description = "Delete a warehouse location by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseLocationService.delete(id);
        return Result.success();
    }

    @GetMapping("/enabled")
    @Operation(summary = "List enabled locations", description = "Retrieve all enabled warehouse locations without pagination")
    public Result<List<WarehouseLocation>> listEnabled() {
        List<WarehouseLocation> locations = warehouseLocationService.listEnabled();
        return Result.success(locations);
    }
}
