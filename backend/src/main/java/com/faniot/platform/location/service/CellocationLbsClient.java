package com.faniot.platform.location.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.location.config.LbsProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Component
public class CellocationLbsClient implements LbsProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LbsProperties properties;

    public CellocationLbsClient(ObjectMapper objectMapper, LbsProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.readTimeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public Result locate(int mcc, int mnc, long lac, long cid, String imei) {
        if ("amap".equalsIgnoreCase(properties.provider())) {
            return locateByAmap(mcc, mnc, lac, cid, imei);
        }
        return locateByCellocation(mcc, mnc, lac, cid);
    }

    private Result locateByCellocation(int mcc, int mnc, long lac, long cid) {
        String uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("mcc", mcc)
                .queryParam("mnc", mnc)
                .queryParam("lac", lac)
                .queryParam("ci", cid)
                .queryParam("coord", "gcj02")
                .queryParam("output", "json")
                .build()
                .toUriString();
        String body;
        try {
            body = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (Exception ex) {
            throw new BusinessException("基站定位服务调用失败：" + ex.getMessage());
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            int errCode = firstInt(root, "errcode", "error", "code");
            BigDecimal latitude = firstDecimal(root, "lat", "latitude");
            BigDecimal longitude = firstDecimal(root, "lon", "lng", "longitude");
            if (errCode != 0 || latitude == null || longitude == null) {
                String message = firstText(root, "errmsg", "message", "reason");
                throw new BusinessException("基站定位失败" + (message == null ? "" : "：" + message));
            }
            return new Result(
                    latitude,
                    longitude,
                    firstNullableInt(root, "accuracy", "radius"),
                    firstText(root, "address", "addr"),
                    null,
                    null,
                    null,
                    body
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("基站定位服务响应无法解析");
        }
    }

    private Result locateByAmap(int mcc, int mnc, long lac, long cid, String imei) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException("高德智能硬件定位Key未配置");
        }
        String bts = "%d,%d,%d,%d,%d,0".formatted(mcc, mnc, lac, cid, properties.signal());
        String uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("key", properties.apiKey())
                .queryParam("diu", imei == null ? "" : imei)
                .queryParam("accesstype", 1)
                .queryParam("cdma", 0)
                .queryParam("network", properties.network())
                .queryParam("bts", bts)
                .queryParam("show_fields", "formatted_address,addressComponent")
                .build()
                .toUriString();
        String body;
        try {
            body = restClient.post().uri(uri).retrieve().body(String.class);
        } catch (Exception ex) {
            throw new BusinessException("高德基站定位服务调用失败，请检查网络、Key权限和配额");
        }
        try {
            return parseAmapResponse(body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("高德基站定位响应无法解析");
        }
    }

    Result parseAmapResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!"1".equals(root.path("status").asText())) {
            String info = root.path("info").asText("未知错误");
            String infoCode = root.path("infocode").asText("");
            throw new BusinessException("高德基站定位失败：" + info + (infoCode.isBlank() ? "" : "（" + infoCode + "）"));
        }

        JsonNode position = root.path("position");
        String[] coordinate = position.path("location").asText("").split(",");
        if (coordinate.length != 2) {
            throw new BusinessException("高德基站定位未返回有效坐标");
        }

        JsonNode component = root.path("addressComponent");
        if (component.isMissingNode() || component.isNull()) {
            component = position.path("addressComponent");
        }
        String address = firstText(root, "formatted_address");
        if (address == null) {
            address = firstText(position, "formatted_address", "desc");
        }

        return new Result(
                new BigDecimal(coordinate[1]),
                new BigDecimal(coordinate[0]),
                firstNullableInt(position, "radius"),
                address,
                firstText(component, "province"),
                firstText(component, "city"),
                firstText(component, "district"),
                body
        );
    }

    private int firstInt(JsonNode root, String... fields) {
        Integer value = firstNullableInt(root, fields);
        return value == null ? 0 : value;
    }

    private Integer firstNullableInt(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull()) {
                return value.asInt();
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                try {
                    return new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    // Try the next supported field.
                }
            }
        }
        return null;
    }

    private String firstText(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}
