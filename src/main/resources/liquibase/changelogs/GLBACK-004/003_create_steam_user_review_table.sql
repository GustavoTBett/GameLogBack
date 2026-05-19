-- Liquibase SQL to create steam_user_review table
CREATE TABLE IF NOT EXISTS steam_user_review (
    id BIGSERIAL PRIMARY KEY,
    version INTEGER,
    steam_account_id BIGINT NOT NULL,
    app_id BIGINT NOT NULL,
    game_id BIGINT NULL,
    review_text TEXT NULL,
    language VARCHAR(32) NULL,
    recommended BOOLEAN NULL,
    reviewed_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    imported_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_sur_steam_account FOREIGN KEY (steam_account_id) REFERENCES steam_account(id),
    CONSTRAINT fk_sur_game FOREIGN KEY (game_id) REFERENCES game(id)
);

CREATE INDEX idx_sur_appid ON steam_user_review(app_id);
CREATE INDEX idx_sur_account ON steam_user_review(steam_account_id);
