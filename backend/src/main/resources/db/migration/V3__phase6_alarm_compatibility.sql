-- Phase 6 compatibility for alarm records in databases created by earlier drafts.

ALTER TABLE alarms ADD COLUMN IF NOT EXISTS gateway_id VARCHAR(64);

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT tc.constraint_name
      INTO constraint_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
       AND tc.table_schema = kcu.table_schema
     WHERE tc.table_schema = 'public'
       AND tc.table_name = 'alarms'
       AND tc.constraint_type = 'FOREIGN KEY'
       AND kcu.column_name = 'telemetry_id'
     LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE alarms DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_alarms_gateway_time ON alarms(gateway_id, occurred_at DESC);
