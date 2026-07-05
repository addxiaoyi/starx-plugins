ALTER TABLE starx_users ADD COLUMN IF NOT EXISTS source_system VARCHAR(50);
ALTER TABLE starx_users ADD COLUMN IF NOT EXISTS migration_state VARCHAR(20);
ALTER TABLE starx_users ADD COLUMN IF NOT EXISTS password_migrated_at TIMESTAMP;
