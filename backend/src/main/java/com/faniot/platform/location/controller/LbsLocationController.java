package com.faniot.platform.location.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.location.dto.LbsLocationRequest;
import com.faniot.platform.location.service.AmapStaticMapService;
import com.faniot.platform.location.service.LbsLocationService;
import com.faniot.platform.location.vo.LbsLocationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Tag(name = "D200基站定位", description = "接收CID/LAC并解析盒子位置")
@RestController
@RequestMapping("/api/iot/location")
public class LbsLocationController {

    private final LbsLocationService lbsLocationService;
    private final AmapStaticMapService amapStaticMapService;

    public LbsLocationController(
            LbsLocationService lbsLocationService,
            AmapStaticMapService amapStaticMapService
    ) {
        this.lbsLocationService = lbsLocationService;
        this.amapStaticMapService = amapStaticMapService;
    }

    @Operation(summary = "解析并更新D200基站位置")
    @PostMapping("/lbs")
    public ApiResponse<LbsLocationVO> locate(@Valid @RequestBody LbsLocationRequest request) {
        return ApiResponse.ok(lbsLocationService.locate(request));
    }

    @Operation(summary = "获取高德静态定位地图")
    @GetMapping(value = "/map", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> map(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude,
            @RequestParam(defaultValue = "15") int zoom,
            @RequestParam(defaultValue = "720") int width,
            @RequestParam(defaultValue = "480") int height
    ) {
        byte[] image = amapStaticMapService.render(longitude, latitude, zoom, width, height);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(image);
    }
}
