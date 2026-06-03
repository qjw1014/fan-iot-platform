package com.faniot.platform.project.repository;

import com.faniot.platform.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByProjectId(String projectId);

    boolean existsByProjectId(String projectId);

    boolean existsByProjectIdAndIdNot(String projectId, Long id);

    boolean existsByCustomerId(String customerId);

    List<Project> findByCustomerIdOrderByIdDesc(String customerId);
}
