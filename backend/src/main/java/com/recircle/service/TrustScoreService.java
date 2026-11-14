package com.recircle.service;

import com.recircle.dto.ApiDtos.TrustHistoryDto;
import com.recircle.dto.ApiDtos.TrustScoreDetailDto;
import com.recircle.entity.*;
import com.recircle.enums.DisputeStatus;
import com.recircle.enums.TrustLevel;
import com.recircle.exception.ApiException;
import com.recircle.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core ReCircle Differentiator: Server-Side Weighted Trust & Reputation Engine.
 *
 * Formula:
 *   Trust Score = (Verified Account Score * 0.15)
 *               + (Completed Transaction Score * 0.35)
 *               + (User Review Score * 0.30)
 *               + (On-Time Return Score * 0.20)
 *
 * Safeguards:
 * - Bounded asymptotic transaction curve prevents artificial score inflation via repeated micro-transactions.
 * - Neutral 50.00 default for On-Time Return Score when no eligible borrowing/rental returns exist.
 * - Automatic history logging on every recalculation event.
 */
@Service
public class TrustScoreService {

    private final UserRepository userRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final TrustScoreHistoryRepository trustScoreHistoryRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReviewRepository reviewRepository;
    private final DisputeRepository disputeRepository;

    public TrustScoreService(
        UserRepository userRepository,
        TrustScoreRepository trustScoreRepository,
        TrustScoreHistoryRepository trustScoreHistoryRepository,
        TransactionRecordRepository transactionRecordRepository,
        ReviewRepository reviewRepository,
        DisputeRepository disputeRepository
    ) {
        this.userRepository = userRepository;
        this.trustScoreRepository = trustScoreRepository;
        this.trustScoreHistoryRepository = trustScoreHistoryRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reviewRepository = reviewRepository;
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public TrustScore initializeTrustScore(User user) {
        TrustScore existing = trustScoreRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        TrustScore ts = new TrustScore();
        ts.setUser(user);
        double verifiedComponent = user.isCampusVerified() ? 100.0 : 0.0;
        double completedComponent = 0.0;
        double reviewComponent = 50.0; // Neutral baseline before first review
        double onTimeComponent = 50.0; // Required neutral default of 50 when 0 eligible returns

        double total = (verifiedComponent * 0.15)
            + (completedComponent * 0.35)
            + (reviewComponent * 0.30)
            + (onTimeComponent * 0.20);

        ts.setVerifiedAccountScore(bd(verifiedComponent));
        ts.setCompletedTxScore(bd(completedComponent));
        ts.setUserReviewScore(bd(reviewComponent));
        ts.setOnTimeReturnScore(bd(onTimeComponent));
        ts.setOnTimeReturnPercentage(bd(50.0));
        ts.setTotalScore(bd(total));
        ts.setTrustLevel(resolveTrustLevel(total));

        TrustScore saved = trustScoreRepository.save(ts);
        recordHistory(user, BigDecimal.ZERO, saved.getTotalScore(), "ACCOUNT_CREATED", "Initial campus trust score initialized.");
        return saved;
    }

    @Transactional
    public TrustScore recalculateTrustScore(Long userId, String triggerEvent, String reasonSummary) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        TrustScore ts = trustScoreRepository.findByUserId(userId)
            .orElseGet(() -> initializeTrustScore(user));

        BigDecimal previousTotal = ts.getTotalScore();

        // Component 1: Verified Account Score (0 or 100)
        double verifiedScore = user.isCampusVerified() ? 100.0 : 0.0;

        // Component 2: Completed Transaction Score (Bounded anti-gaming asymptotic formula)
        // Deduplicate same-counterparty same-day transactions to prevent artificial score farming
        List<TransactionRecord> allUserTx = transactionRecordRepository.findByParticipant(userId);
        List<TransactionRecord> completedTx = allUserTx.stream()
            .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
            .toList();

        Set<String> uniquePairDayKeys = new HashSet<>();
        for (TransactionRecord tx : completedTx) {
            long otherId = tx.getInitiatorUser().getId().equals(userId)
                ? tx.getCounterpartyUser().getId()
                : tx.getInitiatorUser().getId();
            String day = tx.getCompletedAt() != null
                ? tx.getCompletedAt().toString().substring(0, 10)
                : "default";
            uniquePairDayKeys.add(otherId + "_" + day + "_" + tx.getTransactionType().name());
        }
        int effectiveCompletedCount = uniquePairDayKeys.size();
        // Bounded formula: 100 * (1 - exp(-0.16 * effectiveCount)), capped at 100.0
        double completedTxScore = Math.min(100.0, 100.0 * (1.0 - Math.exp(-0.16 * effectiveCompletedCount)));

        // Component 3: User Review Score (valid non-flagged reviews converted from 1-5 to 0-100)
        List<Review> validReviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId)
            .stream()
            .filter(r -> !r.isFlagged())
            .toList();

