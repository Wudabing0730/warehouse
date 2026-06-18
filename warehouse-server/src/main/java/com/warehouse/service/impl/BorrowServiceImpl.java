package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.common.ResultCode;
import com.warehouse.dto.request.BorrowCreateDTO;
import com.warehouse.dto.request.BorrowQueryDTO;
import com.warehouse.dto.request.BorrowReturnDTO;
import com.warehouse.dto.response.BorrowRecordVO;
import com.warehouse.entity.BorrowRecord;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.entity.User;
import com.warehouse.mapper.BorrowRecordMapper;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.BorrowService;
import com.warehouse.util.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<BorrowRecordVO> page(Page<BorrowRecord> page, BorrowQueryDTO query) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getRecordNo() != null && !query.getRecordNo().isEmpty()) {
                wrapper.like(BorrowRecord::getRecordNo, query.getRecordNo());
            }
            if (query.getProductId() != null) {
                wrapper.eq(BorrowRecord::getProductId, query.getProductId());
            }
            if (query.getBorrower() != null && !query.getBorrower().isEmpty()) {
                wrapper.like(BorrowRecord::getBorrower, query.getBorrower());
            }
            if (query.getStatus() != null) {
                wrapper.eq(BorrowRecord::getStatus, query.getStatus());
            }
            if (query.getStartDate() != null) {
                wrapper.ge(BorrowRecord::getBorrowDate, query.getStartDate());
            }
            if (query.getEndDate() != null) {
                wrapper.le(BorrowRecord::getBorrowDate, query.getEndDate());
            }
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);

        IPage<BorrowRecord> recordPage = baseMapper.selectPage(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    public BorrowRecordVO getById(Long recordId) {
        BorrowRecord record = baseMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecordVO create(BorrowCreateDTO dto) {
        // 1. Validate product exists
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(400, "商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(400, "商品已禁用");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "借出数量必须大于0");
        }

        // 2. Generate order number
        String recordNo = OrderNoGenerator.generate("JJ", stringRedisTemplate);

        // 3. Deduct stock
        Long locationId = product.getDefaultLocationId();
        if (locationId == null) {
            throw new BusinessException(400, "商品未设置默认库位");
        }
        Stock stock = stockMapper.selectForUpdate(dto.getProductId(), locationId);
        if (stock == null) {
            throw new BusinessException(400, "库存不足: 该商品在该库位无库存记录");
        }
        if (stock.getQuantity().compareTo(dto.getQuantity()) < 0) {
            throw new BusinessException(400, "库存不足: 当前库存=" + stock.getQuantity()
                    + ", 借出数量=" + dto.getQuantity());
        }
        stock.setQuantity(stock.getQuantity().subtract(dto.getQuantity()));
        stockMapper.updateById(stock);

        // Check lower limit alert after deduction
        if (product.getLowerLimit() != null
                && stock.getQuantity().compareTo(product.getLowerLimit()) < 0) {
            pushStockAlert(stock.getProductId(), stock.getLocationId(),
                    "库存低于下限预警(借出): 商品=" + product.getProductName()
                            + ", 当前库存=" + stock.getQuantity()
                            + ", 下限=" + product.getLowerLimit());
        }

        // Delete redis stock cache
        deleteStockCache(dto.getProductId(), locationId);

        // 4. Save borrow record
        Long operatorId = SecurityUtils.getCurrentUserId();
        BorrowRecord record = new BorrowRecord();
        record.setRecordNo(recordNo);
        record.setProductId(dto.getProductId());
        record.setQuantity(dto.getQuantity());
        record.setBorrower(dto.getBorrower());
        record.setBorrowDate(dto.getBorrowDate());
        record.setExpectedReturnDate(dto.getExpectedReturnDate());
        record.setReturnQuantity(BigDecimal.ZERO);
        record.setOperatorId(operatorId);
        record.setStatus(0);
        record.setRemark(dto.getRemark());
        baseMapper.insert(record);

        // 5. Return VO
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecordVO returnItem(Long recordId, BorrowReturnDTO dto) {
        // 1. Find record, validate status != 1
        BorrowRecord record = baseMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException(400, "该借出记录已全部归还");
        }

        // 2. Validate returnQuantity
        BigDecimal returnQty = dto.getReturnQuantity();
        if (returnQty == null || returnQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "归还数量必须大于0");
        }
        BigDecimal alreadyReturned = record.getReturnQuantity() != null
                ? record.getReturnQuantity() : BigDecimal.ZERO;
        BigDecimal remaining = record.getQuantity().subtract(alreadyReturned);
        if (returnQty.compareTo(remaining) > 0) {
            throw new BusinessException(400, "归还数量超过借出未还数量: 剩余=" + remaining);
        }

        // 3. Add returnQuantity back to stock
        Product product = productMapper.selectById(record.getProductId());
        Long locationId = product != null ? product.getDefaultLocationId() : null;
        if (locationId == null) {
            throw new BusinessException(400, "商品未设置默认库位");
        }
        Stock stock = stockMapper.selectForUpdate(record.getProductId(), locationId);
        if (stock == null) {
            // Create new stock record if none exists
            stock = new Stock();
            stock.setProductId(record.getProductId());
            stock.setLocationId(locationId);
            stock.setQuantity(returnQty);
            stockMapper.insert(stock);
        } else {
            stock.setQuantity(stock.getQuantity().add(returnQty));
            stockMapper.updateById(stock);
        }

        // 4. Update record
        BigDecimal newReturnQty = alreadyReturned.add(returnQty);
        record.setReturnQuantity(newReturnQty);
        if (dto.getActualReturnDate() != null) {
            record.setActualReturnDate(dto.getActualReturnDate());
        } else {
            record.setActualReturnDate(LocalDate.now());
        }

        // Determine status
        if (newReturnQty.compareTo(record.getQuantity()) >= 0) {
            record.setStatus(1); // Fully returned
        } else {
            record.setStatus(2); // Partially returned
        }
        baseMapper.updateById(record);

        // 5. Delete redis stock cache
        deleteStockCache(record.getProductId(), locationId);

        // 6. Check upper limit alert after stock increase
        if (product != null && product.getUpperLimit() != null
                && stock.getQuantity().compareTo(product.getUpperLimit()) > 0) {
            pushStockAlert(stock.getProductId(), stock.getLocationId(),
                    "库存超上限预警(归还): 商品=" + product.getProductName()
                            + ", 当前库存=" + stock.getQuantity()
                            + ", 上限=" + product.getUpperLimit());
        }

        // 7. Return VO
        return convertToVO(record);
    }

    private BorrowRecordVO convertToVO(BorrowRecord record) {
        BorrowRecordVO vo = new BorrowRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setRecordNo(record.getRecordNo());
        vo.setProductId(record.getProductId());
        vo.setQuantity(record.getQuantity());
        vo.setBorrower(record.getBorrower());
        vo.setBorrowDate(record.getBorrowDate());
        vo.setExpectedReturnDate(record.getExpectedReturnDate());
        vo.setActualReturnDate(record.getActualReturnDate());
        vo.setReturnQuantity(record.getReturnQuantity());
        vo.setOperatorId(record.getOperatorId());
        vo.setStatus(record.getStatus());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());

        // Enrich product info
        if (record.getProductId() != null) {
            Product product = productMapper.selectById(record.getProductId());
            if (product != null) {
                vo.setProductCode(product.getProductCode());
                vo.setProductName(product.getProductName());
                vo.setProductUnit(product.getUnit());
            }
        }
        // Enrich operator name
        if (record.getOperatorId() != null) {
            User operator = userMapper.selectById(record.getOperatorId());
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
