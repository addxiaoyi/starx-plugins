CREATE TABLE IF NOT EXISTS starx_users (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    totp_secret VARCHAR(255),
    premium BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    external_user_id VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_starx_users_username ON starx_users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_starx_users_email ON starx_users(email);
