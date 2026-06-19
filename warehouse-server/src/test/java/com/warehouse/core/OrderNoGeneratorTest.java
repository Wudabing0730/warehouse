/**
 * P3-1 核心业务功能测试:OrderNoGenerator
 *
 * 覆盖范围:
 *   - generate(prefix, null) 走 Redis fallback → 4 位数字
 *   - 生成结果 = prefix + yyyyMMdd + 4位数字(共 prefix 长度 + 12 字符)
 *   - 不同调用结果可能不同(随机/fallback 路径)
 *   - 前缀支持 RK/CK/JJ/PD 等单据号
 */
package com.warehouse.core;

import com.warehouse.util.OrderNoGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    /** P3-1: 生成的单据号格式 = prefix(2) + yyyyMMdd(8) + seq(4) = 14 字符 */
    @Test
    @DisplayName("生成的单据号必须恰好 14 字符(prefix 2 + date 8 + seq 4)")
    void generate_mustBe14CharsLong() {
        String orderNo = OrderNoGenerator.generate("RK", null);
        assertEquals(14, orderNo.length(),
                "orderNo 应为 14 字符,实际: " + orderNo + " 长度=" + orderNo.length());
    }

    /** P3-1: 100 次生成结果至少出现 ≥2 个不同单号(随机 fallback 路径下) */
    @Test
    @DisplayName("Redis 不可用时 fallback 到随机,100 次调用应出现多个不同结果")
    void generate_randomFallback_mustProduceVariety() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(OrderNoGenerator.generate("RK", null));
        }
        // 100 次随机,理论几乎不可能全相同;用极宽松的下限 5
        if (seen.size() < 5) {
            fail("随机 fallback 应产生多样化结果,实际只出现 " + seen.size() + " 个不同单号");
        }
    }
}
