package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.RoleCreateDTO;
import com.warehouse.dto.request.RoleQueryDTO;
import com.warehouse.dto.request.RoleUpdateDTO;
import com.warehouse.dto.response.RoleVO;
import com.warehouse.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role CRUD and permission assignment APIs")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "List roles", description = "Paginated query of roles with optional filters")
    public Result<IPage<RoleVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       RoleQueryDTO query) {
        IPage<RoleVO> result = roleService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a single role by its unique identifier")
    public Result<RoleVO> getById(@PathVariable Long id) {
        RoleVO role = roleService.getById(id);
        return Result.success(role);
    }

    @PostMapping
    @RequirePermission("role:create")
    @Operation(summary = "Create role", description = "Create a new role with optional permission assignments")
    public Result<RoleVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        RoleVO role = roleService.create(dto);
        return Result.success(role);
    }

    @PutMapping("/{id}")
    @RequirePermission("role:update")
    @Operation(summary = "Update role", description = "Update an existing role's information")
    public Result<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        RoleVO role = roleService.update(id, dto);
        return Result.success(role);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    @Operation(summary = "Delete role", description = "Delete a role by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission("role:assignPermissions")
    @Operation(summary = "Assign permissions", description = "Assign a list of permission IDs to a role")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }
}
