package com.warehouse.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportService {

    /**
     * 入库汇总报表
     */
    List<Map<String, Object>> inboundReport(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 出库汇总报表
     */
    List<Map<String, Object>> outboundReport(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 库存报表
     */
    List<Map<String, Object>> stockReport(Long productId, Long locationId);
}
