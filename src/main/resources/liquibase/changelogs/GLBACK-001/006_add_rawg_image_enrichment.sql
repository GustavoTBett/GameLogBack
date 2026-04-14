ALTER TABLE game
    ADD COLUMN IF NOT EXISTS rawg_image_url TEXT,
    ADD COLUMN IF NOT EXISTS image_source VARCHAR(20) NOT NULL DEFAULT 'VGCHARTZ',
    ADD COLUMN IF NOT EXISTS image_last_checked_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_game_image_last_checked_at ON game(image_last_checked_at);
