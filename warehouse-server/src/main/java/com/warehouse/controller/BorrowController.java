package com.warehouse.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.Result;
import com.warehouse.dto.request.BorrowCreateDTO;
import com.warehouse.dto.request.BorrowQueryDTO;
import com.warehouse.dto.request.BorrowReturnDTO;
import com.warehouse.dto.response.BorrowRecordVO;
import com.warehouse.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrow-records")
@RequiredArgsConstructor
@Tag(name = "Borrow Record", description = "Borrow record creation, return, and query APIs")
public class BorrowController {

    private final BorrowService borrowService;

    @GetMapping
    @Operation(summary = "List borrow records", description = "Paginated query of borrow records with optional filters")
    public Result<IPage<BorrowRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               BorrowQueryDTO query) {
        IPage<BorrowRecordVO> result = borrowService.page(new Page<>(page, size), query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get borrow record by ID", description = "Retrieve a single borrow record by its unique identifier")
    public Result<BorrowRecordVO> getById(@PathVariable Long id) {
        BorrowRecordVO record = borrowService.getById(id);
        return Result.success(record);
    }

    @PostMapping
    @RequirePermission("borrow:create")
    @Operation(summary = "Create borrow record", description = "Create a new borrow record (also deducts stock)")
    public Result<BorrowRecordVO> create(@Valid @RequestBody BorrowCreateDTO dto) {
        BorrowRecordVO record = borrowService.create(dto);
        return Result.success(record);
    }

    @PutMapping("/{id}/return")
    @RequirePermission("borrow:return")
    @Operation(summary = "Return borrowed items", description = "Return borrowed items and restore stock partially or fully")
    public Result<BorrowRecordVO> returnItem(@PathVariable Long id, @Valid @RequestBody BorrowReturnDTO dto) {
        BorrowRecordVO record = borrowService.returnItem(id, dto);
        return Result.success(record);
    }
}
