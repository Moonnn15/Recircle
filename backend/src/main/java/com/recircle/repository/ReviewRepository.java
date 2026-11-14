package com.recircle.repository;

import com.recircle.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
    List<Review> findByItemIdOrderByCreatedAtDesc(Long itemId);
    boolean existsByTransactionIdAndReviewerId(Long transactionId, Long reviewerId);
}
