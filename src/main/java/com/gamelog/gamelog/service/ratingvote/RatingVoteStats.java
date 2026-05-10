package com.gamelog.gamelog.service.ratingvote;

public record RatingVoteStats(long upvoteCount, long downvoteCount) {

    public long netVotes() {
        return upvoteCount - downvoteCount;
    }

    public static RatingVoteStats empty() {
        return new RatingVoteStats(0L, 0L);
    }
}