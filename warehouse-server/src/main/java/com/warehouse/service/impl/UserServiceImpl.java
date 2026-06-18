package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.UserCreateDTO;
import com.warehouse.dto.request.UserQueryDTO;
import com.warehouse.dto.request.UserUpdateDTO;
import com.warehouse.dto.response.UserVO;
import com.warehouse.entity.Permission;
import com.warehouse.entity.Role;
import com.warehouse.entity.User;
import com.warehouse.mapper.PermissionMapper;
import com.warehouse.mapper.RoleMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.mapper.UserRoleMapper;
import com.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<UserVO> page(Page<User> page, UserQueryDTO query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.hasText(query.getUsername()), User::getUsername, query.getUsername())
                   .like(StringUtils.hasText(query.getRealName()), User::getRealName, query.getRealName())
                   .like(StringUtils.hasText(query.getPhone()), User::getPhone, query.getPhone())
                   .like(StringUtils.hasText(query.getEmail()), User::getEmail, query.getEmail())
                   .eq(query.getStatus() != null, User::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        return userPage.convert(user -> {
            UserVO vo = new UserVO();
            vo.setUserId(user.getUserId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            vo.setUpdateTime(user.getUpdateTime());

            List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getUserId());
            vo.setRoleIds(roleIds);

            if (!roleIds.isEmpty()) {
                List<String> roleNames = roleMapper.selectBatchIds(roleIds).stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toList());
                vo.setRoleNames(roleNames);
            }

            return vo;
        });
    }

    @Override
    public UserVO getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userMapper.insert(user);

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                userRoleMapper.insertUserRole(user.getUserId(), roleId);
            }
        }

        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(userId);
            for (Long roleId : dto.getRoleIds()) {
                userRoleMapper.insertUserRole(userId, roleId);
            }
        }

        clearPermissionCache(userId);

        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        userRoleMapper.deleteByUserId(userId);
        userMapper.deleteById(userId);
        clearPermissionCache(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        // 密码被管理员重置后,失效其 refresh token 缓存,强制重新登录
        try {
            stringRedisTemplate.delete("user:permissions:" + userId);
        } catch (Exception ignored) { }
    }

    @Override
    public Set<String> getPermissionCodes(Long userId) {
        String cacheKey = "user:permissions:" + userId;

        Set<String> cached = stringRedisTemplate.opsForSet().members(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> permissionCodes = roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .flatMap(role -> {
                    List<Permission> permissions = permissionMapper.selectByUserId(userId);
                    return permissions.stream();
                })
                .filter(permission -> permission.getStatus() != null && permission.getStatus() == 1)
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());

        stringRedisTemplate.opsForSet().add(cacheKey, permissionCodes.toArray(new String[0]));
        stringRedisTemplate.expire(cacheKey, Duration.ofMinutes(30));

        return permissionCodes;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());

        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getUserId());
        vo.setRoleIds(roleIds);

        if (!roleIds.isEmpty()) {
            List<String> roleNames = roleMapper.selectBatchIds(roleIds).stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());
            vo.setRoleNames(roleNames);
        }

        return vo;
    }

    private void clearPermissionCache(Long userId) {
        stringRedisTemplate.delete("user:permissions:" + userId);
    }
}
