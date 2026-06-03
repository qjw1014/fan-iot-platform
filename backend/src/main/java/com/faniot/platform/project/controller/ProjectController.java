package com.faniot.platform.project.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.project.dto.ProjectRequest;
import com.faniot.platform.project.service.ProjectService;
import com.faniot.platform.project.vo.ProjectVO;
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

@Tag(name = "项目管理", description = "项目新增、编辑、删除和分页查询")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "分页查询项目")
    @GetMapping
    public ApiResponse<PageResponse<ProjectVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(projectService.page(keyword, customerId, page, size));
    }

    @Operation(summary = "项目下拉列表")
    @GetMapping("/options")
    public ApiResponse<List<ProjectVO>> options(@RequestParam(required = false) String customerId) {
        return ApiResponse.ok(projectService.list(customerId));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public ApiResponse<ProjectVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(projectService.detail(id));
    }

    @Operation(summary = "新增项目")
    @PostMapping
    public ApiResponse<ProjectVO> create(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.ok(projectService.create(request));
    }

    @Operation(summary = "编辑项目")
    @PutMapping("/{id}")
    public ApiResponse<ProjectVO> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.ok(projectService.update(id, request));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.ok();
    }
}
