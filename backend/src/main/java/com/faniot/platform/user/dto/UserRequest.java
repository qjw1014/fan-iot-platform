package com.faniot.platform.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "用户保存请求")
public record UserRequest(
        @Schema(description = "用户名") @NotBlank @Size(max = 64) String username,
        @Schema(description = "密码，编辑时可为空") @Size(max = 120) String password,
        @Schema(description = "姓名") @Size(max = 100) String realName,
        @Schema(description = "手机") @Size(max = 32) String phone,
        @Schema(description = "邮箱") @Email @Size(max = 160) String email,
        @Schema(description = "状态") @Pattern(regexp = "enabled|disabled|locked") String status,
        @Schema(description = "角色编码列表") List<String> roleCodes
) {
}
