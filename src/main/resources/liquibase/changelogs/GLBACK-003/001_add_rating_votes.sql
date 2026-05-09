CREATE TABLE rating_vote (
    id BIGSERIAL PRIMARY KEY,

    rating_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    vote_type VARCHAR(20) NOT NULL,

    version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_rating_vote_rating
        FOREIGN KEY (rating_id)
            REFERENCES rating(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_rating_vote_user
        FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE,

    CONSTRAINT rating_vote_unique
        UNIQUE (rating_id, user_id),

    CONSTRAINT rating_vote_type_check
        CHECK (vote_type IN ('UPVOTE', 'DOWNVOTE'))
);

CREATE INDEX idx_rating_vote_rating ON rating_vote(rating_id);
CREATE INDEX idx_rating_vote_user ON rating_vote(user_id);