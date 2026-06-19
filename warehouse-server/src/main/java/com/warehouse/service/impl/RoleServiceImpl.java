package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.RoleCreateDTO;
import com.warehouse.dto.request.RoleQueryDTO;
import com.warehouse.dto.request.RoleUpdateDTO;
import com.warehouse.dto.response.RoleVO;
import com.warehouse.entity.Permission;
import com.warehouse.entity.Role;
import com.warehouse.entity.RolePermission;
import com.warehouse.mapper.PermissionMapper;
import com.warehouse.mapper.RoleMapper;
import com.warehouse.mapper.RolePermissionMapper;
import com.warehouse.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public IPage<RoleVO> page(Page<Role> page, RoleQueryDTO query) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.hasText(query.getRoleName()), Role::getRoleName, query.getRoleName())
                   .eq(query.getStatus() != null, Role::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Role::getCreateTime);

        IPage<Role> rolePage = baseMapper.selectPage(page, wrapper);

        // 批量预取所有相关 role 的 permissionIds,避免 N+1
        List<Role> roles = rolePage.getRecords();
        if (!roles.isEmpty()) {
            List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
            List<RolePermission> allMappings = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
            );
            Map<Long, List<Long>> rolePermMap = allMappings.stream()
                    .collect(Collectors.groupingBy(
                            RolePermission::getRoleId,
                            Collectors.mapping(RolePermission::getPermissionId, Collectors.toList())
                    ));
            return rolePage.convert(role -> {
                RoleVO vo = toRoleVO(role);
                vo.setPermissionIds(rolePermMap.getOrDefault(role.getRoleId(), new ArrayList<>()));
                return vo;
            });
        }
        return rolePage.convert(this::toRoleVO);
    }

    @Override
    public RoleVO getById(Long roleId) {
        Role role = baseMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleCreateDTO dto) {
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            Long existCount = permissionMapper.selectCount(
                    new LambdaQueryWrapper<Permission>()
                            .in(Permission::getPermissionId, dto.getPermissionIds())
            );
            if (existCount < dto.getPermissionIds().size()) {
                throw new BusinessException(400, "部分权限ID不存在");
            }
        }

        Role role = new Role();
        role.setRoleName(dto.getRoleName());
        role.setRoleDesc(dto.getRoleDesc());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        baseMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            for (Long permissionId : dto.getPermissionIds()) {
                rolePermissionMapper.insertRolePermission(role.getRoleId(), permissionId);
            }
        }

        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(Long roleId, RoleUpdateDTO dto) {
        Role role = baseMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }

        if (dto.getRoleName() != null) {
            role.setRoleName(dto.getRoleName());
        }
        if (dto.getRoleDesc() != null) {
            role.setRoleDesc(dto.getRoleDesc());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        baseMapper.updateById(role);

        if (dto.getPermissionIds() != null) {
            Long existCount = permissionMapper.selectCount(
                    new LambdaQueryWrapper<Permission>()
                            .in(Permission::getPermissionId, dto.getPermissionIds())
            );
            if (existCount < dto.getPermissionIds().size()) {
                throw new BusinessException(400, "部分权限ID不存在");
            }
            rolePermissionMapper.deleteByRoleId(roleId);
            for (Long permissionId : dto.getPermissionIds()) {
                rolePermissionMapper.insertRolePermission(roleId, permissionId);
            }
        }

        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long roleId) {
        Role role = baseMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        baseMapper.deleteById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (permissionIds != null && !permissionIds.isEmpty()) {
            Long existCount = permissionMapper.selectCount(
                    new LambdaQueryWrapper<Permission>()
                            .in(Permission::getPermissionId, permissionIds)
            );
            if (existCount < permissionIds.size()) {
                throw new BusinessException(400, "部分权限ID不存在");
            }
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                rolePermissionMapper.insertRolePermission(roleId, permissionId);
            }
        }
    }

    private RoleVO toRoleVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleDesc(role.getRoleDesc());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());

        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, role.getRoleId())
        ).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        vo.setPermissionIds(permissionIds);

        return vo;
    }
}
