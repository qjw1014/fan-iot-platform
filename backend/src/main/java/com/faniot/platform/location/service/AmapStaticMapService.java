package com.faniot.platform.location.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.location.config.LbsProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Service
public class AmapStaticMapService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LbsProperties properties;

    public AmapStaticMapService(ObjectMapper objectMapper, LbsProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.readTimeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public byte[] render(BigDecimal longitude, BigDecimal latitude, int zoom, int width, int height) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException("高德地图 Web 服务 Key 未配置");
        }
        if (longitude == null || latitude == null
                || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0
                || latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException("地图经纬度不合法");
        }

        int safeZoom = Math.min(Math.max(zoom, 3), 18);
        int safeWidth = Math.min(Math.max(width, 240), 1024);
        int safeHeight = Math.min(Math.max(height, 180), 1024);
        String coordinate = longitude.toPlainString() + "," + latitude.toPlainString();
        String uri = UriComponentsBuilder.fromUriString(properties.mapBaseUrl())
                .queryParam("key", properties.apiKey())
                .queryParam("location", coordinate)
                .queryParam("zoom", safeZoom)
                .queryParam("size", safeWidth + "*" + safeHeight)
                .queryParam("markers", "mid,0x1677FF,A:" + coordinate)
                .build()
                .encode()
                .toUriString();

        ResponseEntity<byte[]> response;
        try {
            response = restClient.get().uri(uri).retrieve().toEntity(byte[].class);
        } catch (Exception ex) {
            throw new BusinessException("高德静态地图加载失败，请检查网络、Key权限和配额");
        }

        byte[] body = response.getBody();
        MediaType contentType = response.getHeaders().getContentType();
        if (body == null || body.length == 0) {
            throw new BusinessException("高德静态地图未返回图片");
        }
        if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
            throw new BusinessException(amapError(body));
        }
        return body;
    }

    private String amapError(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String info = root.path("info").asText("未知错误");
            String infoCode = root.path("infocode").asText("");
            return "高德静态地图加载失败：" + info + (infoCode.isBlank() ? "" : "（" + infoCode + "）");
        } catch (Exception ignored) {
            return "高德静态地图响应无法解析";
        }
    }
}
