package com.recircle.repository;

import com.recircle.entity.Item;
import com.recircle.enums.ListingStatus;
import com.recircle.enums.SharingType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    List<Item> findByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(Long ownerId);
    List<Item> findByArchivedFalseOrderByCreatedAtDesc();
    List<Item> findByCategoryIdAndArchivedFalseAndIdNotOrderByCreatedAtDesc(Long categoryId, Long excludeId);
    long countByOwnerIdAndArchivedFalse(Long ownerId);
    long countByArchivedFalse();
    long countByAvailabilityStatusAndArchivedFalse(ListingStatus status);
    long countBySharingTypeAndArchivedFalse(SharingType sharingType);
}
