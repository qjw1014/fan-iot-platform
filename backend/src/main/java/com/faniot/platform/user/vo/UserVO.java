package com.faniot.platform.user.vo;

import com.faniot.platform.user.domain.User;

import java.time.OffsetDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String realName,
        String phone,
        String email,
        String status,
        List<String> roleCodes,
        List<String> roleNames,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserVO from(User user) {
        return from(user, List.of(), List.of());
    }

    public static UserVO from(User user, List<String> roleCodes, List<String> roleNames) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                roleCodes,
                roleNames,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
