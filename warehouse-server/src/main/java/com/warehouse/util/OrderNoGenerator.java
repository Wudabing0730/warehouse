package com.warehouse.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public static String generate(String prefix, StringRedisTemplate stringRedisTemplate) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String sequenceNumber = getSequenceNumber(prefix, date, stringRedisTemplate);
        return prefix + date + sequenceNumber;
    }

    private static String getSequenceNumber(String prefix, String date, StringRedisTemplate stringRedisTemplate) {
        if (stringRedisTemplate != null) {
            try {
                String key = "seq:order:" + prefix + ":" + date;
                Long seq = stringRedisTemplate.opsForValue().increment(key);
                // Set expiry so keys don't accumulate forever (keep for 2 days)
                stringRedisTemplate.expire(key, java.time.Duration.ofDays(2));
                // P3-1 修复:6 位模数,避免单日超过 10000 单时回绕重复触发 uk_order_no
                return String.format("%06d", seq % 1_000_000L);
            } catch (Exception e) {
                // Redis unavailable, fallback to random
            }
        }

        // P3-1 修复:fallback 用 ThreadLocalRandom 提供 9 位熵(10 亿种可能),
        // 1000 次循环碰撞概率 ~ exp(-0.0005) ≈ 0.05%,实际测试 1000 次不撞;
        // 订单号总长 prefix(2) + date(8) + seq(9) = 19 字符,VARCHAR(30) 容量足够
        long entropy = RANDOM.nextLong(0, 1_000_000_000L);
        return String.format("%09d", entropy);
    }
}
