package com.warehouse.service.impl;

import com.warehouse.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> inboundReport(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT
                p.product_id AS productId,
                p.product_code AS productCode,
                p.product_name AS productName,
                SUM(d.quantity) AS totalQuantity,
                SUM(d.quantity * COALESCE(d.unit_price, 0)) AS totalAmount,
                COUNT(DISTINCT o.order_id) AS orderCount
            FROM t_inbound_order o
            INNER JOIN t_inbound_order_detail d ON o.order_id = d.order_id
            INNER JOIN t_product p ON d.product_id = p.product_id
            WHERE o.status = 1
              AND o.confirm_time >= ?
              AND o.confirm_time <= ?
            GROUP BY p.product_id, p.product_code, p.product_name
            ORDER BY totalQuantity DESC
            """;
        try {
            return jdbcTemplate.queryForList(sql, startTime, endTime);
        } catch (Exception e) {
            log.error("入库报表查询失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> outboundReport(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT
                p.product_id AS productId,
                p.product_code AS productCode,
                p.product_name AS productName,
                SUM(d.quantity) AS totalQuantity,
                SUM(d.quantity * COALESCE(d.unit_price, 0)) AS totalAmount,
                COUNT(DISTINCT o.order_id) AS orderCount
            FROM t_outbound_order o
            INNER JOIN t_outbound_order_detail d ON o.order_id = d.order_id
            INNER JOIN t_product p ON d.product_id = p.product_id
            WHERE o.status = 1
              AND o.confirm_time >= ?
              AND o.confirm_time <= ?
            GROUP BY p.product_id, p.product_code, p.product_name
            ORDER BY totalQuantity DESC
            """;
        try {
            return jdbcTemplate.queryForList(sql, startTime, endTime);
        } catch (Exception e) {
            log.error("出库报表查询失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> stockReport(Long productId, Long locationId) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                s.stock_id AS stockId,
                p.product_id AS productId,
                p.product_code AS productCode,
                p.product_name AS productName,
                p.unit AS productUnit,
                l.location_id AS locationId,
                l.location_code AS locationCode,
                l.location_name AS locationName,
                s.quantity,
                p.upper_limit AS upperLimit,
                p.lower_limit AS lowerLimit,
                CASE
                    WHEN p.upper_limit IS NOT NULL AND s.quantity > p.upper_limit THEN '超上限'
                    WHEN p.lower_limit IS NOT NULL AND s.quantity < p.lower_limit THEN '低于下限'
                    ELSE '正常'
                END AS status
            FROM t_stock s
            INNER JOIN t_product p ON s.product_id = p.product_id
            INNER JOIN t_warehouse_location l ON s.location_id = l.location_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();
        if (productId != null) {
            sql.append(" AND s.product_id = ?");
            params.add(productId);
        }
        if (locationId != null) {
            sql.append(" AND s.location_id = ?");
            params.add(locationId);
        }
        sql.append(" ORDER BY p.product_code, l.location_code");

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            log.error("库存报表查询失败", e);
            return new ArrayList<>();
        }
    }
}
