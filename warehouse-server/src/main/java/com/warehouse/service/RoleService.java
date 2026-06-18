package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.RoleCreateDTO;
import com.warehouse.dto.request.RoleQueryDTO;
import com.warehouse.dto.request.RoleUpdateDTO;
import com.warehouse.dto.response.RoleVO;
import com.warehouse.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    IPage<RoleVO> page(Page<Role> page, RoleQueryDTO query);

    RoleVO getById(Long roleId);

    RoleVO create(RoleCreateDTO dto);

    RoleVO update(Long roleId, RoleUpdateDTO dto);

    void delete(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}
