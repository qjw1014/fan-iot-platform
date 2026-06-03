-- Phase 4 compatibility for databases created by earlier drafts.
-- Current cloud model uses device_model/install_location.
-- Older local volumes may still contain legacy devices.model as NOT NULL.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'model'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE devices ALTER COLUMN model DROP NOT NULL;
    END IF;
END $$;
