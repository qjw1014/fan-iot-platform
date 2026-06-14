package com.faniot.platform.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.location.config.LbsProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellocationLbsClientTest {

    @Test
    void parsesAmapAddressFieldsFromResponseRoot() throws Exception {
        LbsProperties properties = new LbsProperties(
                true,
                "amap",
                "https://restapi.amap.com/v5/position/IoT",
                "test-key",
                "https://restapi.amap.com/v3/staticmap",
                460,
                3,
                "GSM",
                -99,
                30,
                5,
                8
        );
        CellocationLbsClient client = new CellocationLbsClient(new ObjectMapper(), properties);

        LbsProviderClient.Result result = client.parseAmapResponse("""
                {
                  "status": "1",
                  "info": "OK",
                  "position": {
                    "location": "113.864181,22.633859",
                    "radius": 550
                  },
                  "formatted_address": "广东省 深圳市 宝安区",
                  "addressComponent": {
                    "province": "广东省",
                    "city": "深圳市",
                    "district": "宝安区"
                  }
                }
                """);

        assertEquals(new BigDecimal("22.633859"), result.latitude());
        assertEquals(new BigDecimal("113.864181"), result.longitude());
        assertEquals(550, result.accuracy());
        assertEquals("广东省 深圳市 宝安区", result.address());
        assertEquals("广东省", result.province());
        assertEquals("深圳市", result.city());
        assertEquals("宝安区", result.district());
    }
}
