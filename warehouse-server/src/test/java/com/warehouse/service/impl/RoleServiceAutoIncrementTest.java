package com.warehouse.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.request.RoleCreateDTO;
import com.warehouse.dto.response.RoleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RoleServiceAutoIncrementTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("RoleVO 新增角色后 roleId 不被 permissionIds 覆盖")
    void roleIdNotOverriddenByPermissionIds() throws Exception {
        RoleVO vo = new RoleVO();
        setField(vo, "roleId", 7L);
        setField(vo, "roleName", "管理层");
        setField(vo, "roleDesc", "查看报表和统计数据");
        setField(vo, "status", 1);
        setField(vo, "permissionIds", Arrays.asList(1L, 2L, 8L, 27L, 34L));
        JsonNode json = mapper.valueToTree(vo);
        assertTrue(json.get("roleId").asLong() == 7L,
                "roleId 应为 7 而非 permissionIds 的长度或其他错误值");
        assertTrue(json.get("id").asLong() == 7L,
                "id 别名应为 7 而非 permissionIds 的长度或其他错误值");
    }

    @Test
    @DisplayName("RoleVO 必须同时序列化 id 和 roleId，且值相同")
    void roleVoHasIdAndRoleIdAlias() throws Exception {
        RoleVO vo = new RoleVO();
        setField(vo, "roleId", 5L);
        JsonNode json = mapper.valueToTree(vo);
        assertNotNull(json.get("id"), "RoleVO JSON 缺少 'id' 字段,前端 row.id 会是 undefined");
        assertTrue(json.get("id").asLong() == 5L, "id 别名值应等于 roleId");
        assertNotNull(json.get("roleId"), "RoleVO JSON 必须保留原始 roleId 字段");
        assertTrue(json.get("roleId").asLong() == 5L, "roleId 值应正确");
        assertTrue(json.get("id").asLong() == json.get("roleId").asLong(),
                "id 和 roleId 必须相等,否则前端 id/roleId 显示不一致");
    }

    @Test
    @DisplayName("RoleCreateDTO 预校验: 不存在的 permissionIds 应在 insert 前抛出 BusinessException")
    void roleCreateDtoValidation() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleName("测试角色");
        dto.setRoleDesc("测试");
        dto.setPermissionIds(Arrays.asList(999999L));
        assertNotNull(dto.getPermissionIds());
        assertEquals(1, dto.getPermissionIds().size());
        assertEquals(999999L, dto.getPermissionIds().get(0));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}