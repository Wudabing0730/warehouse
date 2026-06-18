package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.UserCreateDTO;
import com.warehouse.dto.request.UserQueryDTO;
import com.warehouse.dto.request.UserUpdateDTO;
import com.warehouse.dto.response.UserVO;
import com.warehouse.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD and password management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Paginated query of users with optional filters")
    public Result<IPage<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       UserQueryDTO query) {
        IPage<UserVO> result = userService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a single user by its unique identifier")
    public Result<UserVO> getById(@PathVariable Long id) {
        UserVO user = userService.getById(id);
        return Result.success(user);
    }

    @PostMapping
    @RequirePermission("system:user:create")
    @Operation(summary = "Create user", description = "Create a new user account with roles")
    public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        UserVO user = userService.create(dto);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @RequirePermission("system:user:edit")
    @Operation(summary = "Update user", description = "Update an existing user's information and roles")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        UserVO user = userService.update(id, dto);
        return Result.success(user);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system:user:delete")
    @Operation(summary = "Delete user", description = "Delete a user by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Update password", description = "Change a user's password by providing old and new password")
    public Result<Void> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> passwords) {
        String oldPassword = passwords.get("oldPassword");
        String newPassword = passwords.get("newPassword");
        userService.updatePassword(id, oldPassword, newPassword);
        return Result.success();
    }
}
