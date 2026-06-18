package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.ProductCreateDTO;
import com.warehouse.dto.request.ProductQueryDTO;
import com.warehouse.dto.request.ProductUpdateDTO;
import com.warehouse.dto.response.ProductVO;
import com.warehouse.entity.Product;
import com.warehouse.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product CRUD and query APIs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products", description = "Paginated query of products with optional filters")
    public Result<IPage<ProductVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          ProductQueryDTO query) {
        IPage<ProductVO> result = productService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a single product by its unique identifier")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO product = productService.getById(id);
        return Result.success(product);
    }

    @PostMapping
    @RequirePermission("base:product:create")
    @Operation(summary = "Create product", description = "Create a new product")
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        ProductVO product = productService.create(dto);
        return Result.success(product);
    }

    @PutMapping("/{id}")
    @RequirePermission("base:product:edit")
    @Operation(summary = "Update product", description = "Update an existing product's information")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        ProductVO product = productService.update(id, dto);
        return Result.success(product);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("base:product:delete")
    @Operation(summary = "Delete product", description = "Delete a product by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    @GetMapping("/enabled")
    @Operation(summary = "List enabled products", description = "Retrieve all enabled products without pagination")
    public Result<List<Product>> listEnabled() {
        List<Product> products = productService.listEnabled();
        return Result.success(products);
    }
}
