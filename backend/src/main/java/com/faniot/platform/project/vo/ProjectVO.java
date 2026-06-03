package com.faniot.platform.project.vo;

import com.faniot.platform.project.domain.Project;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "项目信息")
public record ProjectVO(
        Long id,
        String projectId,
        String customerId,
        String customerName,
        String projectName,
        String location,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProjectVO from(Project project, String customerName) {
        return new ProjectVO(
                project.getId(),
                project.getProjectId(),
                project.getCustomerId(),
                customerName,
                project.getProjectName(),
                project.getLocation(),
                project.getRemark(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
