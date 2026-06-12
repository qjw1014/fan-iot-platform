package com.faniot.platform.location.service;

import java.math.BigDecimal;

public interface LbsProviderClient {

    Result locate(int mcc, int mnc, long lac, long cid, String imei);

    record Result(
            BigDecimal latitude,
            BigDecimal longitude,
            Integer accuracy,
            String address,
            String province,
            String city,
            String district,
            String rawResponse
    ) {
    }
}
