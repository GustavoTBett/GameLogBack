CREATE TABLE game (
      id BIGSERIAL PRIMARY KEY,

      name TEXT NOT NULL,
      slug TEXT NOT NULL UNIQUE,

      description TEXT,
      release_date DATE,

      developer TEXT,
      publisher TEXT,
      platform TEXT,

      cover_url TEXT,

      average_rating NUMERIC(3,2) DEFAULT 0,
      default_rating NUMERIC(3,2) DEFAULT 0,

      version INTEGER NOT NULL DEFAULT 0,

      created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
      updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE app_user (
      id BIGSERIAL PRIMARY KEY,

      username TEXT NOT NULL UNIQUE,
      email TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL,

      avatar_url TEXT,
      bio TEXT,

      version INTEGER NOT NULL DEFAULT 0,

      created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
      updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE genre (
       id BIGSERIAL PRIMARY KEY,

       name TEXT NOT NULL UNIQUE
);

CREATE TABLE game_genre (
        game_id BIGINT NOT NULL,
        genre_id BIGINT NOT NULL,

        PRIMARY KEY (game_id, genre_id),

        CONSTRAINT fk_game_genre_game
            FOREIGN KEY (game_id)
                REFERENCES game(id)
                ON DELETE CASCADE,

        CONSTRAINT fk_game_genre_genre
            FOREIGN KEY (genre_id)
                REFERENCES genre(id)
);

CREATE TABLE rating (
        id BIGSERIAL PRIMARY KEY,

        user_id BIGINT NOT NULL,
        game_id BIGINT NOT NULL,

        score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),

        review TEXT,

        version INTEGER NOT NULL DEFAULT 0,

        created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
        updated_at TIMESTAMP WITH TIME ZONE,

        CONSTRAINT fk_rating_user
            FOREIGN KEY (user_id)
                REFERENCES app_user(id)
                ON DELETE CASCADE,

        CONSTRAINT fk_rating_game
            FOREIGN KEY (game_id)
                REFERENCES game(id)
                ON DELETE CASCADE,

        CONSTRAINT rating_user_game_unique
            UNIQUE (user_id, game_id)
);

CREATE TABLE favorites (
       id BIGSERIAL PRIMARY KEY,

       user_id BIGINT NOT NULL,
       game_id BIGINT NOT NULL,

       version INTEGER NOT NULL DEFAULT 0,

       created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
       updated_at TIMESTAMP WITH TIME ZONE,

       CONSTRAINT fk_favorites_user
           FOREIGN KEY (user_id)
               REFERENCES app_user(id)
               ON DELETE CASCADE,

       CONSTRAINT fk_favorites_game
           FOREIGN KEY (game_id)
               REFERENCES game(id)
               ON DELETE CASCADE,

       CONSTRAINT favorites_user_game_unique
           UNIQUE (user_id, game_id)
);

CREATE INDEX idx_game_slug ON game(slug);
CREATE INDEX idx_game_name ON game(name);

CREATE INDEX idx_rating_user ON rating(user_id);
CREATE INDEX idx_rating_game ON rating(game_id);

CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_game ON favorites(game_id);

CREATE INDEX idx_game_genre_genre ON game_genre(genre_id);

