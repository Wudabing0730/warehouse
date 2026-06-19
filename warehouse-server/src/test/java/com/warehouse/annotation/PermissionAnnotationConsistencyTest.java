package com.warehouse.annotation;

import com.warehouse.controller.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-2 修复验证测试:验证所有 @RequirePermission 注解的 value
 * 与 sql/02-seed.sql 中实际插入的 permission_code 完全一致。
 *
 * Bug 复现:在修复前,所有 controller 的 @RequirePermission 值(如 role:create、role:assignPermissions)
 * 都不在 t_permission.permission_code 中,即使 admin 调用任何写操作都会被 PermissionAspect
 * 抛 403 "权限不足" — 见 PermissionAspect.checkPermission 的精确比对逻辑。
 *
 * 修复:统一改为与种子数据一致的 code(system:role:assign、base:product:create 等)。
 *
 * 测试方法:反射扫描所有 Controller,提取所有 @RequirePermission.value,
 *         与种子中实际存在的 permission_code 集合比对,
 *         确保每一个注解值都能在种子数据中找到。
 */
class PermissionAnnotationConsistencyTest {

    /**
     * 必须与 sql/02-seed.sql INSERT INTO t_permission (...) VALUES (...) 一字不差。
     * 来源:02-seed.sql 第 36-105 行所有 permission_code。
     */
    private static final Set<String> SEED_PERMISSION_CODES = new HashSet<>(Arrays.asList(
            // dashboard & 系统管理
            "dashboard", "dashboard:view",
            "system", "system:user", "system:user:list", "system:user:create",
            "system:user:edit", "system:user:delete", "system:user:resetPassword",
            "system:role", "system:role:list", "system:role:create",
            "system:role:edit", "system:role:delete", "system:role:assign",
            "system:permission", "system:permission:list",
            "system:log", "system:log:list",
            // 基础资料
            "base", "base:product", "base:product:list", "base:product:create",
            "base:product:edit", "base:product:delete",
            "base:category", "base:category:list", "base:category:create",
            "base:category:edit", "base:category:delete",
            "base:location", "base:location:list", "base:location:create",
            "base:location:edit", "base:location:delete",
            "base:supplier", "base:supplier:list", "base:supplier:create",
            "base:supplier:edit", "base:supplier:delete",
            "base:customer", "base:customer:list", "base:customer:create",
            "base:customer:edit", "base:customer:delete",
            "base:stock", "base:stock:list", "base:stock:init", "base:stock:alert",
            // 入库
            "inbound", "inbound:create", "inbound:audit", "inbound:query",
            // 出库
            "outbound", "outbound:create", "outbound:audit", "outbound:query",
            // 借还
            "borrow", "borrow:create", "borrow:return", "borrow:query",
            // 盘点
            "inventory", "inventory:create", "inventory:confirm", "inventory:query",
            // 报表
            "report", "report:inbound", "report:outbound", "report:stock", "report:comprehensive"
    ));

    private static final Class<?>[] ALL_CONTROLLERS = new Class<?>[]{
            UserController.class,
            RoleController.class,
            ProductController.class,
            ProductCategoryController.class,
            WarehouseLocationController.class,
            SupplierController.class,
            CustomerController.class,
            InboundController.class,
            OutboundController.class,
            BorrowController.class,
            InventoryCheckController.class
    };

    @Test
    @DisplayName("所有 @RequirePermission 注解的 value 必须在种子权限表中存在(P0-2 修复验证)")
    void everyRequirePermissionAnnotationMustMatchSeedPermissionCode() {
        StringBuilder failures = new StringBuilder();

        for (Class<?> controller : ALL_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                RequirePermission annotation = method.getAnnotation(RequirePermission.class);
                if (annotation == null) {
                    continue;
                }
                String code = annotation.value();
                String location = controller.getSimpleName() + "#" + method.getName();
                if (!SEED_PERMISSION_CODES.contains(code)) {
                    failures.append("\n  - ").append(location)
                            .append(" → @RequirePermission(\"").append(code).append("\")")
                            .append(" 不在 02-seed.sql 的 permission_code 中");
                }
            }
        }

        assertTrue(failures.length() == 0,
                "P0-2 Bug: following @RequirePermission values are NOT in seed permission_code "
                        + "(admin user would also get 403):" + failures);
    }

    @Test
    @DisplayName("种子权限码列表本身非空,确保测试基线未被破坏")
    void seedPermissionCodeListIsNotEmpty() {
        assertTrue(SEED_PERMISSION_CODES.size() >= 60,
                "种子权限码应 ≥ 60 条,实际 " + SEED_PERMISSION_CODES.size()
                        + " 条,可能是 02-seed.sql 被改动导致测试基线失效");
    }
}