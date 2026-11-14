package com.recircle.repository;

import com.recircle.entity.Rental;
import com.recircle.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @Query("SELECT r FROM Rental r WHERE r.renter.id = :userId OR r.owner.id = :userId ORDER BY r.createdAt DESC")
    List<Rental> findByParticipant(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Rental r
        WHERE r.item.id = :itemId
          AND r.status IN :activeStatuses
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsOverlappingRental(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("activeStatuses") List<RentalStatus> activeStatuses,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT r FROM Rental r
        WHERE r.item.id = :itemId
          AND r.status IN :activeStatuses
        ORDER BY r.startDate ASC
    """)
    List<Rental> findActiveRentalsForItem(
        @Param("itemId") Long itemId,
        @Param("activeStatuses") List<RentalStatus> activeStatuses
    );

    @Query("""
        SELECT r FROM Rental r
        WHERE r.item.id = :itemId
          AND r.status = com.recircle.enums.RentalStatus.PENDING
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    List<Rental> findOverlappingPendingRentals(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId
    );
}
