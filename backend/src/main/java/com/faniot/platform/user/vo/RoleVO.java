package com.faniot.platform.user.vo;

import com.faniot.platform.user.domain.Role;

public record RoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        Boolean enabled
) {
    public static RoleVO from(Role role) {
        return new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(), role.getDescription(), role.getEnabled());
    }
}
