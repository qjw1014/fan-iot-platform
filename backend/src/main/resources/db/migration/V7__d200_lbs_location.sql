ALTER TABLE gateways
    ADD COLUMN IF NOT EXISTS lbs_mcc INTEGER,
    ADD COLUMN IF NOT EXISTS lbs_mnc INTEGER,
    ADD COLUMN IF NOT EXISTS lbs_lac BIGINT,
    ADD COLUMN IF NOT EXISTS lbs_cid BIGINT,
    ADD COLUMN IF NOT EXISTS lbs_accuracy INTEGER;

CREATE TABLE IF NOT EXISTS d200_lbs_location_logs (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64),
    gateway_sn VARCHAR(96),
    imei VARCHAR(32),
    mcc INTEGER NOT NULL,
    mnc INTEGER NOT NULL,
    lac BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    accuracy INTEGER,
    address VARCHAR(500),
    error_message VARCHAR(500),
    raw_response JSONB,
    located_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_d200_lbs_location_status CHECK (status IN ('success', 'failed', 'skipped'))
);

CREATE INDEX IF NOT EXISTS idx_d200_lbs_logs_gateway ON d200_lbs_location_logs(gateway_id);
CREATE INDEX IF NOT EXISTS idx_d200_lbs_logs_created_at ON d200_lbs_location_logs(created_at DESC);
