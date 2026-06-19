package com.warehouse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryVO {

    @JsonProperty("categoryId")
    private Long categoryId;

    private String categoryName;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;

    private List<CategoryVO> children;

    private LocalDateTime createTime;

    @JsonProperty("id")
    public Long getId() {
        return categoryId;
    }
}