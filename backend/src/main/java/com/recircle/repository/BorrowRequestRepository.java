package com.recircle.repository;

import com.recircle.entity.BorrowRequest;
import com.recircle.enums.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    List<BorrowRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    List<BorrowRequest> findByLenderIdOrderByCreatedAtDesc(Long lenderId);

    List<BorrowRequest> findByStatusIn(List<BorrowStatus> statuses);

    @Query("""
        SELECT COUNT(b) > 0 FROM BorrowRequest b
        WHERE b.item.id = :itemId
          AND b.status IN :activeStatuses
          AND b.startDate <= :endDate
          AND b.endDate >= :startDate
          AND (:excludeId IS NULL OR b.id <> :excludeId)
    """)
    boolean existsOverlappingReservation(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("activeStatuses") List<BorrowStatus> activeStatuses,
        @Param("excludeId") Long excludeId
    );
}
