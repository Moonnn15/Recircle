package com.recircle.repository;

import com.recircle.entity.Item;
import com.recircle.enums.ListingStatus;
import com.recircle.enums.SharingType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(Long ownerId);
    List<Item> findByArchivedFalseOrderByCreatedAtDesc();
    List<Item> findByCategoryIdAndArchivedFalseAndIdNotOrderByCreatedAtDesc(Long categoryId, Long excludeId);
    long countByOwnerIdAndArchivedFalse(Long ownerId);
    long countByArchivedFalse();
    long countByAvailabilityStatusAndArchivedFalse(ListingStatus status);
    long countBySharingTypeAndArchivedFalse(SharingType sharingType);
}
