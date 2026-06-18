package com.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.response.PermissionVO;
import com.warehouse.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    List<PermissionVO> getTree();
}
