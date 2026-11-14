package com.recircle.repository;

import com.recircle.entity.TransactionRecord;
import com.recircle.enums.SharingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    @Query("SELECT t FROM TransactionRecord t WHERE t.initiatorUser.id = :userId OR t.counterpartyUser.id = :userId ORDER BY t.createdAt DESC")
    List<TransactionRecord> findByParticipant(@Param("userId") Long userId);

    Optional<TransactionRecord> findByTransactionTypeAndReferenceId(SharingType type, Long referenceId);

    @Query("""
        SELECT COUNT(t) FROM TransactionRecord t
        WHERE (t.initiatorUser.id = :userId OR t.counterpartyUser.id = :userId)
          AND t.status = 'COMPLETED'
    """)
    long countCompletedByUser(@Param("userId") Long userId);

    @Query("""
        SELECT t FROM TransactionRecord t
        WHERE (t.initiatorUser.id = :userId OR t.counterpartyUser.id = :userId)
          AND t.status = 'COMPLETED'
          AND t.transactionType IN ('BORROW', 'RENT')
          AND t.returnedOnTime IS NOT NULL
    """)
    List<TransactionRecord> findEligibleReturnTransactionsByUser(@Param("userId") Long userId);
}
