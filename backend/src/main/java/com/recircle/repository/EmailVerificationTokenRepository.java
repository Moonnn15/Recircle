package com.recircle.repository;

import com.recircle.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(Long userId);
    Optional<EmailVerificationToken> findByVerificationTokenAndConsumedFalse(String token);
}
