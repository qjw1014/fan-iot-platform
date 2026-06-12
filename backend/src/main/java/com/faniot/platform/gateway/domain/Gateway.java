package com.faniot.platform.gateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "gateways")
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway_id", nullable = false, unique = true, length = 64)
    private String gatewayId;

    @Column(name = "gateway_sn", nullable = false, unique = true, length = 96)
    private String gatewaySn;

    @Column(name = "gateway_name", nullable = false, length = 120)
    private String gatewayName;

    @Column(name = "gateway_model", length = 120)
    private String gatewayModel;

    @Column(length = 32)
    private String imei;

    @Column(length = 32)
    private String iccid;

    @Column(name = "sim_card_no", length = 32)
    private String simCardNo;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "activation_status", nullable = false, length = 32)
    private String activationStatus = "inactive";

    @Column(name = "mqtt_username", length = 120)
    private String mqttUsername;

    @Column(name = "mqtt_client_id", length = 96)
    private String mqttClientId;

    @Column(name = "mqtt_password_hash")
    private String mqttPasswordHash;

    @Column(name = "publish_topic", length = 128)
    private String publishTopic;

    @Column(name = "subscribe_topic", length = 128)
    private String subscribeTopic;

    @Column(name = "mqtt_version", nullable = false, length = 16)
    private String mqttVersion = "3.1.1";

    @Column(nullable = false)
    private Integer qos = 1;

    @Column(nullable = false)
    private Integer keepalive = 60;

    @Column(name = "tls_enabled", nullable = false)
    private Boolean tlsEnabled = false;

    @Column(name = "online_status", nullable = false, length = 32)
    private String onlineStatus = "offline";

    @Column(name = "firmware_version", length = 64)
    private String firmwareVersion;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_config_time")
    private OffsetDateTime lastConfigTime;

    @Column(name = "remote_config_supported", nullable = false)
    private Boolean remoteConfigSupported = true;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String province;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String district;

    @Column(name = "location_updated_at")
    private OffsetDateTime locationUpdatedAt;

    @Column(name = "location_source", nullable = false, length = 16)
    private String locationSource = "manual";

    @Column(name = "last_location_time")
    private OffsetDateTime lastLocationTime;

    @Column(name = "lbs_mcc")
    private Integer lbsMcc;

    @Column(name = "lbs_mnc")
    private Integer lbsMnc;

    @Column(name = "lbs_lac")
    private Long lbsLac;

    @Column(name = "lbs_cid")
    private Long lbsCid;

    @Column(name = "lbs_accuracy")
    private Integer lbsAccuracy;

    @Column(length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getGatewaySn() {
        return gatewaySn;
    }

    public void setGatewaySn(String gatewaySn) {
        this.gatewaySn = gatewaySn;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getGatewayModel() {
        return gatewayModel;
    }

    public void setGatewayModel(String gatewayModel) {
        this.gatewayModel = gatewayModel;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getSimCardNo() {
        return simCardNo;
    }

    public void setSimCardNo(String simCardNo) {
        this.simCardNo = simCardNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(String activationStatus) {
        this.activationStatus = activationStatus;
    }

    public String getMqttUsername() {
        return mqttUsername;
    }

    public void setMqttUsername(String mqttUsername) {
        this.mqttUsername = mqttUsername;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public void setMqttClientId(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }

    public String getMqttPasswordHash() {
        return mqttPasswordHash;
    }

    public void setMqttPasswordHash(String mqttPasswordHash) {
        this.mqttPasswordHash = mqttPasswordHash;
    }

    public String getPublishTopic() {
        return publishTopic;
    }

    public void setPublishTopic(String publishTopic) {
        this.publishTopic = publishTopic;
    }

    public String getSubscribeTopic() {
        return subscribeTopic;
    }

    public void setSubscribeTopic(String subscribeTopic) {
        this.subscribeTopic = subscribeTopic;
    }

    public String getMqttVersion() {
        return mqttVersion;
    }

    public void setMqttVersion(String mqttVersion) {
        this.mqttVersion = mqttVersion;
    }

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public Integer getKeepalive() {
        return keepalive;
    }

    public void setKeepalive(Integer keepalive) {
        this.keepalive = keepalive;
    }

    public Boolean getTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(Boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public OffsetDateTime getLastConfigTime() {
        return lastConfigTime;
    }

    public void setLastConfigTime(OffsetDateTime lastConfigTime) {
        this.lastConfigTime = lastConfigTime;
    }

    public Boolean getRemoteConfigSupported() {
        return remoteConfigSupported;
    }

    public void setRemoteConfigSupported(Boolean remoteConfigSupported) {
        this.remoteConfigSupported = remoteConfigSupported;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public OffsetDateTime getLocationUpdatedAt() {
        return locationUpdatedAt;
    }

    public void setLocationUpdatedAt(OffsetDateTime locationUpdatedAt) {
        this.locationUpdatedAt = locationUpdatedAt;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public OffsetDateTime getLastLocationTime() {
        return lastLocationTime;
    }

    public void setLastLocationTime(OffsetDateTime lastLocationTime) {
        this.lastLocationTime = lastLocationTime;
    }

    public Integer getLbsMcc() {
        return lbsMcc;
    }

    public void setLbsMcc(Integer lbsMcc) {
        this.lbsMcc = lbsMcc;
    }

    public Integer getLbsMnc() {
        return lbsMnc;
    }

    public void setLbsMnc(Integer lbsMnc) {
        this.lbsMnc = lbsMnc;
    }

    public Long getLbsLac() {
        return lbsLac;
    }

    public void setLbsLac(Long lbsLac) {
        this.lbsLac = lbsLac;
    }

    public Long getLbsCid() {
        return lbsCid;
    }

    public void setLbsCid(Long lbsCid) {
        this.lbsCid = lbsCid;
    }

    public Integer getLbsAccuracy() {
        return lbsAccuracy;
    }

    public void setLbsAccuracy(Integer lbsAccuracy) {
        this.lbsAccuracy = lbsAccuracy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
