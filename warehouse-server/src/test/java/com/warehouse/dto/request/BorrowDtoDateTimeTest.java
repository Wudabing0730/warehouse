package com.warehouse.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P0-4 修复验证:前端 el-date-picker type=datetime 提交 "yyyy-MM-dd HH:mm:ss",
 * 后端 DTO 必须能正确反序列化为 LocalDateTime。
 *
 * Bug 复现:修复前 DTO 字段是 LocalDate,Jackson 收到 "2026-06-18 14:30:00" 这种
 * 含时分秒的字符串时反序列化抛 InvalidFormatException,@RequestBody 整体 400,
 * 借出/归还接口永远不能成功提交。
 *
 * 修复:DTO/Entity/VO/BorrowQueryDTO 的日期字段统一改为 LocalDateTime,
 *     并加 @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") 显式声明格式。
 *     数据库 DATE → DATETIME(见 sql/01-schema.sql)。
 */
class BorrowDtoDateTimeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("BorrowCreateDTO 应能反序列化 'yyyy-MM-dd HH:mm:ss' datetime 字符串")
    void borrowCreateDtoAcceptsDateTimeString() throws Exception {
        String json = "{\"productId\":1,\"quantity\":10,\"borrower\":\"张三\","
                + "\"borrowDate\":\"2026-06-18 09:30:00\","
                + "\"expectedReturnDate\":\"2026-07-18 17:00:00\"}";

        BorrowCreateDTO dto = mapper.readValue(json, BorrowCreateDTO.class);

        assertNotNull(dto.getBorrowDate(), "borrowDate 必须成功反序列化为 LocalDateTime");
        assertNotNull(dto.getExpectedReturnDate());
        assertEquals(2026, dto.getBorrowDate().getYear());
        assertEquals(6, dto.getBorrowDate().getMonthValue());
        assertEquals(18, dto.getBorrowDate().getDayOfMonth());
        assertEquals(9, dto.getBorrowDate().getHour());
        assertEquals(30, dto.getBorrowDate().getMinute());
    }

    @Test
    @DisplayName("BorrowReturnDTO 应能反序列化 'yyyy-MM-dd HH:mm:ss' datetime 字符串")
    void borrowReturnDtoAcceptsDateTimeString() throws Exception {
        String json = "{\"returnQuantity\":5,"
                + "\"actualReturnDate\":\"2026-06-18 16:45:30\"}";

        BorrowReturnDTO dto = mapper.readValue(json, BorrowReturnDTO.class);

        assertNotNull(dto.getActualReturnDate(), "actualReturnDate 必须成功反序列化为 LocalDateTime");
        assertEquals(2026, dto.getActualReturnDate().getYear());
        assertEquals(16, dto.getActualReturnDate().getHour());
        assertEquals(45, dto.getActualReturnDate().getMinute());
        assertEquals(30, dto.getActualReturnDate().getSecond());
    }

    @Test
    @DisplayName("LocalDateTime 字段在序列化时应输出 'yyyy-MM-dd HH:mm:ss' 字符串(前后端协议对齐)")
    void borrowCreateDtoSerializesDateTimeConsistently() throws Exception {
        BorrowCreateDTO dto = new BorrowCreateDTO();
        dto.setProductId(1L);
        dto.setQuantity(new java.math.BigDecimal("10"));
        dto.setBorrower("李四");
        dto.setBorrowDate(LocalDateTime.of(2026, 6, 18, 9, 30, 0));
        dto.setExpectedReturnDate(LocalDateTime.of(2026, 7, 18, 17, 0, 0));

        String json = mapper.writeValueAsString(dto);

        assertEquals(true, json.contains("2026-06-18 09:30:00"),
                "序列化应保留时分秒,实际 JSON=" + json);
        assertEquals(true, json.contains("2026-07-18 17:00:00"));
    }
}