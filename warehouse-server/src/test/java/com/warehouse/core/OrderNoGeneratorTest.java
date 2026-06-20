/**
 * P3-1 核心业务功能测试:OrderNoGenerator
 *
 * 覆盖范围:
 *   - generate(prefix, null) 走 Redis fallback → 4 位数字
 *   - 生成结果 = prefix + yyyyMMdd + 4位数字(共 prefix 长度 + 12 字符)
 *   - 不同调用结果可能不同(随机/fallback 路径)
 *   - 前缀支持 RK/CK/JJ/PD 等单据号
 *
 * P3-2 修复:Redis 主路径已改为 6 位模数,fallback 改为"纳秒 4 位 + 随机 3 位",
 * 单据号总长 = prefix(2) + date(8) + seq(6 或 7) = 16 / 17 字符。
 * 此处不再断言 14 字符,改为断言 seq 段至少 6 位且不重复。
 */
package com.warehouse.core;

import com.warehouse.util.OrderNoGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OrderNoGeneratorTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** P3-1: 生成的单据号必须以 prefix 开头 */
    @ParameterizedTest
    @ValueSource(strings = {"RK", "CK", "JJ", "PD"})
    @DisplayName("生成的单据号必须以 prefix 开头(覆盖入库/出库/借还/盘点)")
    void generate_mustStartWithPrefix(String prefix) {
        String orderNo = OrderNoGenerator.generate(prefix, null);
        assertNotNull(orderNo);
        assertTrue(orderNo.startsWith(prefix),
                "orderNo 应以 " + prefix + " 开头,实际: " + orderNo);
    }

    /** P3-1: 生成的单据号必须包含今天日期 */
    @Test
    @DisplayName("生成的单据号必须包含今天日期 yyyyMMdd")
    void generate_mustContainTodayDate() {
        String orderNo = OrderNoGenerator.generate("RK", null);
        String today = LocalDate.now().format(FMT);
        assertTrue(orderNo.contains(today),
                "orderNo 应包含 " + today + ",实际: " + orderNo);
    }

    /** P3-2 修复后:单据号总长 = prefix(2) + date(8) + seq(fallback 9 位) = 19 字符 */
    @Test
    @DisplayName("fallback 路径下单据号必须 19 字符(prefix 2 + date 8 + seq 9)")
    void generate_mustBe19CharsLong() {
        String orderNo = OrderNoGenerator.generate("RK", null);
        assertEquals(19, orderNo.length(),
                "orderNo 应为 19 字符(prefix 2 + date 8 + seq 9),实际: " + orderNo + " 长度=" + orderNo.length());
    }

    /** P3-1: 100 次生成结果至少出现 ≥2 个不同单号(随机 fallback 路径下) */
    @Test
    @DisplayName("Redis 不可用时 fallback 到随机,100 次调用应出现多个不同结果")
    void generate_randomFallback_mustProduceVariety() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(OrderNoGenerator.generate("RK", null));
        }
        // 100 次随机,理论几乎不可能全相同;用极宽松的下限 5
        if (seen.size() < 5) {
            fail("随机 fallback 应产生多样化结果,实际只出现 " + seen.size() + " 个不同单号");
        }
    }

    /**
     * P3-2 新增:fallback 路径 1000 次生成必须全部唯一。
     * 复现 Bug:旧实现 fallback 为 4 位随机数,生日悖论下 1000 次约 50% 概率碰撞,
     * 会触发 uk_order_no 唯一约束 → "提交入库单失败"。修复后改为"纳秒 4 位 + 随机 3 位"。
     */
    @Test
    @DisplayName("P3-2 修复:fallback 路径 1000 次连续生成应全部唯一(消除 uk_order_no 撞号)")
    void generate_fallback_shouldNotDuplicateOver1000Calls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String orderNo = OrderNoGenerator.generate("RK", null);
            assertTrue(seen.add(orderNo),
                    "BUG:fallback 路径 1000 次生成中出现重复: " + orderNo);
        }
        assertEquals(1000, seen.size(), "应有 1000 个不同单号,实际: " + seen.size());
    }
}
