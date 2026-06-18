package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.common.ResultCode;
import com.warehouse.dto.request.InventoryCheckConfirmDTO;
import com.warehouse.dto.request.InventoryCheckCreateDTO;
import com.warehouse.dto.request.InventoryCheckQueryDTO;
import com.warehouse.dto.response.InventoryCheckVO;
import com.warehouse.entity.InventoryCheck;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.entity.User;
import com.warehouse.mapper.InventoryCheckMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.InventoryCheckService;
import com.warehouse.util.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InventoryCheckServiceImpl extends ServiceImpl<InventoryCheckMapper, InventoryCheck> implements InventoryCheckService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<InventoryCheckVO> page(Page<InventoryCheck> page, InventoryCheckQueryDTO query) {
        LambdaQueryWrapper<InventoryCheck> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getCheckNo() != null && !query.getCheckNo().isEmpty()) {
                wrapper.like(InventoryCheck::getCheckNo, query.getCheckNo());
            }
            if (query.getProductId() != null) {
                wrapper.eq(InventoryCheck::getProductId, query.getProductId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(InventoryCheck::getStatus, query.getStatus());
            }
            if (query.getStartDate() != null) {
                wrapper.ge(InventoryCheck::getCheckDate, query.getStartDate());
            }
            if (query.getEndDate() != null) {
                wrapper.le(InventoryCheck::getCheckDate, query.getEndDate());
            }
        }
        wrapper.orderByDesc(InventoryCheck::getCreateTime);

        IPage<InventoryCheck> checkPage = baseMapper.selectPage(page, wrapper);
        return checkPage.convert(this::convertToVO);
    }

    @Override
    public InventoryCheckVO getById(Long checkId) {
        InventoryCheck check = baseMapper.selectById(checkId);
        if (check == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(check);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCheckVO create(InventoryCheckCreateDTO dto) {
        // 1. Validate product exists
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(400, "商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(400, "商品已禁用");
        }
        if (dto.getActualQuantity() == null || dto.getActualQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "盘点数量不能为负数");
        }

        // 2. Query current stock as bookQuantity
        BigDecimal bookQuantity = BigDecimal.ZERO;
        Long locationId = product.getDefaultLocationId();
        if (locationId != null) {
            Stock stock = stockMapper.selectForUpdate(dto.getProductId(), locationId);
            if (stock != null) {
                bookQuantity = stock.getQuantity();
            }
        } else {
            // If no default location, try to find any stock for this product
            LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(Stock::getProductId, dto.getProductId());
            Stock stock = stockMapper.selectOne(stockWrapper);
            if (stock != null) {
                bookQuantity = stock.getQuantity();
            }
        }

        // 3. Generate order number
        String checkNo = OrderNoGenerator.generate("PD", stringRedisTemplate);

        // 4. Calculate diffQuantity
        BigDecimal diffQuantity = dto.getActualQuantity().subtract(bookQuantity);

        // 5. Save inventory check record
        Long operatorId = SecurityUtils.getCurrentUserId();
        InventoryCheck check = new InventoryCheck();
        check.setCheckNo(checkNo);
        check.setProductId(dto.getProductId());
        check.setBookQuantity(bookQuantity);
        check.setActualQuantity(dto.getActualQuantity());
        check.setDiffQuantity(diffQuantity);
        check.setOperatorId(operatorId);
        check.setCheckDate(dto.getCheckDate());
        check.setStatus(0);
        check.setRemark(dto.getRemark());
        baseMapper.insert(check);

        // 6. Return VO
        return convertToVO(check);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long checkId, InventoryCheckConfirmDTO dto) {
        // 1. Find check record, validate status == 0
        InventoryCheck check = baseMapper.selectById(checkId);
        if (check == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (check.getStatus() != 0) {
            throw new BusinessException(400, "只有待处理的盘点记录才能确认");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // a. Update stock: set quantity to actualQuantity
            Product product = productMapper.selectById(check.getProductId());
            Long locationId = product != null ? product.getDefaultLocationId() : null;

            if (locationId != null) {
                Stock stock = stockMapper.selectForUpdate(check.getProductId(), locationId);
                if (stock == null) {
                    // Create new stock record if none exists
                    stock = new Stock();
                    stock.setProductId(check.getProductId());
                    stock.setLocationId(locationId);
                    stock.setQuantity(check.getActualQuantity());
                    stockMapper.insert(stock);
                } else {
                    stock.setQuantity(check.getActualQuantity());
                    stockMapper.updateById(stock);
                }
            } else {
                // Try to find stock by productId only
                LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
                stockWrapper.eq(Stock::getProductId, check.getProductId());
                Stock stock = stockMapper.selectOne(stockWrapper);
                if (stock != null) {
                    // Lock and update
                    stock = stockMapper.selectForUpdate(check.getProductId(), stock.getLocationId());
                    stock.setQuantity(check.getActualQuantity());
                    stockMapper.updateById(stock);
                } else {
                    throw new BusinessException(400, "该商品无库存记录且未设置默认库位，无法调整");
                }
            }

            // Check upper/lower limit alerts
            if (product != null && locationId != null) {
                Stock updatedStock = stockMapper.selectById(
                        new LambdaQueryWrapper<Stock>()
                                .eq(Stock::getProductId, check.getProductId())
                                .eq(Stock::getLocationId, locationId));
                if (updatedStock != null) {
                    if (product.getUpperLimit() != null
                            && updatedStock.getQuantity().compareTo(product.getUpperLimit()) > 0) {
                        pushStockAlert(updatedStock.getProductId(), updatedStock.getLocationId(),
                                "库存超上限预警(盘点调整): 商品=" + product.getProductName()
                                        + ", 当前库存=" + updatedStock.getQuantity()
                                        + ", 上限=" + product.getUpperLimit());
                    }
                    if (product.getLowerLimit() != null
                            && updatedStock.getQuantity().compareTo(product.getLowerLimit()) < 0) {
                        pushStockAlert(updatedStock.getProductId(), updatedStock.getLocationId(),
                                "库存低于下限预警(盘点调整): 商品=" + product.getProductName()
                                        + ", 当前库存=" + updatedStock.getQuantity()
                                        + ", 下限=" + product.getLowerLimit());
                    }
                    // Delete redis stock cache
                    deleteStockCache(check.getProductId(), locationId);
                }
            }

            // b. Update check record: status=1
            check.setStatus(1);
            check.setRemark(dto.getRemark());
            baseMapper.updateById(check);
        } else {
            // 3. Rejected: status=2
            check.setStatus(2);
            check.setRemark(dto.getRemark());
            baseMapper.updateById(check);
        }
    }

    private InventoryCheckVO convertToVO(InventoryCheck check) {
        InventoryCheckVO vo = new InventoryCheckVO();
        vo.setCheckId(check.getCheckId());
        vo.setCheckNo(check.getCheckNo());
        vo.setProductId(check.getProductId());
        vo.setBookQuantity(check.getBookQuantity());
        vo.setActualQuantity(check.getActualQuantity());
        vo.setDiffQuantity(check.getDiffQuantity());
        vo.setOperatorId(check.getOperatorId());
        vo.setCheckDate(check.getCheckDate());
        vo.setStatus(check.getStatus());
        vo.setRemark(check.getRemark());
        vo.setCreateTime(check.getCreateTime());

        // Enrich product info
        if (check.getProductId() != null) {
            Product product = productMapper.selectById(check.getProductId());
            if (product != null) {
                vo.setProductCode(product.getProductCode());
                vo.setProductName(product.getProductName());
                vo.setProductUnit(product.getUnit());
            }
        }
        // Enrich operator name
        if (check.getOperatorId() != null) {
            User operator = userMapper.selectById(check.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
        }
        return vo;
    }

    private void pushStockAlert(Long productId, Long locationId, String message) {
        try {
            stringRedisTemplate.opsForList().leftPush("stock:alert:queue", message);
        } catch (Exception ignored) {
            // Redis unavailable, skip alert
        }
    }

    private void deleteStockCache(Long productId, Long locationId) {
        try {
            stringRedisTemplate.delete("stock:" + productId + ":" + locationId);
        } catch (Exception ignored) {
            // Redis unavailable, skip cache delete
        }
    }
}
