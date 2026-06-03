package com.faniot.platform.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "项目保存请求")
public record ProjectRequest(
        @Schema(description = "项目编号") @NotBlank @Size(max = 64) String projectId,
        @Schema(description = "客户编号") @NotBlank @Size(max = 64) String customerId,
        @Schema(description = "项目名称") @NotBlank @Size(max = 160) String projectName,
        @Schema(description = "项目位置") @Size(max = 255) String location,
        @Schema(description = "备注") @Size(max = 500) String remark
) {
}
