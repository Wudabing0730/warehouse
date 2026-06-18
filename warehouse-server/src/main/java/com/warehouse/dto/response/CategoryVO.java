package com.warehouse.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryVO {
    private Long categoryId;

    private String categoryName;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;

    private List<CategoryVO> children;

    private LocalDateTime createTime;
}
