package com.warehouse.controller;

import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.CategoryCreateDTO;
import com.warehouse.dto.request.CategoryUpdateDTO;
import com.warehouse.dto.response.CategoryVO;
import com.warehouse.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Product Category", description = "Product category tree and CRUD APIs")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    @Operation(summary = "Get category tree", description = "Retrieve the full hierarchical product category tree")
    public Result<List<CategoryVO>> getTree() {
        List<CategoryVO> tree = productCategoryService.getTree();
        return Result.success(tree);
    }

    @PostMapping
    @RequirePermission("category:create")
    @Operation(summary = "Create category", description = "Create a new product category")
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateDTO dto) {
        CategoryVO category = productCategoryService.create(dto);
        return Result.success(category);
    }

    @PutMapping("/{id}")
    @RequirePermission("category:update")
    @Operation(summary = "Update category", description = "Update an existing product category's information")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateDTO dto) {
        CategoryVO category = productCategoryService.update(id, dto);
        return Result.success(category);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("category:delete")
    @Operation(summary = "Delete category", description = "Delete a product category by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return Result.success();
    }
}
