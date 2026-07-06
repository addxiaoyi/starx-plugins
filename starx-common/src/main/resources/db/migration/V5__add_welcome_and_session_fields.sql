ALTER TABLE starx_users ADD COLUMN last_login_ip VARCHAR(255);
ALTER TABLE starx_users ADD COLUMN last_login_isp VARCHAR(255);
ALTER TABLE starx_users ADD COLUMN last_login_location VARCHAR(255);
ALTER TABLE starx_users ADD COLUMN total_playtime BIGINT DEFAULT 0;
ALTER TABLE starx_users ADD COLUMN last_logout_at TIMESTAMP;
ALTER TABLE starx_users ADD COLUMN welcome_message_shown BOOLEAN DEFAULT FALSE;
