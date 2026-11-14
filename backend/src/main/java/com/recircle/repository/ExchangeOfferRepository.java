package com.recircle.repository;

import com.recircle.entity.ExchangeOffer;
import com.recircle.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExchangeOfferRepository extends JpaRepository<ExchangeOffer, Long> {

    @Query("SELECT e FROM ExchangeOffer e WHERE e.proposer.id = :userId OR e.receiver.id = :userId ORDER BY e.createdAt DESC")
    List<ExchangeOffer> findByParticipant(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(e) > 0 FROM ExchangeOffer e
        WHERE (e.requestedItem.id = :itemId OR e.offeredItem.id = :itemId)
          AND e.status IN :activeStatuses
          AND (:excludeId IS NULL OR e.id <> :excludeId)
    """)
    boolean existsActiveExchangeForItem(
        @Param("itemId") Long itemId,
        @Param("activeStatuses") List<ExchangeStatus> activeStatuses,
        @Param("excludeId") Long excludeId
    );
}
