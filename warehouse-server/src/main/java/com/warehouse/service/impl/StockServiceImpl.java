package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.StockQueryDTO;
import com.warehouse.dto.response.StockVO;
import com.warehouse.entity.Product;
import com.warehouse.entity.Stock;
import com.warehouse.entity.WarehouseLocation;
import com.warehouse.mapper.ProductMapper;
import com.warehouse.mapper.StockMapper;
import com.warehouse.mapper.WarehouseLocationMapper;
import com.warehouse.service.StockService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private static final String STOCK_ALERT_QUEUE_KEY = "stock:alert:queue";

    @Resource
    private ProductMapper productMapper;

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<StockVO> page(Page<Stock> page, StockQueryDTO query) {
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        if (query.getProductId() != null) {
            wrapper.eq("product_id", query.getProductId());
        }
        if (query.getLocationId() != null) {
            wrapper.eq("location_id", query.getLocationId());
        }
        wrapper.orderByDesc("create_time");

        IPage<Stock> stockPage = baseMapper.selectPage(page, wrapper);
        List<StockVO> voList = stockPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        IPage<StockVO> voPage = new Page<>(stockPage.getCurrent(), stockPage.getSize(), stockPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void init(Long productId, Long locationId, BigDecimal quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "产品不存在");
        }
        WarehouseLocation location = warehouseLocationMapper.selectById(locationId);
        if (location == null) {
            throw new BusinessException(404, "库位不存在");
        }

        // Check if stock record already exists (product+location unique)
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        wrapper.eq("location_id", locationId);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "该产品库位已有库存记录");
        }

        Stock stock = new Stock();
        stock.setProductId(productId);
        stock.setLocationId(locationId);
        stock.setQuantity(quantity);
        baseMapper.insert(stock);
    }

    @Override
    public List<StockVO> getAlerts() {
        List<String> messages = stringRedisTemplate.opsForList().range(STOCK_ALERT_QUEUE_KEY, 0, -1);
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        // Trim the queue to remove all consumed messages
        stringRedisTemplate.opsForList().trim(STOCK_ALERT_QUEUE_KEY, messages.size(), -1);

        return messages.stream().map(msg -> {
            StockVO vo = new StockVO();
            vo.setAlertMessage(msg);
            return vo;
        }).collect(Collectors.toList());
    }

    private StockVO convertToVO(Stock stock) {
        StockVO vo = new StockVO();
        BeanUtils.copyProperties(stock, vo);
        if (stock.getProductId() != null) {
            Product product = productMapper.selectById(stock.getProductId());
            if (product != null) {
                vo.setProductCode(product.getProductCode());
                vo.setProductName(product.getProductName());
            }
        }
        if (stock.getLocationId() != null) {
            WarehouseLocation location = warehouseLocationMapper.selectById(stock.getLocationId());
            if (location != null) {
                vo.setLocationCode(location.getLocationCode());
                vo.setLocationName(location.getLocationName());
            }
        }
        return vo;
    }
}
