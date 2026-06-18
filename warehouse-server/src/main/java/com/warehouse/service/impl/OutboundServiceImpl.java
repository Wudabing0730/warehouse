package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.common.ResultCode;
import com.warehouse.dto.request.OutboundConfirmDTO;
import com.warehouse.dto.request.OutboundCreateDTO;
import com.warehouse.dto.request.OutboundDetailDTO;
import com.warehouse.dto.request.OutboundQueryDTO;
import com.warehouse.dto.response.OutboundOrderDetailVO;
import com.warehouse.dto.response.OutboundOrderVO;
import com.warehouse.entity.Customer;
import com.warehouse.entity.OutboundOrder;
import com.warehouse.entity.OutboundOrderDetail;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.entity.User;
import com.warehouse.mapper.CustomerMapper;
import com.warehouse.mapper.OutboundOrderDetailMapper;
import com.warehouse.mapper.OutboundOrderMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.OutboundService;
import com.warehouse.util.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutboundServiceImpl extends ServiceImpl<OutboundOrderMapper, OutboundOrder> implements OutboundService {

    @Autowired
    private OutboundOrderDetailMapper outboundOrderDetailMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<OutboundOrderVO> page(Page<OutboundOrder> page, OutboundQueryDTO query) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) {
                wrapper.like(OutboundOrder::getOrderNo, query.getOrderNo());
            }
            if (query.getCustomerId() != null) {
                wrapper.eq(OutboundOrder::getCustomerId, query.getCustomerId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(OutboundOrder::getStatus, query.getStatus());
            }
            if (query.getStartTime() != null) {
                wrapper.ge(OutboundOrder::getOrderTime, query.getStartTime());
            }
            if (query.getEndTime() != null) {
                wrapper.le(OutboundOrder::getOrderTime, query.getEndTime());
            }
        }
        wrapper.orderByDesc(OutboundOrder::getCreateTime);

        IPage<OutboundOrder> orderPage = baseMapper.selectPage(page, wrapper);
        return orderPage.convert(this::convertToVO);
    }

    @Override
    public OutboundOrderVO getById(Long orderId) {
        OutboundOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        OutboundOrderVO vo = convertToVO(order);

        // Load details
        List<OutboundOrderDetail> details = outboundOrderDetailMapper.selectByOrderId(orderId);
        if (!CollectionUtils.isEmpty(details)) {
            vo.setDetails(details.stream().map(this::convertDetailToVO).collect(Collectors.toList()));
        } else {
            vo.setDetails(Collections.emptyList());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderVO create(OutboundCreateDTO dto) {
        // 1. Validate details not empty
        if (CollectionUtils.isEmpty(dto.getDetails())) {
            throw new BusinessException(400, "出库明细不能为空");
        }

        // 2. Validate each detail
        for (OutboundDetailDTO detail : dto.getDetails()) {
            Product product = productMapper.selectById(detail.getProductId());
            if (product == null) {
                throw new BusinessException(400, "商品不存在: " + detail.getProductId());
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(400, "商品已禁用: " + product.getProductName());
            }
            if (detail.getQuantity() == null || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "出库数量必须大于0");
            }
        }

        // 3. Generate order number
        String orderNo = OrderNoGenerator.generate("CK", stringRedisTemplate);

        // 4. Get current operator
        Long operatorId = SecurityUtils.getCurrentUserId();

        // 5. Save order header
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(dto.getCustomerId());
        order.setDepartment(dto.getDepartment());
        order.setApplicant(dto.getApplicant());
        order.setOperatorId(operatorId);
        order.setOrderTime(dto.getOrderTime());
        order.setStatus(0);
        order.setRemark(dto.getRemark());
        baseMapper.insert(order);

        // 6. Batch save details
        for (OutboundDetailDTO detailDTO : dto.getDetails()) {
            OutboundOrderDetail detail = new OutboundOrderDetail();
            detail.setOrderId(order.getOrderId());
            detail.setProductId(detailDTO.getProductId());
            detail.setQuantity(detailDTO.getQuantity());
            detail.setUnitPrice(detailDTO.getUnitPrice());
            detail.setLocationId(detailDTO.getLocationId());
            outboundOrderDetailMapper.insert(detail);
        }

        // 7. Return VO
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId, OutboundConfirmDTO dto) {
        // 1. Find order, validate exists and status == 0
        OutboundOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "只有待处理的出库单才能确认");
        }

        Long confirmOperatorId = SecurityUtils.getCurrentUserId();

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // a. Update order: status=1, confirmOperatorId, confirmTime=now
            order.setStatus(1);
            order.setConfirmOperatorId(confirmOperatorId);
            order.setConfirmTime(LocalDateTime.now());
            baseMapper.updateById(order);

            // b. For each detail: deduct stock
            List<OutboundOrderDetail> details = outboundOrderDetailMapper.selectByOrderId(orderId);
            for (OutboundOrderDetail detail : details) {
                Long locationId = detail.getLocationId();
                if (locationId == null) {
                    Product product = productMapper.selectById(detail.getProductId());
                    locationId = product != null ? product.getDefaultLocationId() : null;
                }
                if (locationId == null) {
                    throw new BusinessException(400, "库位不能为空");
                }

                // selectForUpdate on stock -> MUST exist and quantity >= detail.quantity
                Stock stock = stockMapper.selectForUpdate(detail.getProductId(), locationId);
                if (stock == null) {
                    throw new BusinessException(400, "库存不足: 该商品在该库位无库存记录");
                }
                if (stock.getQuantity().compareTo(detail.getQuantity()) < 0) {
                    throw new BusinessException(400, "库存不足: 当前库存=" + stock.getQuantity()
                            + ", 出库数量=" + detail.getQuantity());
                }

                // Subtract quantity
                stock.setQuantity(stock.getQuantity().subtract(detail.getQuantity()));
                stockMapper.updateById(stock);

                // Check lower limit alert
                Product product = productMapper.selectById(detail.getProductId());
                if (product != null && product.getLowerLimit() != null
                        && stock.getQuantity().compareTo(product.getLowerLimit()) < 0) {
                    pushStockAlert(stock.getProductId(), stock.getLocationId(),
                            "库存低于下限预警: 商品=" + product.getProductName()
                                    + ", 当前库存=" + stock.getQuantity()
                                    + ", 下限=" + product.getLowerLimit());
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

    private OutboundOrderVO convertToVO(OutboundOrder order) {
        OutboundOrderVO vo = new OutboundOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerId(order.getCustomerId());
        vo.setDepartment(order.getDepartment());
        vo.setApplicant(order.getApplicant());
        vo.setOperatorId(order.getOperatorId());
        vo.setConfirmOperatorId(order.getConfirmOperatorId());
        vo.setOrderTime(order.getOrderTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // Enrich customer name
        if (order.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
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

    private OutboundOrderDetailVO convertDetailToVO(OutboundOrderDetail detail) {
        OutboundOrderDetailVO vo = new OutboundOrderDetailVO();
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
