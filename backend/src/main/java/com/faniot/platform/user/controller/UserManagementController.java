package com.faniot.platform.user.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.user.dto.UserRequest;
import com.faniot.platform.user.service.UserManagementService;
import com.faniot.platform.user.vo.RoleVO;
import com.faniot.platform.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户管理", description = "用户、角色和基础权限管理")
@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
    public ApiResponse<PageResponse<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(userManagementService.page(keyword, status, page, size));
    }

    @Operation(summary = "角色选项")
    @GetMapping("/roles")
    public ApiResponse<List<RoleVO>> roles() {
        return ApiResponse.ok(userManagementService.roles());
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody UserRequest request) {
        return ApiResponse.ok(userManagementService.create(request));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ApiResponse.ok(userManagementService.update(id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userManagementService.delete(id);
        return ApiResponse.ok();
    }
}
