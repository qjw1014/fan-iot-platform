package com.faniot.platform.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "D200基站定位请求")
public record LbsLocationRequest(
        @Schema(description = "盒子SN") @Size(max = 96) String gatewaySn,
        @Schema(description = "IMEI") @Size(max = 32) String imei,
        @Schema(description = "移动国家码，例如中国为460") @PositiveOrZero Integer mcc,
        @Schema(description = "移动网络码") @PositiveOrZero Integer mnc,
        @Schema(description = "位置区码LAC") @NotNull @PositiveOrZero Long lac,
        @Schema(description = "基站编号CID/CI") @NotNull @PositiveOrZero Long cid
) {
}
