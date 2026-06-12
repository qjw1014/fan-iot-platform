package com.faniot.platform.location.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.location.dto.LbsLocationRequest;
import com.faniot.platform.location.service.LbsLocationService;
import com.faniot.platform.location.vo.LbsLocationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "D200基站定位", description = "接收CID/LAC并解析盒子位置")
@RestController
@RequestMapping("/api/iot/location")
public class LbsLocationController {

    private final LbsLocationService lbsLocationService;

    public LbsLocationController(LbsLocationService lbsLocationService) {
        this.lbsLocationService = lbsLocationService;
    }

    @Operation(summary = "解析并更新D200基站位置")
    @PostMapping("/lbs")
    public ApiResponse<LbsLocationVO> locate(@Valid @RequestBody LbsLocationRequest request) {
        return ApiResponse.ok(lbsLocationService.locate(request));
    }
}
