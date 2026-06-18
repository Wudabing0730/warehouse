package com.warehouse.dto.request;

import lombok.Data;

@Data
public class CategoryUpdateDTO {
    private String categoryName;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;
}
