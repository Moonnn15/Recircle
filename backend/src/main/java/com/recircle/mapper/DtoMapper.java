package com.recircle.mapper;

import com.recircle.dto.ApiDtos.*;
import com.recircle.entity.*;
import com.recircle.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DtoMapper {

    private final UserProfileRepository userProfileRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final ItemImageRepository itemImageRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReviewRepository reviewRepository;

    public DtoMapper(
        UserProfileRepository userProfileRepository,
        TrustScoreRepository trustScoreRepository,
        ItemImageRepository itemImageRepository,
        TransactionRecordRepository transactionRecordRepository,
        ReviewRepository reviewRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.trustScoreRepository = trustScoreRepository;
        this.itemImageRepository = itemImageRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reviewRepository = reviewRepository;
    }

    public UserSummaryDto toUserSummary(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        TrustScore ts = trustScoreRepository.findByUserId(user.getId()).orElse(null);
        return new UserSummaryDto(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRegistrationNumber(),
            user.getRole().name(),
            user.getAccountStatus().name(),
            user.isEmailVerified(),
            user.isCampusVerified(),
            profile != null ? profile.getDepartment() : "General Engineering",
            profile != null ? profile.getYearOfStudy() : 1,
            profile != null ? profile.getContactNumber() : "",
            profile != null ? profile.getHostelOrBlock() : "",
            profile != null ? profile.getBio() : "",
            profile != null ? profile.getAvatarUrl() : "",
            profile != null ? profile.getPreferredPickupSpot() : "",
            profile != null ? profile.getProfileCompletionPct() : 60,
            ts != null ? ts.getTotalScore() : new BigDecimal("50.00"),
            ts != null ? ts.getTrustLevel().name() : "DEVELOPING",
            ts != null ? ts.getAverageRating() : BigDecimal.ZERO,
            ts != null ? ts.getCompletedTransactionsCount() : 0,
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        );
    }

    public CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(
            c.getId(),
            c.getName(),
            c.getSlug(),
            c.getDescription(),
            c.getIconName(),
            c.isActive()
        );
    }

    public ItemDto toItemDto(Item item) {
        User owner = item.getOwner();
        UserProfile ownerProfile = userProfileRepository.findByUserId(owner.getId()).orElse(null);
        TrustScore ownerTs = trustScoreRepository.findByUserId(owner.getId()).orElse(null);
        List<String> gallery = itemImageRepository.findByItemIdOrderByDisplayOrderAsc(item.getId())
            .stream()
            .map(ItemImage::getImageUrl)
            .toList();

        return new ItemDto(
            item.getId(),
            item.getTitle(),
            item.getDescription(),
            item.getCategory().getId(),
            item.getCategory().getName(),
            item.getSubcategory(),
            item.getItemCondition().name(),
            item.getSharingType().name(),
            item.getAvailabilityStatus().name(),
            item.getCampusLocation(),
            item.getLocation(),
            item.getCity(),
            item.getNeighborhood(),
            item.getLandmark(),
            item.getPickupInstructions(),
            item.getSecurityDeposit(),
            item.getRentalPricePerDay(),
            item.getAvailableFrom() != null ? item.getAvailableFrom().toString() : null,
            item.getAvailableUntil() != null ? item.getAvailableUntil().toString() : null,
            item.getMaxBorrowDays(),
            item.getExchangePreferences(),
            item.getUsageRules(),
            item.getPrimaryImageUrl(),
            gallery,
            item.getViewCount(),
            owner.getId(),
            owner.getFullName(),
            ownerProfile != null ? ownerProfile.getDepartment() : "Community Member",
            owner.isCampusVerified(),
            ownerTs != null ? ownerTs.getTotalScore() : new BigDecimal("50.00"),
            ownerTs != null ? ownerTs.getTrustLevel().name() : "DEVELOPING",
            ownerTs != null ? ownerTs.getAverageRating() : BigDecimal.ZERO,
            item.getCreatedAt() != null ? item.getCreatedAt().toString() : ""
        );
    }

    public BorrowRequestDto toBorrowDto(BorrowRequest b) {
        TrustScore borrowerTs = trustScoreRepository.findByUserId(b.getBorrower().getId()).orElse(null);
        Long txId = transactionRecordRepository.findByTransactionTypeAndReferenceId(
            com.recircle.enums.SharingType.BORROW, b.getId()
        ).map(TransactionRecord::getId).orElse(null);

        return new BorrowRequestDto(
            b.getId(),
            b.getItem().getId(),
            b.getItem().getTitle(),
            b.getItem().getPrimaryImageUrl(),
            b.getItem().getCampusLocation(),
            b.getItem().getLocation(),
            b.getBorrower().getId(),
            b.getBorrower().getFullName(),
            borrowerTs != null ? borrowerTs.getTotalScore() : new BigDecimal("50.00"),
            b.getLender().getId(),
            b.getLender().getFullName(),
            b.getStartDate().toString(),
            b.getEndDate().toString(),
            b.getPurpose(),
            b.getStatus().name(),
            b.getOwnerResponseNote(),
            b.getHandoverConfirmedAt() != null ? b.getHandoverConfirmedAt().toString() : null,
            b.getReturnRequestedAt() != null ? b.getReturnRequestedAt().toString() : null,
            b.getReturnedAt() != null ? b.getReturnedAt().toString() : null,
            b.getReturnedOnTime(),
            txId,
            b.getCreatedAt().toString()
        );
    }

    public ExchangeOfferDto toExchangeDto(ExchangeOffer e) {
        TrustScore propTs = trustScoreRepository.findByUserId(e.getProposer().getId()).orElse(null);
        TrustScore recTs = trustScoreRepository.findByUserId(e.getReceiver().getId()).orElse(null);
        Long txId = transactionRecordRepository.findByTransactionTypeAndReferenceId(
            com.recircle.enums.SharingType.EXCHANGE, e.getId()
        ).map(TransactionRecord::getId).orElse(null);

        return new ExchangeOfferDto(
            e.getId(),
            e.getRequestedItem().getId(),
            e.getRequestedItem().getTitle(),
            e.getRequestedItem().getPrimaryImageUrl(),
            e.getOfferedItem().getId(),
            e.getOfferedItem().getTitle(),
            e.getOfferedItem().getPrimaryImageUrl(),
            e.getProposer().getId(),
            e.getProposer().getFullName(),
            propTs != null ? propTs.getTotalScore() : new BigDecimal("50.00"),
            e.getReceiver().getId(),
            e.getReceiver().getFullName(),
            recTs != null ? recTs.getTotalScore() : new BigDecimal("50.00"),
            e.getProposalMessage(),
            e.getStatus().name(),
            e.isProposerHandoverConfirmed(),
            e.isReceiverHandoverConfirmed(),
            e.getCompletedAt() != null ? e.getCompletedAt().toString() : null,
            txId,
            e.getCreatedAt().toString()
        );
    }

    public RentalDto toRentalDto(Rental r) {
        TrustScore renterTs = trustScoreRepository.findByUserId(r.getRenter().getId()).orElse(null);
        Long txId = transactionRecordRepository.findByTransactionTypeAndReferenceId(
            com.recircle.enums.SharingType.RENT, r.getId()
        ).map(TransactionRecord::getId).orElse(null);

        return new RentalDto(
            r.getId(),
            r.getItem().getId(),
            r.getItem().getTitle(),
            r.getItem().getPrimaryImageUrl(),
            r.getRenter().getId(),
            r.getRenter().getFullName(),
            renterTs != null ? renterTs.getTotalScore() : new BigDecimal("50.00"),
            r.getOwner().getId(),
            r.getOwner().getFullName(),
            r.getStartDate().toString(),
            r.getEndDate().toString(),
            r.getRentalDays(),
            r.getDailyRate(),
            r.getSecurityDeposit(),
            r.getTotalRentalAmount(),
            r.getPaymentStatus().name(),
            r.getStatus().name(),
            r.getHandoverConfirmedAt() != null ? r.getHandoverConfirmedAt().toString() : null,
            r.getReturnedAt() != null ? r.getReturnedAt().toString() : null,
            r.getReturnedOnTime(),
            txId,
            r.getCreatedAt().toString()
        );
    }

    public DonationRequestDto toDonationDto(DonationRequest d) {
        UserProfile reqProfile = userProfileRepository.findByUserId(d.getRequester().getId()).orElse(null);
        TrustScore reqTs = trustScoreRepository.findByUserId(d.getRequester().getId()).orElse(null);
        Long txId = transactionRecordRepository.findByTransactionTypeAndReferenceId(
            com.recircle.enums.SharingType.DONATE, d.getId()
        ).map(TransactionRecord::getId).orElse(null);

        return new DonationRequestDto(
            d.getId(),
            d.getItem().getId(),
            d.getItem().getTitle(),
            d.getItem().getPrimaryImageUrl(),
            d.getRequester().getId(),
            d.getRequester().getFullName(),
            reqProfile != null ? reqProfile.getDepartment() : "SRM Campus",
            reqTs != null ? reqTs.getTotalScore() : new BigDecimal("50.00"),
            d.getDonor().getId(),
            d.getDonor().getFullName(),
            d.getMotivationMessage(),
            d.getStatus().name(),
            d.getHandoverConfirmedAt() != null ? d.getHandoverConfirmedAt().toString() : null,
            d.getCompletedAt() != null ? d.getCompletedAt().toString() : null,
            txId,
            d.getCreatedAt().toString()
        );
    }

    public TransactionDto toTransactionDto(TransactionRecord t, Long currentUserId) {
        boolean reviewed = currentUserId != null &&
            reviewRepository.existsByTransactionIdAndReviewerId(t.getId(), currentUserId);

        return new TransactionDto(
            t.getId(),
            t.getTransactionCode(),
            t.getTransactionType().name(),
            t.getReferenceId(),
            t.getPrimaryItem().getId(),
            t.getPrimaryItem().getTitle(),
            t.getPrimaryItem().getPrimaryImageUrl(),
            t.getSecondaryItem() != null ? t.getSecondaryItem().getId() : null,
            t.getSecondaryItem() != null ? t.getSecondaryItem().getTitle() : null,
            t.getInitiatorUser().getId(),
            t.getInitiatorUser().getFullName(),
            t.getCounterpartyUser().getId(),
            t.getCounterpartyUser().getFullName(),
            t.getStatus(),
            t.getMonetaryAmount(),
            t.getPaymentStatus().name(),
            t.getStartDate() != null ? t.getStartDate().toString() : null,
            t.getDueDate() != null ? t.getDueDate().toString() : null,
            t.getCompletedAt() != null ? t.getCompletedAt().toString() : null,
            t.getReturnedOnTime(),
            t.getNotes(),
            reviewed,
            t.getCreatedAt().toString()
        );
    }

    public ReviewDto toReviewDto(Review r) {
        return new ReviewDto(
            r.getId(),
            r.getTransaction().getId(),
            r.getTransaction().getTransactionCode(),
            r.getItem().getId(),
            r.getItem().getTitle(),
            r.getReviewer().getId(),
            r.getReviewer().getFullName(),
            r.getReviewee().getId(),
            r.getReviewee().getFullName(),
            r.getRating(),
            r.getComment(),
            r.isFlagged(),
            r.getCreatedAt().toString()
        );
    }

    public NotificationDto toNotificationDto(Notification n) {
        return new NotificationDto(
            n.getId(),
            n.getNotificationType(),
            n.getTitle(),
            n.getMessage(),
            n.getTargetPath(),
            n.getRelatedEntityId(),
            n.isRead(),
            n.getCreatedAt().toString()
        );
    }
}
