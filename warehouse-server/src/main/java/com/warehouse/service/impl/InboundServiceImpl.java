package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.common.ResultCode;
import com.warehouse.dto.request.InboundConfirmDTO;
import com.warehouse.dto.request.InboundCreateDTO;
import com.warehouse.dto.request.InboundDetailDTO;
import com.warehouse.dto.request.InboundQueryDTO;
import com.warehouse.dto.response.InboundOrderDetailVO;
import com.warehouse.dto.response.InboundOrderVO;
import com.warehouse.entity.InboundOrder;
import com.warehouse.entity.InboundOrderDetail;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.entity.Supplier;
import com.warehouse.entity.User;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.InboundOrderDetailMapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.InboundService;
import com.warehouse.util.OrderNoGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InboundServiceImpl extends ServiceImpl<InboundOrderMapper, InboundOrder> implements InboundService {

    private static final Logger log = LoggerFactory.getLogger(InboundServiceImpl.class);

    @Autowired
    private InboundOrderDetailMapper inboundOrderDetailMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WarehouseLocationMapper warehouseLocationMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<InboundOrderVO> page(Page<InboundOrder> page, InboundQueryDTO query) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) {
                wrapper.like(InboundOrder::getOrderNo, query.getOrderNo());
            }
            if (query.getSupplierId() != null) {
                wrapper.eq(InboundOrder::getSupplierId, query.getSupplierId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(InboundOrder::getStatus, query.getStatus());
            }
            if (query.getStartTime() != null) {
                wrapper.ge(InboundOrder::getOrderTime, query.getStartTime());
            }
            if (query.getEndTime() != null) {
                wrapper.le(InboundOrder::getOrderTime, query.getEndTime());
            }
        }
        wrapper.orderByDesc(InboundOrder::getCreateTime);

        IPage<InboundOrder> orderPage = baseMapper.selectPage(page, wrapper);
        return orderPage.convert(this::convertToVO);
    }

    @Override
    public InboundOrderVO getById(Long orderId) {
        InboundOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        InboundOrderVO vo = convertToVO(order);

        // Load details
        List<InboundOrderDetail> details = inboundOrderDetailMapper.selectByOrderId(orderId);
        if (!CollectionUtils.isEmpty(details)) {
            vo.setDetails(details.stream().map(this::convertDetailToVO).collect(Collectors.toList()));
        } else {
            vo.setDetails(Collections.emptyList());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundOrderVO create(InboundCreateDTO dto) {
        // 1. Validate details not empty
        if (CollectionUtils.isEmpty(dto.getDetails())) {
            throw new BusinessException(400, "入库明细不能为空");
        }

        // 2. Validate each detail
        for (InboundDetailDTO detail : dto.getDetails()) {
            Product product = productMapper.selectById(detail.getProductId());
            if (product == null) {
                throw new BusinessException(400, "商品不存在: " + detail.getProductId());
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(400, "商品已禁用: " + product.getProductName());
            }
            if (detail.getQuantity() == null || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "入库数量必须大于0");
            }
            // 修复:未指定库位时给 warning 日志,confirm 阶段会用 product.defaultLocationId 兜底
            if (detail.getLocationId() == null) {
                log.warn("入库明细 productId={} 未指定库位,confirm 阶段将回退到商品默认库位",
                        detail.getProductId());
            }
        }

        // 3. Generate order number with retry(避免 Redis 撞 uk_order_no)
        String orderNo = generateUniqueOrderNo("RK");

        // 4. Get current operator
        Long operatorId = SecurityUtils.getCurrentUserId();

        // 5. Save order header
        InboundOrder order = new InboundOrder();
        order.setOrderNo(orderNo);
        order.setSupplierId(dto.getSupplierId());
        order.setOperatorId(operatorId);
        order.setOrderTime(dto.getOrderTime());
        order.setStatus(0);
        order.setRemark(dto.getRemark());
        baseMapper.insert(order);

        // 6. Batch save details
        for (InboundDetailDTO detailDTO : dto.getDetails()) {
            InboundOrderDetail detail = new InboundOrderDetail();
            detail.setOrderId(order.getOrderId());
            detail.setProductId(detailDTO.getProductId());
            detail.setQuantity(detailDTO.getQuantity());
            detail.setUnitPrice(detailDTO.getUnitPrice());
            detail.setLocationId(detailDTO.getLocationId());
            inboundOrderDetailMapper.insert(detail);
        }

        // 7. Return VO with details loaded(修复:之前只 convertToVO(order) 导致 details 为空)
        InboundOrderVO vo = convertToVO(order);
        List<InboundOrderDetail> savedDetails = inboundOrderDetailMapper.selectByOrderId(order.getOrderId());
        if (!CollectionUtils.isEmpty(savedDetails)) {
            vo.setDetails(savedDetails.stream().map(this::convertDetailToVO).collect(Collectors.toList()));
        } else {
            vo.setDetails(Collections.emptyList());
        }
        return vo;
    }

    /**
     * 修复:生成不重复的单号。Redis 主路径即使 % 1000000 在极高并发下仍可能撞号,
     * 因此加一次 DB 唯一性预检 + 重试(最多 3 次)。
     */
    private String generateUniqueOrderNo(String prefix) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String candidate = OrderNoGenerator.generate(prefix, stringRedisTemplate);
            Long existing = baseMapper.selectCount(
                    new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getOrderNo, candidate));
            if (existing == null || existing == 0L) {
                return candidate;
            }
            log.warn("订单号 {} 已存在,重试 attempt={}", candidate, attempt);
        }
        throw new BusinessException(500, "订单号生成失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId, InboundConfirmDTO dto) {
        // 1. Find order, validate exists and status == 0
        InboundOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "只有待处理的入库单才能确认");
        }

        Long confirmOperatorId = SecurityUtils.getCurrentUserId();

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // a. Update order: status=1, confirmOperatorId, confirmTime=now
            order.setStatus(1);
            order.setConfirmOperatorId(confirmOperatorId);
            order.setConfirmTime(LocalDateTime.now());
            baseMapper.updateById(order);

            // b. For each detail: update stock
            List<InboundOrderDetail> details = inboundOrderDetailMapper.selectByOrderId(orderId);
            for (InboundOrderDetail detail : details) {
                Long locationId = detail.getLocationId();
                if (locationId == null) {
                    // Use product's default location
                    Product product = productMapper.selectById(detail.getProductId());
                    locationId = product != null ? product.getDefaultLocationId() : null;
                }
                if (locationId == null) {
                    throw new BusinessException(400, "库位不能为空");
                }

                Stock stock = stockMapper.selectForUpdate(detail.getProductId(), locationId);
                if (stock == null) {
                    // Create new stock record
                    stock = new Stock();
                    stock.setProductId(detail.getProductId());
                    stock.setLocationId(locationId);
                    stock.setQuantity(detail.getQuantity());
                    stockMapper.insert(stock);
                } else {
                    // Add quantity to existing stock
                    stock.setQuantity(stock.getQuantity().add(detail.getQuantity()));
                    stockMapper.updateById(stock);
                }

                // Check upper limit alert
                Product product = productMapper.selectById(detail.getProductId());
                if (product != null && product.getUpperLimit() != null
                        && stock.getQuantity().compareTo(product.getUpperLimit()) > 0) {
                    pushStockAlert(stock.getProductId(), stock.getLocationId(),
                            "库存超上限预警: 商品=" + product.getProductName()
                                    + ", 当前库存=" + stock.getQuantity()
                                    + ", 上限=" + product.getUpperLimit());
                }

                // Delete redis cache
                deleteStockCache(detail.getProductId(), locationId);
            }
        } else {
            // 3. Rejected: status=2
            order.setStatus(2);
            order.setConfirmOperatorId(confirmOperatorId);
            order.setConfirmTime(LocalDateTime.now());
            order.setRemark(dto.getRemark());
            baseMapper.updateById(order);
        }
    }

    private InboundOrderVO convertToVO(InboundOrder order) {
        InboundOrderVO vo = new InboundOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSupplierId(order.getSupplierId());
        vo.setOperatorId(order.getOperatorId());
        vo.setConfirmOperatorId(order.getConfirmOperatorId());
        vo.setOrderTime(order.getOrderTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // Enrich supplier name
        if (order.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
        }
        // Enrich operator name
        if (order.getOperatorId() != null) {
            User operator = userMapper.selectById(order.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
        }
        // Enrich confirm operator name
        if (order.getConfirmOperatorId() != null) {
            User confirmOperator = userMapper.selectById(order.getConfirmOperatorId());
            if (confirmOperator != null) {
                vo.setConfirmOperatorName(confirmOperator.getRealName());
            }
        }
        return vo;
    }

    private InboundOrderDetailVO convertDetailToVO(InboundOrderDetail detail) {
        InboundOrderDetailVO vo = new InboundOrderDetailVO();
        vo.setDetailId(detail.getDetailId());
        vo.setProductId(detail.getProductId());
        vo.setQuantity(detail.getQuantity());
        vo.setUnitPrice(detail.getUnitPrice());
        vo.setLocationId(detail.getLocationId());

        if (detail.getProductId() != null) {
            Product product = productMapper.selectById(detail.getProductId());
            if (product != null) {
                vo.setProductCode(product.getProductCode());
                vo.setProductName(product.getProductName());
                vo.setProductUnit(product.getUnit());
            }
        }
        if (detail.getLocationId() != null) {
            WarehouseLocation location = warehouseLocationMapper.selectById(detail.getLocationId());
            if (location != null) {
                vo.setLocationCode(location.getLocationCode());
                vo.setLocationName(location.getLocationName());
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
