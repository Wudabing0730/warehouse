package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.ProductCreateDTO;
import com.warehouse.dto.request.ProductQueryDTO;
import com.warehouse.dto.request.ProductUpdateDTO;
import com.warehouse.dto.response.ProductVO;
import com.warehouse.entity.Product;
import com.warehouse.entity.ProductCategory;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.ProductCategoryMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

    @Override
    public IPage<ProductVO> page(Page<Product> page, ProductQueryDTO query) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(query.getProductCode())) {
            wrapper.like("product_code", query.getProductCode());
        }
        if (StringUtils.hasText(query.getProductName())) {
            wrapper.like("product_name", query.getProductName());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq("category_id", query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        wrapper.orderByDesc("create_time");

        IPage<Product> productPage = baseMapper.selectPage(page, wrapper);
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        IPage<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ProductVO getById(Long productId) {
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "产品不存在");
        }
        return convertToVO(product);
    }

    @Override
    @Transactional
    public ProductVO create(ProductCreateDTO dto) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("product_code", dto.getProductCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "产品编码已存在");
        }
        if (dto.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BusinessException(400, "产品类别不存在");
            }
        }
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(1);
        baseMapper.insert(product);
        return convertToVO(product);
    }

    @Override
    @Transactional
    public ProductVO update(Long productId, ProductUpdateDTO dto) {
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "产品不存在");
        }
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(product.getCategoryId())) {
            ProductCategory category = productCategoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                throw new BusinessException(400, "产品类别不存在");
            }
        }
        BeanUtils.copyProperties(dto, product, "productId", "productCode");
        baseMapper.updateById(product);
        return convertToVO(baseMapper.selectById(productId));
    }

    @Override
    @Transactional
    public void delete(Long productId) {
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "产品不存在");
        }
        product.setStatus(0);
        baseMapper.updateById(product);
    }

    @Override
    public List<Product> listEnabled() {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        if (product.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        if (product.getDefaultLocationId() != null) {
            WarehouseLocation location = warehouseLocationMapper.selectById(product.getDefaultLocationId());
            if (location != null) {
                vo.setLocationName(location.getLocationName());
            }
        }
        return vo;
    }
}
