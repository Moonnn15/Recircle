package com.recircle.repository;

import com.recircle.entity.Dispute;
import com.recircle.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    @Query("SELECT d FROM Dispute d WHERE d.reporter.id = :userId OR d.reportedUser.id = :userId ORDER BY d.createdAt DESC")
    List<Dispute> findByParticipant(@Param("userId") Long userId);

    List<Dispute> findAllByOrderByCreatedAtDesc();

    long countByStatusIn(List<DisputeStatus> statuses);
}
