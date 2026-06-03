-- Industrial Fan IoT Private Cloud Platform
-- Phase 1 PostgreSQL schema.
-- Cloud receives data from IoT gateways by MQTT or HTTP.
-- Cloud does not configure Modbus TCP points or collect field devices directly.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    phone VARCHAR(32),
    email VARCHAR(160),
    status VARCHAR(32) NOT NULL DEFAULT 'enabled',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status CHECK (status IN ('enabled', 'disabled', 'locked'))
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(160) NOT NULL,
    contact_person VARCHAR(100),
    contact_phone VARCHAR(32),
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL UNIQUE,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(customer_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    project_name VARCHAR(160) NOT NULL,
    location VARCHAR(255),
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gateways (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL UNIQUE,
    gateway_sn VARCHAR(96) NOT NULL UNIQUE,
    gateway_name VARCHAR(120) NOT NULL,
    gateway_model VARCHAR(120),
    imei VARCHAR(32),
    sim_card_no VARCHAR(32),
    customer_id VARCHAR(64) REFERENCES customers(customer_id) ON UPDATE CASCADE ON DELETE SET NULL,
    project_id VARCHAR(64) REFERENCES projects(project_id) ON UPDATE CASCADE ON DELETE SET NULL,
    activation_status VARCHAR(32) NOT NULL DEFAULT 'inactive',
    mqtt_username VARCHAR(120),
    mqtt_password_hash VARCHAR(255),
    api_key_prefix VARCHAR(32),
    api_key_hash VARCHAR(255),
    online_status VARCHAR(32) NOT NULL DEFAULT 'offline',
    firmware_version VARCHAR(64),
    last_seen_at TIMESTAMPTZ,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_gateways_activation_status CHECK (activation_status IN ('inactive', 'active', 'disabled')),
    CONSTRAINT chk_gateways_online_status CHECK (online_status IN ('online', 'offline'))
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL UNIQUE,
    gateway_id VARCHAR(64) NOT NULL REFERENCES gateways(gateway_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    customer_id VARCHAR(64) REFERENCES customers(customer_id) ON UPDATE CASCADE ON DELETE SET NULL,
    project_id VARCHAR(64) REFERENCES projects(project_id) ON UPDATE CASCADE ON DELETE SET NULL,
    device_name VARCHAR(120) NOT NULL,
    device_model VARCHAR(120),
    install_location VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'offline',
    last_seen_at TIMESTAMPTZ,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_devices_status CHECK (status IN ('online', 'offline', 'alarm', 'maintenance', 'disabled'))
);

CREATE TABLE device_telemetry (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL REFERENCES gateways(gateway_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    device_id VARCHAR(64) NOT NULL REFERENCES devices(device_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    timestamp TIMESTAMPTZ NOT NULL,
    rpm NUMERIC(12, 2),
    current NUMERIC(12, 3),
    voltage NUMERIC(12, 3),
    power NUMERIC(12, 3),
    frequency NUMERIC(12, 3),
    pressure NUMERIC(12, 3),
    airflow NUMERIC(12, 3),
    motor_temperature NUMERIC(8, 2),
    bearing_temperature NUMERIC(8, 2),
    vibration NUMERIC(12, 4),
    status VARCHAR(32) NOT NULL DEFAULT 'normal',
    alarm_code VARCHAR(64),
    raw_payload JSONB,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_device_telemetry_status CHECK (status IN ('normal', 'running', 'stopped', 'warning', 'critical', 'offline', 'fault'))
);

CREATE TABLE alarms (
    id BIGSERIAL PRIMARY KEY,
    alarm_id UUID NOT NULL DEFAULT gen_random_uuid(),
    gateway_id VARCHAR(64) REFERENCES gateways(gateway_id) ON UPDATE CASCADE ON DELETE SET NULL,
    device_id VARCHAR(64) REFERENCES devices(device_id) ON UPDATE CASCADE ON DELETE SET NULL,
    telemetry_id BIGINT REFERENCES device_telemetry(id) ON DELETE SET NULL,
    alarm_type VARCHAR(64) NOT NULL,
    alarm_level VARCHAR(32) NOT NULL,
    alarm_code VARCHAR(64),
    alarm_message VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recovered_at TIMESTAMPTZ,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_alarms_alarm_id UNIQUE (alarm_id),
    CONSTRAINT chk_alarms_type CHECK (alarm_type IN ('temperature_over_limit', 'vibration_over_limit', 'current_over_limit', 'device_offline', 'gateway_offline', 'communication_interrupted')),
    CONSTRAINT chk_alarms_level CHECK (alarm_level IN ('normal', 'warning', 'critical')),
    CONSTRAINT chk_alarms_status CHECK (status IN ('active', 'recovered', 'acknowledged', 'closed'))
);

CREATE TABLE ai_api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_name VARCHAR(120) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    owner_name VARCHAR(120),
    owner_contact VARCHAR(160),
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_api_call_logs (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT REFERENCES ai_api_keys(id) ON DELETE SET NULL,
    key_prefix VARCHAR(32),
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(16) NOT NULL,
    request_params JSONB,
    response_format VARCHAR(16),
    status_code INTEGER,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(1000),
    client_ip INET,
    user_agent VARCHAR(500),
    duration_ms INTEGER,
    called_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ai_api_call_logs_method CHECK (method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE')),
    CONSTRAINT chk_ai_api_call_logs_response_format CHECK (response_format IS NULL OR response_format IN ('json', 'csv'))
);

CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    log_type VARCHAR(64) NOT NULL,
    log_level VARCHAR(32) NOT NULL,
    module VARCHAR(120),
    operation VARCHAR(160),
    message VARCHAR(1000) NOT NULL,
    trace_id VARCHAR(128),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    gateway_id VARCHAR(64) REFERENCES gateways(gateway_id) ON UPDATE CASCADE ON DELETE SET NULL,
    device_id VARCHAR(64) REFERENCES devices(device_id) ON UPDATE CASCADE ON DELETE SET NULL,
    ip_address INET,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_system_logs_level CHECK (log_level IN ('debug', 'info', 'warn', 'error'))
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_customers_customer_name ON customers(customer_name);
CREATE INDEX idx_projects_customer_id ON projects(customer_id);
CREATE INDEX idx_gateways_customer_project ON gateways(customer_id, project_id);
CREATE INDEX idx_gateways_activation_status ON gateways(activation_status);
CREATE INDEX idx_gateways_online_status ON gateways(online_status);
CREATE INDEX idx_gateways_last_seen_at ON gateways(last_seen_at DESC);
CREATE INDEX idx_devices_gateway_id ON devices(gateway_id);
CREATE INDEX idx_devices_customer_project ON devices(customer_id, project_id);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_device_telemetry_device_time ON device_telemetry(device_id, timestamp DESC);
CREATE INDEX idx_device_telemetry_gateway_time ON device_telemetry(gateway_id, timestamp DESC);
CREATE INDEX idx_device_telemetry_time ON device_telemetry(timestamp DESC);
CREATE INDEX idx_alarms_device_time ON alarms(device_id, occurred_at DESC);
CREATE INDEX idx_alarms_gateway_time ON alarms(gateway_id, occurred_at DESC);
CREATE INDEX idx_alarms_level_status ON alarms(alarm_level, status);
CREATE INDEX idx_alarms_active ON alarms(occurred_at DESC) WHERE status = 'active';
CREATE INDEX idx_ai_api_call_logs_key_time ON ai_api_call_logs(api_key_id, called_at DESC);
CREATE INDEX idx_ai_api_call_logs_endpoint_time ON ai_api_call_logs(endpoint, called_at DESC);
CREATE INDEX idx_system_logs_created_at ON system_logs(created_at DESC);
CREATE INDEX idx_system_logs_trace_id ON system_logs(trace_id) WHERE trace_id IS NOT NULL;

CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_projects_updated_at BEFORE UPDATE ON projects FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_gateways_updated_at BEFORE UPDATE ON gateways FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_devices_updated_at BEFORE UPDATE ON devices FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_alarms_updated_at BEFORE UPDATE ON alarms FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_ai_api_keys_updated_at BEFORE UPDATE ON ai_api_keys FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE users IS 'System users, password hash only';
COMMENT ON TABLE roles IS 'System roles';
COMMENT ON TABLE customers IS 'Customer registry';
COMMENT ON TABLE projects IS 'Customer project registry';
COMMENT ON TABLE gateways IS 'IoT gateway registry';
COMMENT ON TABLE devices IS 'Fan device registry under gateways';
COMMENT ON TABLE device_telemetry IS 'Standard telemetry uploaded by IoT gateways through MQTT or HTTP';
COMMENT ON TABLE alarms IS 'Alarm records';
COMMENT ON TABLE ai_api_keys IS 'AI Open API keys, hash and prefix only';
COMMENT ON TABLE ai_api_call_logs IS 'AI Open API call logs';
COMMENT ON TABLE system_logs IS 'System operation and runtime logs';
