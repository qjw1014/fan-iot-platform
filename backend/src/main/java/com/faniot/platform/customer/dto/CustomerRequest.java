package com.faniot.platform.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "客户保存请求")
public record CustomerRequest(
        @Schema(description = "客户编号") @NotBlank @Size(max = 64) String customerId,
        @Schema(description = "客户名称") @NotBlank @Size(max = 160) String customerName,
        @Schema(description = "联系人") @Size(max = 100) String contactPerson,
        @Schema(description = "联系电话") @Size(max = 32) String contactPhone,
        @Schema(description = "备注") @Size(max = 500) String remark
) {
}
