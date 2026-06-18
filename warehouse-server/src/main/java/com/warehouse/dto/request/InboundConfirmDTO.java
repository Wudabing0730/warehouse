package com.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InboundConfirmDTO {
    @NotNull
    private Boolean approved;

    private String remark;
}
