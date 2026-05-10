package com.gamelog.gamelog.service.ratingvote;

import com.gamelog.gamelog.model.enums.RatingVoteType;

import java.util.Collection;
import java.util.Map;

public interface RatingVoteService {

    void vote(Long ratingId, Long userId, RatingVoteType voteType);

    void removeVote(Long ratingId, Long userId);

    Map<Long, RatingVoteStats> getVoteStatsByRatingIds(Collection<Long> ratingIds);

    Map<Long, RatingVoteType> getUserVotesByRatingIds(Collection<Long> ratingIds, Long userId);
}