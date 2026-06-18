package com.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.warehouse.dto.request.UserCreateDTO;
import com.warehouse.dto.request.UserQueryDTO;
import com.warehouse.dto.request.UserUpdateDTO;
import com.warehouse.dto.response.UserVO;
import com.warehouse.entity.User;

import java.util.Set;

public interface UserService extends IService<User> {

    IPage<UserVO> page(Page<User> page, UserQueryDTO query);

    UserVO getById(Long userId);

    UserVO create(UserCreateDTO dto);

    UserVO update(Long userId, UserUpdateDTO dto);

    void delete(Long userId);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    Set<String> getPermissionCodes(Long userId);
}