        double avgRating = 0.0;
        double userReviewScore = 50.0; // Neutral if no reviews yet
        if (!validReviews.isEmpty()) {
            avgRating = validReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            userReviewScore = Math.min(100.0, Math.max(0.0, (avgRating / 5.0) * 100.0));
        }

        // Component 4: On-Time Return Score (percentage of eligible borrow/rental returns on time; 50 neutral if 0)
        List<TransactionRecord> eligibleReturns = transactionRecordRepository.findEligibleReturnTransactionsByUser(userId);
        int eligibleCount = eligibleReturns.size();
        int onTimeCount = (int) eligibleReturns.stream()
            .filter(t -> Boolean.TRUE.equals(t.getReturnedOnTime()))
            .count();

        double onTimeReturnScore = 50.0; // Required neutral default when no eligible returns
        if (eligibleCount > 0) {
            onTimeReturnScore = ((double) onTimeCount / (double) eligibleCount) * 100.0;
        }

        // Weighted Sum: (Verified * 0.15) + (Completed * 0.35) + (Reviews * 0.30) + (OnTime * 0.20) - DisputePenalty
        long penalizedDisputeCount = disputeRepository.countByReportedUserIdAndStatusAndPenaltyAppliedTrue(
            userId, DisputeStatus.RESOLVED
        );
        double disputePenalty = 15.0 * penalizedDisputeCount;

        double rawTotal = (verifiedScore * 0.15)
            + (completedTxScore * 0.35)
            + (userReviewScore * 0.30)
            + (onTimeReturnScore * 0.20)
            - disputePenalty;
        double boundedTotal = Math.min(100.0, Math.max(0.0, rawTotal));

        ts.setVerifiedAccountScore(bd(verifiedScore));
        ts.setCompletedTxScore(bd(completedTxScore));
        ts.setUserReviewScore(bd(userReviewScore));
        ts.setOnTimeReturnScore(bd(onTimeReturnScore));
        ts.setCompletedTransactionsCount(completedTx.size());
        ts.setAverageRating(bd(avgRating));
        ts.setOnTimeReturnPercentage(bd(onTimeReturnScore));
        ts.setEligibleReturnCount(eligibleCount);
        ts.setOnTimeReturnCount(onTimeCount);
        ts.setTotalScore(bd(boundedTotal));
        ts.setTrustLevel(resolveTrustLevel(boundedTotal));

        TrustScore saved = trustScoreRepository.save(ts);
        recordHistory(user, previousTotal, saved.getTotalScore(), triggerEvent, reasonSummary);
        return saved;
    }

    @Transactional(readOnly = true)
    public TrustScoreDetailDto getTrustScoreDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        TrustScore ts = trustScoreRepository.findByUserId(userId)
            .orElseGet(() -> initializeTrustScore(user));

        List<TrustHistoryDto> history = trustScoreHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(h -> new TrustHistoryDto(
                h.getId(),
                h.getPreviousScore(),
                h.getNewScore(),
                h.getDelta(),
                h.getTriggerEvent(),
                h.getReasonSummary(),
                h.getCreatedAt().toString()
            ))
            .toList();

        return new TrustScoreDetailDto(
            user.getId(),
            user.getFullName(),
            user.isCampusVerified(),
            ts.getTotalScore(),
            ts.getTrustLevel().name(),
            ts.getVerifiedAccountScore(),
            ts.getCompletedTxScore(),
            ts.getUserReviewScore(),
            ts.getOnTimeReturnScore(),
            ts.getCompletedTransactionsCount(),
            ts.getAverageRating(),
            ts.getOnTimeReturnPercentage(),
            ts.getEligibleReturnCount(),
            ts.getOnTimeReturnCount(),
            history
        );
    }

    public TrustLevel resolveTrustLevel(double score) {
        if (score >= 90.0) return TrustLevel.EXCELLENT;
        if (score >= 75.0) return TrustLevel.TRUSTED;
        if (score >= 50.0) return TrustLevel.DEVELOPING;
        return TrustLevel.LOW_TRUST;
    }

    private void recordHistory(User user, BigDecimal previous, BigDecimal next, String event, String summary) {
        TrustScoreHistory h = new TrustScoreHistory();
        h.setUser(user);
        h.setPreviousScore(previous);
        h.setNewScore(next);
        h.setDelta(next.subtract(previous).setScale(2, RoundingMode.HALF_UP));
        h.setTriggerEvent(event);
        h.setReasonSummary(summary);
        trustScoreHistoryRepository.save(h);
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }
}
