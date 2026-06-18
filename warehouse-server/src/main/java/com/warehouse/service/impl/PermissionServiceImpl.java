package com.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehouse.dto.response.PermissionVO;
import com.warehouse.entity.Permission;
import com.warehouse.mapper.PermissionMapper;
import com.warehouse.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> getTree() {
        List<Permission> allPermissions = permissionMapper.selectList(null);

        List<PermissionVO> allVos = allPermissions.stream().map(perm -> {
            PermissionVO vo = new PermissionVO();
            vo.setPermissionId(perm.getPermissionId());
            vo.setPermissionCode(perm.getPermissionCode());
            vo.setPermissionName(perm.getPermissionName());
            vo.setResourceType(perm.getResourceType());
            vo.setParentId(perm.getParentId());
            vo.setStatus(perm.getStatus());
            vo.setChildren(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        Map<Long, List<PermissionVO>> childrenMap = allVos.stream()
                .filter(vo -> vo.getParentId() != null && vo.getParentId() != 0)
                .collect(Collectors.groupingBy(PermissionVO::getParentId));

        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO vo : allVos) {
            List<PermissionVO> children = childrenMap.get(vo.getPermissionId());
            if (children != null) {
                vo.setChildren(children);
            }
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            }
        }

        return roots;
    }
}
