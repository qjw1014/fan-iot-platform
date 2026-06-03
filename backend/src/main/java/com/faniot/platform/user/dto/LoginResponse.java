package com.faniot.platform.user.dto;

import com.faniot.platform.user.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "JWT访问令牌") String token,
        @Schema(description = "令牌类型") String tokenType,
        @Schema(description = "用户信息") UserVO user
) {
}
