package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.ProductCreateDTO;
import com.warehouse.dto.request.ProductQueryDTO;
import com.warehouse.dto.request.ProductUpdateDTO;
import com.warehouse.dto.response.ProductVO;
import com.warehouse.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    IPage<ProductVO> page(Page<Product> page, ProductQueryDTO query);

    ProductVO getById(Long productId);

    ProductVO create(ProductCreateDTO dto);

    ProductVO update(Long productId, ProductUpdateDTO dto);

    void delete(Long productId);

    List<Product> listEnabled();
}
