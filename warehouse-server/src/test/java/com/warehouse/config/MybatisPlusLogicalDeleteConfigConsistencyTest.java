/**
 * Bug 复现验证:MyBatis-Plus 全局 logical-delete 配置与实体不一致导致 UPDATE/SELECT 失败
 *
 * 用户报告:
 *   "在前端做的任何数据更改操作都无法正常持久化,比如说在产品管理里禁用P001这个产品时,
 *    显示操作成功,但是数据不更新,并且刷新后依旧无法看到修改后的结果"
 *
 * 根因(高度怀疑):
 *   - src/main/resources/application.yml 第 32-34 行配置了:
 *       mybatis-plus.global-config.db-config.logic-delete-field: deleted
 *       mybatis-plus.global-config.db-config.logic-delete-value: 1
 *       mybatis-plus.global-config.db-config.logic-not-delete-value: 0
 *   - 但 src/main/java/com/warehouse/entity/ 下所有实体(Product/User/Customer/...)
 *     都没有 @TableLogic 注解的 deleted 字段
 *   - src/main/resources/mapper/ 下也没有 deleted 列
 *
 *   MyBatis-Plus 3.5.9 的行为:
 *     - 当 global-config.db-config.logic-delete-field 设置时,所有 UPDATE/SELECT
 *       会自动追加 `WHERE deleted = '0'`
 *     - 由于 deleted 列在多数业务表中不存在,SQL 报
 *       `BadSqlGrammarException: Unknown column 'deleted' in 'where clause'`
 *     - 该异常被 GlobalExceptionHandler 兜底,前端看到 500 + 错误提示
 *
 * 修复:删除 application.yml 中的 logical-delete 全局配置,或显式标注所有 entity 的
 *      @TableLogic 字段(本项目从未启用逻辑删除,选择删除全局配置)
 *
 * 验证策略:
 *   1. 扫描 com.warehouse.entity 下所有 entity,断言没有任何一个声明 deleted 字段或
 *      @TableLogic 注解
 *   2. 读取 application.yml,断言不应该有 logic-delete-field / logic-delete-value
 *      / logic-not-delete-value 全局配置
 *
 * 注:src/test/resources/application-test.yml 已经没有该配置,所以测试 profile
 *     能正常通过。本测试仅检查生产 profile (src/main/resources/application.yml)。
 */
package com.warehouse.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一致性检查:如果项目没有用逻辑删除(没有任何 @TableLogic 字段),
 * application.yml 也不应该配置 logic-delete-field
 */
class MybatisPlusLogicalDeleteConfigConsistencyTest {

    private static final String ENTITY_PACKAGE = "com.warehouse.entity";

    @Test
    @DisplayName("BUG 复现:application.yml 不应配置 logic-delete-field(没有 entity 用 @TableLogic)")
    void applicationYmlMustNotConfigureLogicalDeleteField() throws Exception {
        // 1. 扫描所有 entity,检查是否有任何 deleted 字段或 @TableLogic 注解
        Set<String> entitiesWithDeleted = scanEntitiesWithDeleted();

        // 2. 读取 application.yml
        String yml = readClasspathFile("application.yml");

        // 3. 断言:如果没有任何 entity 用 deleted,application.yml 不应该配置 logic-delete-field
        if (entitiesWithDeleted.isEmpty()) {
            assertFalse(yml.contains("logic-delete-field"),
                    "BUG 复现:application.yml 配置了 mybatis-plus.global-config.db-config.logic-delete-field,"
                            + " 但项目没有任何 entity 声明 deleted 字段或 @TableLogic 注解。"
                            + " 这种配置会导致所有 UPDATE/SELECT 自动追加 `WHERE deleted = '0'`,"
                            + " 而 deleted 列在 t_product 等业务表中不存在,"
                            + " SQL 报 `Unknown column 'deleted' in 'where clause'`,"
                            + " 写入操作全部失败。\n"
                            + " 修复:删除 application.yml 中的 logic-delete-field/logic-delete-value/logic-not-delete-value 三行。");
            assertFalse(yml.contains("logic-delete-value"),
                    "BUG:application.yml 不应配置 logic-delete-value(没有任何 entity 用逻辑删除)");
            assertFalse(yml.contains("logic-not-delete-value"),
                    "BUG:application.yml 不应配置 logic-not-delete-value(没有任何 entity 用逻辑删除)");
        }
    }

    @Test
    @DisplayName("扫描所有 entity,确认没有任何 @TableLogic 字段")
    void noEntityShouldDeclareTableLogic() throws Exception {
        Set<String> entities = scanEntitiesWithDeleted();
        assertTrue(entities.isEmpty(),
                "扫描发现以下 entity 声明了 deleted 字段或 @TableLogic 注解(本项目不应使用逻辑删除):\n"
                        + "  - " + String.join("\n  - ", entities));
    }

    /**
     * 扫描 com.warehouse.entity 包下所有 .class 文件,
     * 检查是否有字段名为 "deleted" 或带 com.baomidou.mybatisplus.annotation.@TableLogic 注解
     */
    private Set<String> scanEntitiesWithDeleted() throws Exception {
        Set<String> result = new HashSet<>();
        java.io.File entityDir = new java.io.File(
                getClass().getClassLoader().getResource("com/warehouse/entity").toURI());
        if (!entityDir.exists()) return result;

        java.io.File[] files = entityDir.listFiles((dir, name) -> name.endsWith(".class"));
        if (files == null) return result;

        for (java.io.File f : files) {
            String simpleName = f.getName().replace(".class", "");
            String className = ENTITY_PACKAGE + "." + simpleName;
            try {
                Class<?> clazz = Class.forName(className);
                for (Field field : clazz.getDeclaredFields()) {
                    // 检查字段名
                    if ("deleted".equalsIgnoreCase(field.getName())) {
                        result.add(className + "." + field.getName() + " (字段名 deleted)");
                    }
                    // 检查 @TableLogic 注解
                    if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableLogic.class)) {
                        result.add(className + "." + field.getName() + " (@TableLogic)");
                    }
                }
            } catch (Throwable t) {
                // 忽略无法加载的类(可能依赖了运行时 bean)
            }
        }
        return result;
    }

    private String readClasspathFile(String path) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤掉 YAML 注释行(# 开头)
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) continue;
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}