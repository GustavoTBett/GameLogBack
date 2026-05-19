-- Liquibase SQL to create steam_game_review table
CREATE TABLE IF NOT EXISTS steam_game_review (
    id BIGSERIAL PRIMARY KEY,
    version INTEGER,
    steam_account_id BIGINT NOT NULL,
    app_id BIGINT NOT NULL,
    game_id BIGINT NULL,
    positive_percent DOUBLE PRECISION,
    total_reviews INTEGER,
    last_checked_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_sgr_steam_account FOREIGN KEY (steam_account_id) REFERENCES steam_account(id),
    CONSTRAINT fk_sgr_game FOREIGN KEY (game_id) REFERENCES game(id)
);

CREATE INDEX idx_sgr_appid ON steam_game_review(app_id);
CREATE INDEX idx_sgr_account ON steam_game_review(steam_account_id);
