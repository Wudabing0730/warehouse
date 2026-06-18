package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    @NotBlank
    private String categoryName;

    private Long parentId;

    private Integer sortOrder;
}
