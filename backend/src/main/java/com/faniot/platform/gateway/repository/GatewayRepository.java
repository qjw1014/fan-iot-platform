package com.faniot.platform.gateway.repository;

import com.faniot.platform.gateway.domain.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface GatewayRepository extends JpaRepository<Gateway, Long>, JpaSpecificationExecutor<Gateway> {

    Optional<Gateway> findByGatewayId(String gatewayId);

    Optional<Gateway> findByGatewaySn(String gatewaySn);

    Optional<Gateway> findByImei(String imei);

    boolean existsByGatewayId(String gatewayId);

    boolean existsByGatewayIdAndIdNot(String gatewayId, Long id);

    boolean existsByGatewaySn(String gatewaySn);

    boolean existsByGatewaySnAndIdNot(String gatewaySn, Long id);

    boolean existsByCustomerId(String customerId);

    boolean existsByProjectId(String projectId);

    List<Gateway> findByProjectIdOrderByIdDesc(String projectId);

    List<Gateway> findByCustomerIdOrderByIdDesc(String customerId);
}
