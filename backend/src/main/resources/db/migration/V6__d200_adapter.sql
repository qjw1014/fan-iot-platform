ALTER TABLE gateways
    ADD COLUMN IF NOT EXISTS iccid VARCHAR(32),
    ADD COLUMN IF NOT EXISTS mqtt_client_id VARCHAR(96),
    ADD COLUMN IF NOT EXISTS publish_topic VARCHAR(128),
    ADD COLUMN IF NOT EXISTS subscribe_topic VARCHAR(128),
    ADD COLUMN IF NOT EXISTS mqtt_version VARCHAR(16) NOT NULL DEFAULT '3.1.1',
    ADD COLUMN IF NOT EXISTS qos INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS keepalive INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN IF NOT EXISTS tls_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_config_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS remote_config_supported BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS location_source VARCHAR(16) NOT NULL DEFAULT 'manual',
    ADD COLUMN IF NOT EXISTS last_location_time TIMESTAMPTZ;

UPDATE gateways
SET gateway_model = COALESCE(NULLIF(gateway_model, ''), 'D200'),
    mqtt_version = COALESCE(NULLIF(mqtt_version, ''), '3.1.1'),
    tls_enabled = FALSE,
    remote_config_supported = TRUE,
    mqtt_client_id = COALESCE(NULLIF(mqtt_client_id, ''), gateway_sn),
    publish_topic = COALESCE(NULLIF(publish_topic, ''), 'iot/d200/' || gateway_sn || '/up'),
    subscribe_topic = COALESCE(NULLIF(subscribe_topic, ''), 'iot/d200/' || gateway_sn || '/down'),
    location_source = COALESCE(NULLIF(location_source, ''), 'manual'),
    last_location_time = COALESCE(last_location_time, location_updated_at)
WHERE gateway_sn IS NOT NULL;

ALTER TABLE gateways
    ADD CONSTRAINT chk_gateways_mqtt_version CHECK (mqtt_version IN ('3.1', '3.1.1')),
    ADD CONSTRAINT chk_gateways_qos CHECK (qos IN (0, 1, 2)),
    ADD CONSTRAINT chk_gateways_location_source CHECK (location_source IN ('manual', 'lbs'));

CREATE INDEX IF NOT EXISTS idx_gateways_sn ON gateways(gateway_sn);
CREATE INDEX IF NOT EXISTS idx_gateways_imei ON gateways(imei);
CREATE INDEX IF NOT EXISTS idx_gateways_publish_topic ON gateways(publish_topic);

CREATE TABLE IF NOT EXISTS d200_raw_payloads (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64),
    gateway_sn VARCHAR(96),
    topic VARCHAR(128) NOT NULL,
    qos INTEGER,
    imei VARCHAR(32),
    iccid VARCHAR(32),
    device_time TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload JSONB NOT NULL,
    data_payload JSONB,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    process_error VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_d200_raw_gateway_sn ON d200_raw_payloads(gateway_sn);
CREATE INDEX IF NOT EXISTS idx_d200_raw_received_at ON d200_raw_payloads(received_at DESC);
CREATE INDEX IF NOT EXISTS idx_d200_raw_processed ON d200_raw_payloads(processed);

CREATE TABLE IF NOT EXISTS d200_field_mappings (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    source_key VARCHAR(80) NOT NULL,
    target_field VARCHAR(80) NOT NULL,
    scale_factor NUMERIC(18,6) NOT NULL DEFAULT 1,
    offset_value NUMERIC(18,6) NOT NULL DEFAULT 0,
    unit VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_d200_field_mapping UNIQUE (gateway_id, device_id, source_key, target_field)
);

CREATE INDEX IF NOT EXISTS idx_d200_field_mappings_gateway ON d200_field_mappings(gateway_id);
CREATE INDEX IF NOT EXISTS idx_d200_field_mappings_device ON d200_field_mappings(device_id);

CREATE TABLE IF NOT EXISTS d200_config_tasks (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL,
    gateway_sn VARCHAR(96) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    config_payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    sent_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_d200_config_task_status CHECK (status IN ('pending', 'sent', 'success', 'failed', 'timeout'))
);

CREATE INDEX IF NOT EXISTS idx_d200_config_tasks_gateway_sn ON d200_config_tasks(gateway_sn);
CREATE INDEX IF NOT EXISTS idx_d200_config_tasks_status ON d200_config_tasks(status);
