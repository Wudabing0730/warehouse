package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Delete("DELETE FROM t_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO t_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
