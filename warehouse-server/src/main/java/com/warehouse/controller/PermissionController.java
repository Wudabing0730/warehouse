package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.dto.response.PermissionVO;
import com.warehouse.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "Permission tree query APIs")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get permission tree", description = "Retrieve the full hierarchical permission tree")
    public Result<List<PermissionVO>> getTree() {
        List<PermissionVO> tree = permissionService.getTree();
        return Result.success(tree);
    }
}
