package com.recircle.scheduler;

import com.recircle.entity.BorrowRequest;
import com.recircle.enums.BorrowStatus;
import com.recircle.repository.BorrowRequestRepository;
import com.recircle.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Scheduled task checking active borrowings for upcoming due dates or overdue items.
 */
@Component
public class ReturnReminderScheduler {

    private final BorrowRequestRepository borrowRequestRepository;
    private final NotificationService notificationService;

    public ReturnReminderScheduler(
        BorrowRequestRepository borrowRequestRepository,
        NotificationService notificationService
    ) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 3600000) // Runs hourly
    @Transactional
    public void processReturnReminders() {
        LocalDate today = LocalDate.now();
        List<BorrowRequest> activeList = borrowRequestRepository.findByStatusIn(List.of(BorrowStatus.ACTIVE));
        for (BorrowRequest br : activeList) {
            if (br.getEndDate().isBefore(today)) {
                br.setStatus(BorrowStatus.OVERDUE);
                borrowRequestRepository.save(br);
                notificationService.createNotification(
                    br.getBorrower(),
                    "RETURN_OVERDUE",
                    "Overdue Return Alert: " + br.getItem().getTitle(),
                    "Your borrowing period for '" + br.getItem().getTitle() + "' ended on " + br.getEndDate() + ". Please return it immediately to protect your Trust Score.",
                    "/borrowing",
                    br.getId()
                );
            }
        }
    }
}
