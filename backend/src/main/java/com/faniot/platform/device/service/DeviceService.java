package com.faniot.platform.device.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.customer.domain.Customer;
import com.faniot.platform.customer.repository.CustomerRepository;
import com.faniot.platform.device.domain.Device;
import com.faniot.platform.device.dto.DeviceLocationRequest;
import com.faniot.platform.device.dto.DeviceRequest;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.device.vo.DeviceLocationVO;
import com.faniot.platform.device.vo.DeviceVO;
import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.project.domain.Project;
import com.faniot.platform.project.repository.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GatewayRepository gatewayRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final JdbcTemplate jdbcTemplate;

    public DeviceService(
            DeviceRepository deviceRepository,
            GatewayRepository gatewayRepository,
            CustomerRepository customerRepository,
            ProjectRepository projectRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.deviceRepository = deviceRepository;
        this.gatewayRepository = gatewayRepository;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<DeviceVO> page(String keyword, String customerId, String projectId, String gatewayId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(deviceRepository.findAll(buildSpec(keyword, customerId, projectId, gatewayId, status), pageable).map(this::toVO));
    }

    @Transactional(readOnly = true)
    public DeviceVO detail(Long id) {
        return toVO(getById(id));
    }

    @Transactional(readOnly = true)
    public DeviceVO detailByDeviceId(String deviceId) {
        return toVO(getByDeviceId(deviceId));
    }

    @Transactional(readOnly = true)
    public DeviceLocationVO location(String deviceId) {
        Device device = getByDeviceId(deviceId);
        Gateway gateway = gatewayRepository.findByGatewayId(device.getGatewayId()).orElse(null);
        EffectiveLocation location = effectiveLocation(device, gateway);
        return DeviceLocationVO.from(
                device,
                location.latitude(),
                location.longitude(),
                location.address(),
                location.source(),
                location.lastLocationTime()
        );
    }

    @Transactional
    public DeviceLocationVO updateLocation(String deviceId, DeviceLocationRequest request) {
        Device device = getByDeviceId(deviceId);
        device.setInstallLocation(request.installLocation());
        device.setLatitude(request.latitude());
        device.setLongitude(request.longitude());
        device.setAddress(request.address());
        Device saved = deviceRepository.save(device);
        return DeviceLocationVO.from(
                saved,
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getAddress(),
                "manual",
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public DeviceVO create(DeviceRequest request) {
        if (deviceRepository.existsByDeviceId(request.deviceId())) {
            throw new BusinessException("设备编号已存在");
        }
        ensureRelations(request);
        Device device = new Device();
        apply(device, request);
        return toVO(deviceRepository.save(device));
    }

    @Transactional
    public DeviceVO update(Long id, DeviceRequest request) {
        Device device = getById(id);
        if (deviceRepository.existsByDeviceIdAndIdNot(request.deviceId(), id)) {
            throw new BusinessException("设备编号已存在");
        }
        ensureRelations(request);
        apply(device, request);
        return toVO(deviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        Device device = getById(id);
        Long telemetryCount = jdbcTemplate.queryForObject("select count(*) from device_telemetry where device_id = ?", Long.class, device.getDeviceId());
        if (telemetryCount != null && telemetryCount > 0) {
            throw new BusinessException("设备已有历史数据，不能删除");
        }
        deviceRepository.delete(device);
    }

    private Device getById(Long id) {
        return deviceRepository.findById(id).orElseThrow(() -> new BusinessException("设备不存在"));
    }

    private Device getByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(() -> new BusinessException("设备不存在"));
    }

    private void apply(Device device, DeviceRequest request) {
        device.setDeviceId(request.deviceId());
        device.setGatewayId(request.gatewayId());
        device.setCustomerId(blankToNull(request.customerId()));
        device.setProjectId(blankToNull(request.projectId()));
        device.setDeviceName(request.deviceName());
        device.setDeviceModel(request.deviceModel());
        device.setInstallLocation(request.installLocation());
        device.setLatitude(request.latitude());
        device.setLongitude(request.longitude());
        device.setAddress(request.address());
        device.setStatus(StringUtils.hasText(request.status()) ? request.status() : "offline");
        device.setRemark(request.remark());
    }

    private void ensureRelations(DeviceRequest request) {
        Gateway gateway = gatewayRepository.findByGatewayId(request.gatewayId())
                .orElseThrow(() -> new BusinessException("盒子不存在"));
        if (StringUtils.hasText(request.customerId()) && !customerRepository.existsByCustomerId(request.customerId())) {
            throw new BusinessException("客户不存在");
        }
        if (StringUtils.hasText(request.projectId())) {
            Project project = projectRepository.findByProjectId(request.projectId())
                    .orElseThrow(() -> new BusinessException("项目不存在"));
            if (StringUtils.hasText(request.customerId()) && !project.getCustomerId().equals(request.customerId())) {
                throw new BusinessException("项目不属于所选客户");
            }
        }
        if (StringUtils.hasText(request.customerId()) && StringUtils.hasText(gateway.getCustomerId())
                && !gateway.getCustomerId().equals(request.customerId())) {
            throw new BusinessException("盒子不属于所选客户");
        }
        if (StringUtils.hasText(request.projectId()) && StringUtils.hasText(gateway.getProjectId())
                && !gateway.getProjectId().equals(request.projectId())) {
            throw new BusinessException("盒子不属于所选项目");
        }
    }

    private DeviceVO toVO(Device device) {
        Gateway gateway = gatewayRepository.findByGatewayId(device.getGatewayId()).orElse(null);
        String gatewayName = gateway == null ? null : gateway.getGatewayName();
        String gatewaySn = gateway == null ? null : gateway.getGatewaySn();
        String customerName = StringUtils.hasText(device.getCustomerId())
                ? customerRepository.findByCustomerId(device.getCustomerId()).map(Customer::getCustomerName).orElse(null)
                : null;
        String projectName = StringUtils.hasText(device.getProjectId())
                ? projectRepository.findByProjectId(device.getProjectId()).map(Project::getProjectName).orElse(null)
                : null;
        EffectiveLocation location = effectiveLocation(device, gateway);
        return DeviceVO.from(
                device,
                gatewaySn,
                gatewayName,
                customerName,
                projectName,
                location.latitude(),
                location.longitude(),
                location.address(),
                location.source(),
                location.lastLocationTime()
        );
    }

    private EffectiveLocation effectiveLocation(Device device, Gateway gateway) {
        if (device.getLatitude() != null && device.getLongitude() != null) {
            return new EffectiveLocation(
                    device.getLatitude(),
                    device.getLongitude(),
                    device.getAddress(),
                    "manual",
                    device.getUpdatedAt()
            );
        }
        if (gateway != null && gateway.getLatitude() != null && gateway.getLongitude() != null) {
            return new EffectiveLocation(
                    gateway.getLatitude(),
                    gateway.getLongitude(),
                    gateway.getAddress(),
                    gateway.getLocationSource(),
                    gateway.getLastLocationTime()
            );
        }
        return new EffectiveLocation(null, null, device.getAddress(), null, null);
    }

    private record EffectiveLocation(
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            String address,
            String source,
            java.time.OffsetDateTime lastLocationTime
    ) {
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private Specification<Device> buildSpec(String keyword, String customerId, String projectId, String gatewayId, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (StringUtils.hasText(projectId)) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (StringUtils.hasText(gatewayId)) {
                predicates.add(cb.equal(root.get("gatewayId"), gatewayId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("deviceId"), like),
                        cb.like(root.get("deviceName"), like),
                        cb.like(root.get("deviceModel"), like),
                        cb.like(root.get("installLocation"), like),
                        cb.like(root.get("address"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
