package com.recircle.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strongly-typed Request and Response DTO records so JPA entities are never exposed directly.
 */
public class ApiDtos {

    public record RegisterRequest(
        String fullName,
        String email,
        String password,
        String confirmPassword,
        String registrationNumber,
        String department,
        Integer yearOfStudy,
        String contactNumber,
        String hostelOrBlock,
        String bio,
        String preferredPickupSpot,
        String avatarUrl
    ) {}

    public record LoginRequest(String email, String password) {}

    public record VerifyEmailRequest(String email, String otpCode) {}

    public record ForgotPasswordRequest(String email) {}

    public record ResetPasswordRequest(String resetToken, String newPassword, String confirmPassword) {}

    public record RefreshTokenRequest(String refreshToken) {}

    public record UpdateProfileRequest(
        String fullName,
        String department,
        Integer yearOfStudy,
        String contactNumber,
        String hostelOrBlock,
        String bio,
        String preferredPickupSpot,
        String avatarUrl
    ) {}

    public record UserSummaryDto(
        Long id,
        String fullName,
        String email,
        String registrationNumber,
        String role,
        String accountStatus,
        boolean emailVerified,
        boolean campusVerified,
        String department,
        Integer yearOfStudy,
        String contactNumber,
        String hostelOrBlock,
        String bio,
        String avatarUrl,
        String preferredPickupSpot,
        Integer profileCompletionPct,
        BigDecimal trustScore,
        String trustLevel,
        BigDecimal averageRating,
        Integer completedTransactions,
        String createdAt
    ) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String message,
        String devOtpPreview,
        UserSummaryDto user
    ) {}

    public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        String iconName,
        boolean active
    ) {}

    public record ItemUpsertRequest(
        String title,
        String description,
        Long categoryId,
        String subcategory,
        String itemCondition,
        String sharingType,
        String availabilityStatus,
        String campusLocation,
        BigDecimal securityDeposit,
        BigDecimal rentalPricePerDay,
        String availableFrom,
        String availableUntil,
        Integer maxBorrowDays,
        String exchangePreferences,
        String usageRules,
        String primaryImageUrl,
        List<String> galleryUrls
    ) {}

    public record ItemDto(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        String subcategory,
        String itemCondition,
        String sharingType,
        String availabilityStatus,
        String campusLocation,
        BigDecimal securityDeposit,
        BigDecimal rentalPricePerDay,
        String availableFrom,
        String availableUntil,
        Integer maxBorrowDays,
        String exchangePreferences,
        String usageRules,
        String primaryImageUrl,
        List<String> galleryUrls,
        Integer viewCount,
        Long ownerId,
        String ownerName,
        String ownerDepartment,
        boolean ownerVerified,
        BigDecimal ownerTrustScore,
        String ownerTrustLevel,
        BigDecimal ownerAverageRating,
        String createdAt
    ) {}

    public record BorrowCreateRequest(
        Long itemId,
        String startDate,
        String endDate,
        String purpose
    ) {}

    public record DecisionNoteRequest(String note) {}

    public record BorrowRequestDto(
        Long id,
        Long itemId,
        String itemTitle,
        String itemImageUrl,
        String campusLocation,
        Long borrowerId,
        String borrowerName,
        BigDecimal borrowerTrustScore,
        Long lenderId,
        String lenderName,
        String startDate,
        String endDate,
        String purpose,
        String status,
        String ownerResponseNote,
        String handoverConfirmedAt,
        String returnRequestedAt,
        String returnedAt,
        Boolean returnedOnTime,
        Long transactionId,
        String createdAt
    ) {}

    public record ExchangeCreateRequest(
        Long requestedItemId,
        Long offeredItemId,
        String proposalMessage
    ) {}

    public record ExchangeOfferDto(
        Long id,
        Long requestedItemId,
        String requestedItemTitle,
        String requestedItemImage,
        Long offeredItemId,
        String offeredItemTitle,
        String offeredItemImage,
        Long proposerId,
        String proposerName,
        BigDecimal proposerTrustScore,
        Long receiverId,
        String receiverName,
        BigDecimal receiverTrustScore,
        String proposalMessage,
        String status,
        boolean proposerHandoverConfirmed,
        boolean receiverHandoverConfirmed,
        String completedAt,
        Long transactionId,
        String createdAt
    ) {}

    public record RentalCreateRequest(
        Long itemId,
        String startDate,
        String endDate,
        boolean simulateImmediatePayment
    ) {}

    public record RentalDto(
        Long id,
        Long itemId,
        String itemTitle,
        String itemImageUrl,
        Long renterId,
        String renterName,
        BigDecimal renterTrustScore,
        Long ownerId,
        String ownerName,
        String startDate,
        String endDate,
        Integer rentalDays,
        BigDecimal dailyRate,
        BigDecimal securityDeposit,
        BigDecimal totalRentalAmount,
        String paymentStatus,
        String status,
        String handoverConfirmedAt,
        String returnedAt,
        Boolean returnedOnTime,
        Long transactionId,
        String createdAt
    ) {}

    public record DonationCreateRequest(
        Long itemId,
        String motivationMessage
    ) {}

    public record DonationRequestDto(
        Long id,
        Long itemId,
        String itemTitle,
        String itemImageUrl,
        Long requesterId,
        String requesterName,
        String requesterDepartment,
        BigDecimal requesterTrustScore,
        Long donorId,
        String donorName,
        String motivationMessage,
        String status,
        String handoverConfirmedAt,
        String completedAt,
        Long transactionId,
        String createdAt
    ) {}

    public record TransactionDto(
        Long id,
        String transactionCode,
        String transactionType,
        Long referenceId,
        Long primaryItemId,
        String primaryItemTitle,
        String primaryItemImage,
        Long secondaryItemId,
        String secondaryItemTitle,
        Long initiatorUserId,
        String initiatorUserName,
        Long counterpartyUserId,
        String counterpartyUserName,
        String status,
        BigDecimal monetaryAmount,
        String paymentStatus,
        String startDate,
        String dueDate,
        String completedAt,
        Boolean returnedOnTime,
        String notes,
        boolean reviewedByCurrentUser,
        String createdAt
    ) {}

    public record ReviewCreateRequest(
        Long transactionId,
        Integer rating,
        String comment
    ) {}

    public record ReviewDto(
        Long id,
        Long transactionId,
        String transactionCode,
        Long itemId,
        String itemTitle,
        Long reviewerId,
        String reviewerName,
        Long revieweeId,
        String revieweeName,
        Integer rating,
        String comment,
        boolean flagged,
        String createdAt
    ) {}

    public record TrustScoreDetailDto(
        Long userId,
        String fullName,
        boolean campusVerified,
        BigDecimal totalScore,
        String trustLevel,
        BigDecimal verifiedAccountScore,
        BigDecimal completedTxScore,
        BigDecimal userReviewScore,
        BigDecimal onTimeReturnScore,
        Integer completedTransactionsCount,
        BigDecimal averageRating,
        BigDecimal onTimeReturnPercentage,
        Integer eligibleReturnCount,
        Integer onTimeReturnCount,
        List<TrustHistoryDto> history
    ) {}

    public record TrustHistoryDto(
        Long id,
        BigDecimal previousScore,
        BigDecimal newScore,
        BigDecimal delta,
        String triggerEvent,
        String reasonSummary,
        String createdAt
    ) {}

    public record NotificationDto(
        Long id,
        String notificationType,
        String title,
        String message,
        String targetPath,
        Long relatedEntityId,
        boolean read,
        String createdAt
    ) {}

    public record DisputeCreateRequest(
        Long transactionId,
        Long itemId,
        Long reportedUserId,
        String category,
        String title,
        String description,
        String evidenceComment,
        String evidenceUrl
    ) {}

    public record DisputeResolveRequest(
        String status,
        String resolutionNotes
    ) {}

    public record DisputeDto(
        Long id,
        Long transactionId,
        String transactionCode,
        Long itemId,
        String itemTitle,
        Long reporterId,
        String reporterName,
        Long reportedUserId,
        String reportedUserName,
        String category,
        String title,
        String description,
        String status,
        String resolutionNotes,
        String resolvedAt,
        String createdAt,
        List<Map<String, Object>> evidenceList
    ) {}

    public record AuditLogDto(
        Long id,
        Long actorUserId,
        String actorName,
        String actorRole,
        String actionType,
        String targetEntity,
        Long targetId,
        String details,
        String createdAt
    ) {}
}
