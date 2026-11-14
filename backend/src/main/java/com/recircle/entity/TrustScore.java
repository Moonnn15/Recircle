package com.recircle.entity;

import com.recircle.enums.TrustLevel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trust_scores")
public class TrustScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore = new BigDecimal("50.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 25)
    private TrustLevel trustLevel = TrustLevel.DEVELOPING;

    @Column(name = "verified_account_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal verifiedAccountScore = BigDecimal.ZERO;

    @Column(name = "completed_tx_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal completedTxScore = BigDecimal.ZERO;

    @Column(name = "user_review_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal userReviewScore = new BigDecimal("50.00");

    @Column(name = "on_time_return_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeReturnScore = new BigDecimal("50.00");

    @Column(name = "completed_transactions_count", nullable = false)
    private Integer completedTransactionsCount = 0;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "on_time_return_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeReturnPercentage = new BigDecimal("50.00");

    @Column(name = "eligible_return_count", nullable = false)
    private Integer eligibleReturnCount = 0;

    @Column(name = "on_time_return_count", nullable = false)
    private Integer onTimeReturnCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }
    public BigDecimal getVerifiedAccountScore() { return verifiedAccountScore; }
    public void setVerifiedAccountScore(BigDecimal verifiedAccountScore) { this.verifiedAccountScore = verifiedAccountScore; }
    public BigDecimal getCompletedTxScore() { return completedTxScore; }
    public void setCompletedTxScore(BigDecimal completedTxScore) { this.completedTxScore = completedTxScore; }
    public BigDecimal getUserReviewScore() { return userReviewScore; }
    public void setUserReviewScore(BigDecimal userReviewScore) { this.userReviewScore = userReviewScore; }
    public BigDecimal getOnTimeReturnScore() { return onTimeReturnScore; }
    public void setOnTimeReturnScore(BigDecimal onTimeReturnScore) { this.onTimeReturnScore = onTimeReturnScore; }
    public Integer getCompletedTransactionsCount() { return completedTransactionsCount; }
    public void setCompletedTransactionsCount(Integer completedTransactionsCount) { this.completedTransactionsCount = completedTransactionsCount; }
    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }
    public BigDecimal getOnTimeReturnPercentage() { return onTimeReturnPercentage; }
    public void setOnTimeReturnPercentage(BigDecimal onTimeReturnPercentage) { this.onTimeReturnPercentage = onTimeReturnPercentage; }
    public Integer getEligibleReturnCount() { return eligibleReturnCount; }
    public void setEligibleReturnCount(Integer eligibleReturnCount) { this.eligibleReturnCount = eligibleReturnCount; }
    public Integer getOnTimeReturnCount() { return onTimeReturnCount; }
    public void setOnTimeReturnCount(Integer onTimeReturnCount) { this.onTimeReturnCount = onTimeReturnCount; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
