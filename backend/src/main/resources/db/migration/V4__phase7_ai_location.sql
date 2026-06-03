-- Phase 7: AI open API bootstrap and gateway/device location fields.

ALTER TABLE gateways ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS province VARCHAR(80);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS city VARCHAR(80);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS district VARCHAR(80);
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMPTZ;

ALTER TABLE devices ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS address VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_gateways_location ON gateways(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_devices_location ON devices(latitude, longitude);

INSERT INTO ai_api_keys (
    key_name, key_prefix, key_hash, owner_name, owner_contact, permissions, enabled
)
SELECT
    '默认AI开放接口密钥',
    'fanai',
    crypt('fanai_dev_key_please_change', gen_salt('bf')),
    '系统内置',
    'local',
    '["realtime","history","export"]'::jsonb,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM ai_api_keys WHERE key_prefix = 'fanai'
);
