package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.CustomerCreateDTO;
import com.warehouse.dto.request.CustomerUpdateDTO;
import com.warehouse.dto.response.CustomerVO;
import com.warehouse.entity.Customer;
import com.warehouse.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer CRUD and query APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List customers", description = "Paginated query of customers")
    public Result<IPage<CustomerVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        IPage<CustomerVO> result = customerService.page(new Page<>(page, size));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieve a single customer by its unique identifier")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        CustomerVO customer = customerService.getById(id);
        return Result.success(customer);
    }

    @PostMapping
    @RequirePermission("base:customer:create")
    @Operation(summary = "Create customer", description = "Create a new customer")
    public Result<CustomerVO> create(@Valid @RequestBody CustomerCreateDTO dto) {
        CustomerVO customer = customerService.create(dto);
        return Result.success(customer);
    }

    @PutMapping("/{id}")
    @RequirePermission("base:customer:edit")
    @Operation(summary = "Update customer", description = "Update an existing customer's information")
    public Result<CustomerVO> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateDTO dto) {
        CustomerVO customer = customerService.update(id, dto);
        return Result.success(customer);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("base:customer:delete")
    @Operation(summary = "Delete customer", description = "Delete a customer by its unique identifier")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }

    @GetMapping("/enabled")
    @Operation(summary = "List enabled customers", description = "Retrieve all enabled customers without pagination")
    public Result<List<Customer>> listEnabled() {
        List<Customer> customers = customerService.listEnabled();
        return Result.success(customers);
    }
}
