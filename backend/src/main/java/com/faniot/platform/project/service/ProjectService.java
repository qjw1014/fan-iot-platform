package com.faniot.platform.project.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.customer.domain.Customer;
import com.faniot.platform.customer.repository.CustomerRepository;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.project.domain.Project;
import com.faniot.platform.project.dto.ProjectRequest;
import com.faniot.platform.project.repository.ProjectRepository;
import com.faniot.platform.project.vo.ProjectVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final GatewayRepository gatewayRepository;
    private final DeviceRepository deviceRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            CustomerRepository customerRepository,
            GatewayRepository gatewayRepository,
            DeviceRepository deviceRepository
    ) {
        this.projectRepository = projectRepository;
        this.customerRepository = customerRepository;
        this.gatewayRepository = gatewayRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectVO> page(String keyword, String customerId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(projectRepository.findAll(buildSpec(keyword, customerId), pageable).map(this::toVO));
    }

    @Transactional(readOnly = true)
    public List<ProjectVO> list(String customerId) {
        List<Project> projects = StringUtils.hasText(customerId)
                ? projectRepository.findByCustomerIdOrderByIdDesc(customerId)
                : projectRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return projects.stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public ProjectVO detail(Long id) {
        return toVO(getById(id));
    }

    @Transactional
    public ProjectVO create(ProjectRequest request) {
        if (projectRepository.existsByProjectId(request.projectId())) {
            throw new BusinessException("项目编号已存在");
        }
        ensureCustomerExists(request.customerId());
        Project project = new Project();
        apply(project, request);
        return toVO(projectRepository.save(project));
    }

    @Transactional
    public ProjectVO update(Long id, ProjectRequest request) {
        Project project = getById(id);
        if (projectRepository.existsByProjectIdAndIdNot(request.projectId(), id)) {
            throw new BusinessException("项目编号已存在");
        }
        ensureCustomerExists(request.customerId());
        apply(project, request);
        return toVO(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        Project project = getById(id);
        String projectId = project.getProjectId();
        if (gatewayRepository.existsByProjectId(projectId) || deviceRepository.existsByProjectId(projectId)) {
            throw new BusinessException("项目已关联盒子或设备，不能删除");
        }
        projectRepository.delete(project);
    }

    private Project getById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private void apply(Project project, ProjectRequest request) {
        project.setProjectId(request.projectId());
        project.setCustomerId(request.customerId());
        project.setProjectName(request.projectName());
        project.setLocation(request.location());
        project.setRemark(request.remark());
    }

    private void ensureCustomerExists(String customerId) {
        if (!customerRepository.existsByCustomerId(customerId)) {
            throw new BusinessException("客户不存在");
        }
    }

    private ProjectVO toVO(Project project) {
        String customerName = customerRepository.findByCustomerId(project.getCustomerId())
                .map(Customer::getCustomerName)
                .orElse(null);
        return ProjectVO.from(project, customerName);
    }

    private Specification<Project> buildSpec(String keyword, String customerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("projectId"), like),
                        cb.like(root.get("projectName"), like),
                        cb.like(root.get("location"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
