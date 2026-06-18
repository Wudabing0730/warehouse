package com.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.CategoryCreateDTO;
import com.warehouse.dto.request.CategoryUpdateDTO;
import com.warehouse.dto.response.CategoryVO;
import com.warehouse.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {

    List<CategoryVO> getTree();

    CategoryVO create(CategoryCreateDTO dto);

    CategoryVO update(Long categoryId, CategoryUpdateDTO dto);

    void delete(Long categoryId);
}
