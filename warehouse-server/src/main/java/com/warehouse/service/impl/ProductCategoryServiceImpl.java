package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.CategoryCreateDTO;
import com.warehouse.dto.request.CategoryUpdateDTO;
import com.warehouse.dto.response.CategoryVO;
import com.warehouse.entity.Product;
import com.warehouse.entity.ProductCategory;
import com.warehouse.mapper.ProductCategoryMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.service.ProductCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public List<CategoryVO> getTree() {
        List<ProductCategory> allCategories = baseMapper.selectList(null);
        List<CategoryVO> allVOs = allCategories.stream()
                .map(this::convertToVO)
                .sorted(Comparator.comparingInt(vo -> vo.getSortOrder() != null ? vo.getSortOrder() : 0))
                .collect(Collectors.toList());

        Map<Long, List<CategoryVO>> parentIdMap = allVOs.stream()
                .filter(vo -> vo.getParentId() != null && vo.getParentId() > 0)
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        List<CategoryVO> tree = new ArrayList<>();
        for (CategoryVO vo : allVOs) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                tree.add(vo);
                buildChildren(vo, parentIdMap);
            }
        }
        return tree;
    }

    private void buildChildren(CategoryVO parent, Map<Long, List<CategoryVO>> parentIdMap) {
        List<CategoryVO> children = parentIdMap.get(parent.getCategoryId());
        if (children != null) {
            children.sort(Comparator.comparingInt(vo -> vo.getSortOrder() != null ? vo.getSortOrder() : 0));
            parent.setChildren(children);
            for (CategoryVO child : children) {
                buildChildren(child, parentIdMap);
            }
        }
    }

    @Override
    @Transactional
    public CategoryVO create(CategoryCreateDTO dto) {
        QueryWrapper<ProductCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_name", dto.getCategoryName());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "类别名称已存在");
        }
        if (dto.getParentId() != null && dto.getParentId() != 0) {
            ProductCategory parent = baseMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父类别不存在");
            }
        }
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(dto, category);
        category.setStatus(1);
        baseMapper.insert(category);
        return convertToVO(category);
    }

    @Override
    @Transactional
    public CategoryVO update(Long categoryId, CategoryUpdateDTO dto) {
        ProductCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(404, "类别不存在");
        }
        if (dto.getParentId() != null) {
            // Prevent circular reference: can't set parent to self or descendant
            if (dto.getParentId().equals(categoryId)) {
                throw new BusinessException(400, "不能将自身设置为父类别");
            }
            if (isDescendant(categoryId, dto.getParentId())) {
                throw new BusinessException(400, "不能将子类别设置为父类别，会造成循环引用");
            }
        }
        BeanUtils.copyProperties(dto, category, "categoryId");
        baseMapper.updateById(category);
        return convertToVO(baseMapper.selectById(categoryId));
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        ProductCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(404, "类别不存在");
        }
        // Check no children
        QueryWrapper<ProductCategory> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", categoryId);
        if (baseMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(400, "该类别下存在子类别，无法删除");
        }
        // Check no products reference this category
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("category_id", categoryId);
        if (productMapper.selectCount(productWrapper) > 0) {
            throw new BusinessException(400, "该类别下存在产品，无法删除");
        }
        baseMapper.deleteById(categoryId);
    }

    private boolean isDescendant(Long ancestorId, Long targetId) {
        QueryWrapper<ProductCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", ancestorId);
        List<ProductCategory> children = baseMapper.selectList(wrapper);
        for (ProductCategory child : children) {
            if (child.getCategoryId().equals(targetId)) {
                return true;
            }
            if (isDescendant(child.getCategoryId(), targetId)) {
                return true;
            }
        }
        return false;
    }

    private CategoryVO convertToVO(ProductCategory category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
