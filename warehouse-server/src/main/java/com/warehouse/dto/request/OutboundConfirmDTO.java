package com.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OutboundConfirmDTO {
    @NotNull
    private Boolean approved;

    private String remark;
}
