package com.faniot.platform.location.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LbsLocationVO(
        String gatewayId,
        String gatewaySn,
        Integer mcc,
        Integer mnc,
        Long lac,
        Long cid,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer accuracy,
        String address,
        String locationSource,
        boolean gatewayLocationUpdated,
        OffsetDateTime locatedAt
) {
}
