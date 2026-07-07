ALTER TABLE starx_users ADD COLUMN source_system VARCHAR(50);
ALTER TABLE starx_users ADD COLUMN migration_state VARCHAR(20);
ALTER TABLE starx_users ADD COLUMN password_migrated_at TIMESTAMP;
