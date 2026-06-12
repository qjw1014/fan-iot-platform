package com.faniot.platform.gateway.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.customer.domain.Customer;
import com.faniot.platform.customer.repository.CustomerRepository;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.dto.GatewayRequest;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.gateway.vo.GatewayVO;
import com.faniot.platform.project.domain.Project;
import com.faniot.platform.project.repository.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    public GatewayService(
            GatewayRepository gatewayRepository,
            CustomerRepository customerRepository,
            ProjectRepository projectRepository,
            DeviceRepository deviceRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.gatewayRepository = gatewayRepository;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.deviceRepository = deviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<GatewayVO> page(String keyword, String customerId, String projectId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(gatewayRepository.findAll(buildSpec(keyword, customerId, projectId), pageable).map(this::toVO));
    }

    @Transactional(readOnly = true)
    public List<GatewayVO> list(String customerId, String projectId) {
        List<Gateway> gateways;
        if (StringUtils.hasText(projectId)) {
            gateways = gatewayRepository.findByProjectIdOrderByIdDesc(projectId);
        } else if (StringUtils.hasText(customerId)) {
            gateways = gatewayRepository.findByCustomerIdOrderByIdDesc(customerId);
        } else {
            gateways = gatewayRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }
        return gateways.stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public GatewayVO detail(Long id) {
        return toVO(getById(id));
    }

    @Transactional
    public GatewayVO create(GatewayRequest request) {
        if (gatewayRepository.existsByGatewayId(request.gatewayId())) {
            throw new BusinessException("盒子编号已存在");
        }
        if (gatewayRepository.existsByGatewaySn(request.gatewaySn())) {
            throw new BusinessException("盒子序列号已存在");
        }
        ensureRelations(request.customerId(), request.projectId());
        Gateway gateway = new Gateway();
        apply(gateway, request);
        return toVO(gatewayRepository.save(gateway));
    }

    @Transactional
    public GatewayVO update(Long id, GatewayRequest request) {
        Gateway gateway = getById(id);
        if (gatewayRepository.existsByGatewayIdAndIdNot(request.gatewayId(), id)) {
            throw new BusinessException("盒子编号已存在");
        }
        if (gatewayRepository.existsByGatewaySnAndIdNot(request.gatewaySn(), id)) {
            throw new BusinessException("盒子序列号已存在");
        }
        ensureRelations(request.customerId(), request.projectId());
        apply(gateway, request);
        return toVO(gatewayRepository.save(gateway));
    }

    @Transactional
    public void delete(Long id) {
        Gateway gateway = getById(id);
        if (deviceRepository.existsByGatewayId(gateway.getGatewayId())) {
            throw new BusinessException("盒子已关联设备，不能删除");
        }
        gatewayRepository.delete(gateway);
    }

    private Gateway getById(Long id) {
        return gatewayRepository.findById(id).orElseThrow(() -> new BusinessException("盒子不存在"));
    }

    private void apply(Gateway gateway, GatewayRequest request) {
        gateway.setGatewayId(request.gatewayId());
        gateway.setGatewaySn(request.gatewaySn());
        gateway.setGatewayName(request.gatewayName());
        gateway.setGatewayModel(StringUtils.hasText(request.gatewayModel()) ? request.gatewayModel() : "D200");
        gateway.setImei(request.imei());
        gateway.setIccid(request.iccid());
        gateway.setSimCardNo(request.simCardNo());
        gateway.setCustomerId(blankToNull(request.customerId()));
        gateway.setProjectId(blankToNull(request.projectId()));
        gateway.setActivationStatus(StringUtils.hasText(request.activationStatus()) ? request.activationStatus() : "inactive");
        if (gateway.getId() == null) {
            gateway.setOnlineStatus("offline");
        }
        gateway.setMqttClientId(StringUtils.hasText(request.mqttClientId()) ? request.mqttClientId() : request.gatewaySn());
        gateway.setMqttUsername(blankToNull(request.mqttUsername()));
        if (StringUtils.hasText(request.mqttPassword())) {
            gateway.setMqttPasswordHash(passwordEncoder.encode(request.mqttPassword()));
        }
        gateway.setPublishTopic(StringUtils.hasText(request.publishTopic()) ? request.publishTopic() : d200UpTopic(request.gatewaySn()));
        gateway.setSubscribeTopic(StringUtils.hasText(request.subscribeTopic()) ? request.subscribeTopic() : d200DownTopic(request.gatewaySn()));
        gateway.setMqttVersion(StringUtils.hasText(request.mqttVersion()) ? request.mqttVersion() : "3.1.1");
        gateway.setQos(request.qos() == null ? 1 : request.qos());
        gateway.setKeepalive(request.keepalive() == null ? 60 : request.keepalive());
        gateway.setTlsEnabled(Boolean.TRUE.equals(request.tlsEnabled()));
        gateway.setRemoteConfigSupported(request.remoteConfigSupported() == null || request.remoteConfigSupported());
        gateway.setFirmwareVersion(request.firmwareVersion());
        gateway.setLatitude(request.latitude());
        gateway.setLongitude(request.longitude());
        gateway.setAddress(request.address());
        gateway.setProvince(request.province());
        gateway.setCity(request.city());
        gateway.setDistrict(request.district());
        gateway.setLocationSource(StringUtils.hasText(request.locationSource()) ? request.locationSource() : "manual");
        if (request.latitude() != null || request.longitude() != null || StringUtils.hasText(request.address())) {
            gateway.setLocationUpdatedAt(OffsetDateTime.now());
            gateway.setLastLocationTime(OffsetDateTime.now());
        }
        gateway.setRemark(request.remark());
    }

    private void ensureRelations(String customerId, String projectId) {
        if (StringUtils.hasText(customerId) && !customerRepository.existsByCustomerId(customerId)) {
            throw new BusinessException("客户不存在");
        }
        if (StringUtils.hasText(projectId)) {
            Project project = projectRepository.findByProjectId(projectId)
                    .orElseThrow(() -> new BusinessException("项目不存在"));
            if (StringUtils.hasText(customerId) && !project.getCustomerId().equals(customerId)) {
                throw new BusinessException("项目不属于所选客户");
            }
        }
    }

    private GatewayVO toVO(Gateway gateway) {
        String customerName = StringUtils.hasText(gateway.getCustomerId())
                ? customerRepository.findByCustomerId(gateway.getCustomerId()).map(Customer::getCustomerName).orElse(null)
                : null;
        String projectName = StringUtils.hasText(gateway.getProjectId())
                ? projectRepository.findByProjectId(gateway.getProjectId()).map(Project::getProjectName).orElse(null)
                : null;
        return GatewayVO.from(gateway, customerName, projectName);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private Specification<Gateway> buildSpec(String keyword, String customerId, String projectId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (StringUtils.hasText(projectId)) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("gatewayId"), like),
                        cb.like(root.get("gatewaySn"), like),
                        cb.like(root.get("gatewayName"), like),
                        cb.like(root.get("imei"), like),
                        cb.like(root.get("iccid"), like),
                        cb.like(root.get("mqttClientId"), like),
                        cb.like(root.get("mqttUsername"), like),
                        cb.like(root.get("publishTopic"), like),
                        cb.like(root.get("subscribeTopic"), like),
                        cb.like(root.get("address"), like),
                        cb.like(root.get("city"), like),
                        cb.like(root.get("district"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String d200UpTopic(String gatewaySn) {
        return "iot/d200/" + gatewaySn + "/up";
    }

    private String d200DownTopic(String gatewaySn) {
        return "iot/d200/" + gatewaySn + "/down";
    }
}
