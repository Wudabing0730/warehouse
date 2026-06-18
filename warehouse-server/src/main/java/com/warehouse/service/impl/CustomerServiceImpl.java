package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.CustomerCreateDTO;
import com.warehouse.dto.request.CustomerQueryDTO;
import com.warehouse.dto.request.CustomerUpdateDTO;
import com.warehouse.dto.response.CustomerVO;
import com.warehouse.entity.Customer;
import com.warehouse.mapper.CustomerMapper;
import com.warehouse.service.CustomerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    @Override
    public IPage<CustomerVO> page(Page<Customer> page, CustomerQueryDTO query) {
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (query.getCustomerCode() != null && !query.getCustomerCode().isEmpty()) {
                wrapper.like("customer_code", query.getCustomerCode());
            }
            if (query.getCustomerName() != null && !query.getCustomerName().isEmpty()) {
                wrapper.like("customer_name", query.getCustomerName());
            }
            if (query.getStatus() != null) {
                wrapper.eq("status", query.getStatus());
            }
        }
        wrapper.orderByDesc("create_time");
        IPage<Customer> customerPage = baseMapper.selectPage(page, wrapper);
        List<CustomerVO> voList = customerPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        IPage<CustomerVO> voPage = new Page<>(customerPage.getCurrent(), customerPage.getSize(), customerPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public CustomerVO getById(Long customerId) {
        Customer customer = baseMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return convertToVO(customer);
    }

    @Override
    @Transactional
    public CustomerVO create(CustomerCreateDTO dto) {
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_code", dto.getCustomerCode());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "客户编码已存在");
        }
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        customer.setStatus(1);
        baseMapper.insert(customer);
        return convertToVO(customer);
    }

    @Override
    @Transactional
    public CustomerVO update(Long customerId, CustomerUpdateDTO dto) {
        Customer customer = baseMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        BeanUtils.copyProperties(dto, customer, "customerId", "customerCode");
        baseMapper.updateById(customer);
        return convertToVO(baseMapper.selectById(customerId));
    }

    @Override
    @Transactional
    public void delete(Long customerId) {
        Customer customer = baseMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        customer.setStatus(0);
        baseMapper.updateById(customer);
    }

    @Override
    public List<Customer> listEnabled() {
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    private CustomerVO convertToVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }
}
