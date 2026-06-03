package com.faniot.platform.device.repository;

import com.faniot.platform.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndIdNot(String deviceId, Long id);

    boolean existsByCustomerId(String customerId);

    boolean existsByProjectId(String projectId);

    boolean existsByGatewayId(String gatewayId);
}
