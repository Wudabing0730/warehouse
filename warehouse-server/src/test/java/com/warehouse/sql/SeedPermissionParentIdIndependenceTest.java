/**
 * P2-4 种子数据 parent_id 解耦修复验证
 *
 * Bug 复现:
 *   - 02-seed.sql 中所有 t_permission INSERT 的 parent_id 字段都是字面数字
 *     (1, 3, 4, 9, 15 ...),这些数字是 AUTO_INCREMENT 在固定插入顺序下产生的
 *   - 一旦在中间插入新行(比如 P2-3 加的 system:user:resetPassword),
 *     后面所有 parent_id 都会指向错误的行
 *   - 现象:树状菜单错乱,子菜单挂到错误的父菜单
 *
 * 修复:
 *   - 重构 02-seed.sql:t_permission 的 INSERT 不再写 parent_id 字面数字
 *   - 改用 "INSERT ... SELECT parent_id FROM t_permission WHERE permission_code = '...'"
 *   - 一级菜单的 parent_id 仍为 NULL
 *   - 后续任何行插入都不影响现有 parent_id 引用
 *
 * 验证策略:
 *   1. 解析 02-seed.sql,提取所有 t_permission INSERT 语句
 *   2. 断言所有 parent_id 都不再是字面数字(只允许 NULL)
 *   3. 断言文件中至少出现 N 次 SELECT ... FROM t_permission WHERE permission_code 模式
 *   4. 断言所有原 permission_code 都仍然存在于种子中
 */
package com.warehouse.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedPermissionParentIdIndependenceTest {

    private static final String[] SEED_FILE_CANDIDATES = {
            "../sql/02-seed.sql",
            "../../sql/02-seed.sql",
            "../../../sql/02-seed.sql",
            "sql/02-seed.sql",
    };

    private String readSeed() throws Exception {
        // 多路径尝试,兼容 mvn 不同的 working directory
        for (String path : SEED_FILE_CANDIDATES) {
            try {
                return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            } catch (java.nio.file.NoSuchFileException ignored) {
            }
        }
        throw new java.nio.file.NoSuchFileException(
                "无法在 " + java.util.Arrays.toString(SEED_FILE_CANDIDATES) + " 找到 02-seed.sql");
    }

    /** P2-4: 顶级菜单的 parent_id 可以为 NULL,其他都不能用字面数字 */
    @Test
    void permissionInsert_parentIdMustNotBeHardcodedNumber() throws Exception {
        String sql = readSeed();
        // 只解析 INSERT INTO t_permission 块:从该行开始,到下一个 INSERT INTO 或 ; 结束
        Pattern block = Pattern.compile(
                "INSERT INTO t_permission\\s*\\([^)]*\\)\\s*VALUES\\s*(.+?);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher bm = block.matcher(sql);
        int totalPermRows = 0;
        int withLiteralNumber = 0;
        StringBuilder literalFound = new StringBuilder();
        while (bm.find()) {
            String valuesBlock = bm.group(1);
            // 单行 (..., ..., ..., parent_id)
            Pattern p = Pattern.compile(
                    "\\('([^']+)',\\s*'[^']+',\\s*'[^']+',\\s*([^)]+)\\)");
            Matcher m = p.matcher(valuesBlock);
            while (m.find()) {
                String code = m.group(1);
                String parentRaw = m.group(2).trim();
                totalPermRows++;
                if ("NULL".equalsIgnoreCase(parentRaw)) continue;
                if (parentRaw.contains("SELECT") && parentRaw.contains("t_permission")) continue;
                if (parentRaw.matches("\\d+")) {
                    withLiteralNumber++;
                    literalFound.append("\n  - ").append(code).append(" → parent_id=").append(parentRaw);
                }
            }
        }
        assertTrue(totalPermRows >= 60,
                "P2-4: 种子文件中应至少 60 条 permission 记录,实际找到 " + totalPermRows);
        assertEquals(0, withLiteralNumber,
                "P2-4: t_permission INSERT 的 parent_id 不应再用字面数字(AUTO_INCREMENT 顺序依赖):"
                        + literalFound);
    }

    /** P2-4: 必须用 SELECT FROM t_permission WHERE permission_code 模式解析 parent_id */
    @Test
    void permissionInsert_mustUseSelectByCodeForParent() throws Exception {
        String sql = readSeed();
        Pattern p = Pattern.compile(
                "SELECT\\s+permission_id\\s+FROM\\s+t_permission\\s+WHERE\\s+permission_code\\s*=",
                Pattern.CASE_INSENSITIVE);
        int count = 0;
        Matcher m = p.matcher(sql);
        while (m.find()) count++;
        assertTrue(count >= 50,
                "P2-4: 至少应有 50 处用 SELECT FROM t_permission WHERE permission_code 模式解析 parent_id,"
                        + " 实际只有 " + count + " 处");
    }

    /** P2-4: 所有原 permission_code 仍然存在,没有因为重构丢失 */
    @Test
    void allOriginalPermissionCodesStillPresent() throws Exception {
        String sql = readSeed();
        // 必须保留的所有 permission_code(P0-2 修复后 + P1-5 + P2-3 补充)
        Set<String> required = new HashSet<>();
        required.add("dashboard");
        required.add("dashboard:view");
        required.add("system");
        required.add("system:user");
        required.add("system:user:list");
        required.add("system:user:create");
        required.add("system:user:edit");
        required.add("system:user:delete");
        required.add("system:user:resetPassword"); // P2-3
        required.add("system:role");
        required.add("system:role:assign");
        required.add("base:product");
        required.add("inbound");
        required.add("inbound:audit");
        required.add("outbound");
        required.add("outbound:audit");
        required.add("borrow");
        required.add("borrow:return");
        required.add("inventory");
        required.add("inventory:confirm");
        required.add("report");
        required.add("report:comprehensive");

        Set<String> missing = new HashSet<>();
        for (String code : required) {
            // 必须在 INSERT 语句的 permission_code 字段出现
            if (!sql.contains("'" + code + "'")) {
                missing.add(code);
            }
        }
        assertTrue(missing.isEmpty(),
                "P2-4: 重构后丢失了原 permission_code: " + missing);
    }
}
