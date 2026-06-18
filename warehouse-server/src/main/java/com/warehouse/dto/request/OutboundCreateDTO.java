package com.warehouse.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundCreateDTO {
    private Long customerId;

    private String department;

    private String applicant;

    @NotNull
    private LocalDateTime orderTime;

    private String remark;

    @NotEmpty
    private List<OutboundDetailDTO> details;
}
