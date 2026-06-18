package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.CustomerCreateDTO;
import com.warehouse.dto.request.CustomerQueryDTO;
import com.warehouse.dto.request.CustomerUpdateDTO;
import com.warehouse.dto.response.CustomerVO;
import com.warehouse.entity.Customer;

import java.util.List;

public interface CustomerService extends IService<Customer> {

    IPage<CustomerVO> page(Page<Customer> page, CustomerQueryDTO query);

    CustomerVO getById(Long customerId);

    CustomerVO create(CustomerCreateDTO dto);

    CustomerVO update(Long customerId, CustomerUpdateDTO dto);

    void delete(Long customerId);

    List<Customer> listEnabled();
}
