package com.recircle.repository;

import com.recircle.entity.TrustScore;
import com.recircle.entity.TrustScoreHistory;
import com.recircle.entity.Notification;
import com.recircle.entity.Dispute;
import com.recircle.entity.DisputeEvidence;
import com.recircle.entity.Report;
import com.recircle.entity.AuditLog;
import com.recircle.entity.RefreshToken;
import com.recircle.entity.EmailVerificationToken;
import com.recircle.entity.PasswordResetToken;
import com.recircle.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrustScoreRepository extends JpaRepository<TrustScore, Long> {
    Optional<TrustScore> findByUserId(Long userId);
}
