package com.recircle.repository;

import com.recircle.entity.DonationRequest;
import com.recircle.enums.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DonationRequestRepository extends JpaRepository<DonationRequest, Long> {

    @Query("SELECT d FROM DonationRequest d WHERE d.requester.id = :userId OR d.donor.id = :userId ORDER BY d.createdAt DESC")
    List<DonationRequest> findByParticipant(@Param("userId") Long userId);

    List<DonationRequest> findByItemIdOrderByCreatedAtDesc(Long itemId);

    boolean existsByItemIdAndStatusIn(Long itemId, List<DonationStatus> statuses);
}
