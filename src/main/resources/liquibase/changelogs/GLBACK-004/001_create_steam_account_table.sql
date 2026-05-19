-- Liquibase SQL to create steam_account table
CREATE TABLE IF NOT EXISTS steam_account (
    id BIGSERIAL PRIMARY KEY,
    version INTEGER,
    user_id BIGINT NOT NULL,
    steam_id VARCHAR(32) NOT NULL,
    profile_url VARCHAR(255),
    last_synced_at TIMESTAMP NULL,
    synced BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_steam_account_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE UNIQUE INDEX ux_steam_account_user ON steam_account(user_id);
CREATE UNIQUE INDEX ux_steam_account_steam_id ON steam_account(steam_id);
