package com.gamelog.gamelog.model;

import com.gamelog.gamelog.model.enums.RatingVoteType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Entity
@Table(name = "rating_vote")
@NoArgsConstructor
@AllArgsConstructor
public class RatingVote extends MasterEntityWAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    @NotNull(message = "Rating is required")
    private Rating rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    @NotNull(message = "Vote type is required")
    private RatingVoteType voteType;
}