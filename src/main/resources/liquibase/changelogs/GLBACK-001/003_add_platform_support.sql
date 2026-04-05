-- Create user_platform join table
CREATE TABLE user_platform (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,

    version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_platform_user
        FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE,

    CONSTRAINT user_platform_unique
        UNIQUE (user_id, platform),

    CONSTRAINT user_platform_check
        CHECK (platform IN ('PC', 'PLAYSTATION', 'XBOX', 'NINTENDO', 'MOBILE', 'CLOUD', 'VR', 'ARCADE'))
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_user_platform_user_id ON user_platform(user_id);

-- Create game_platform join table
CREATE TABLE game_platform (
    id BIGSERIAL PRIMARY KEY,

    game_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,

    version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_game_platform_game
        FOREIGN KEY (game_id)
            REFERENCES game(id)
            ON DELETE CASCADE,

    CONSTRAINT game_platform_unique
        UNIQUE (game_id, platform),

    CONSTRAINT game_platform_check
        CHECK (platform IN ('PC', 'PLAYSTATION', 'XBOX', 'NINTENDO', 'MOBILE', 'CLOUD', 'VR', 'ARCADE'))
);

-- Create index on game_id for faster lookups
CREATE INDEX idx_game_platform_game_id ON game_platform(game_id);

-- Migrate existing game platform data to game_platform table
-- Only if the platform column contains valid values
INSERT INTO game_platform (game_id, platform, version, created_at, updated_at)
SELECT 
    id,
    UPPER(TRIM(
        CASE 
            WHEN platform IS NULL OR platform = '' THEN NULL
            WHEN platform ILIKE '%playstation%' OR platform ILIKE '%ps%' THEN 'PLAYSTATION'
            WHEN platform ILIKE '%xbox%' THEN 'XBOX'
            WHEN platform ILIKE '%nintendo%' OR platform ILIKE '%switch%' THEN 'NINTENDO'
            WHEN platform ILIKE '%mobile%' OR platform ILIKE '%android%' OR platform ILIKE '%ios%' THEN 'MOBILE'
            WHEN platform ILIKE '%pc%' OR platform ILIKE '%windows%' THEN 'PC'
            WHEN platform ILIKE '%cloud%' THEN 'CLOUD'
            WHEN platform ILIKE '%vr%' THEN 'VR'
            WHEN platform ILIKE '%arcade%' THEN 'ARCADE'
            ELSE NULL
        END
    )),
    0,
    now(),
    NULL
FROM game
WHERE platform IS NOT NULL 
  AND platform != ''
  AND (
    platform ILIKE '%playstation%' OR platform ILIKE '%ps%'
    OR platform ILIKE '%xbox%'
    OR platform ILIKE '%nintendo%' OR platform ILIKE '%switch%'
    OR platform ILIKE '%mobile%' OR platform ILIKE '%android%' OR platform ILIKE '%ios%'
    OR platform ILIKE '%pc%' OR platform ILIKE '%windows%'
    OR platform ILIKE '%cloud%'
    OR platform ILIKE '%vr%'
    OR platform ILIKE '%arcade%'
  );

-- Drop the old platform column from game table
ALTER TABLE game DROP COLUMN platform;
