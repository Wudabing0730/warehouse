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
                return String.format("%04d", seq % 10000);
            } catch (Exception e) {
                // Redis unavailable, fallback to random
            }
        }

        // Fallback: random 4-digit number
        return String.format("%04d", RANDOM.nextInt(10000));
    }
}
