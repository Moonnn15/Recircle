package com.recircle.service;

import com.recircle.dto.ApiDtos.*;
import com.recircle.entity.*;
import com.recircle.enums.DisputeStatus;
import com.recircle.exception.ApiException;
import com.recircle.mapper.DtoMapper;
import com.recircle.repository.*;
import com.recircle.validation.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ReviewDisputeService {

    private final ReviewRepository reviewRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository disputeEvidenceRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final TrustScoreService trustScoreService;
    private final NotificationService notificationService;
    private final InputValidator inputValidator;
    private final DtoMapper dtoMapper;

    public ReviewDisputeService(
        ReviewRepository reviewRepository,
        TransactionRecordRepository transactionRecordRepository,
        UserRepository userRepository,
        ItemRepository itemRepository,
        DisputeRepository disputeRepository,
        DisputeEvidenceRepository disputeEvidenceRepository,
        ReportRepository reportRepository,
        AuditLogRepository auditLogRepository,
        TrustScoreService trustScoreService,
        NotificationService notificationService,
        InputValidator inputValidator,
        DtoMapper dtoMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.disputeRepository = disputeRepository;
        this.disputeEvidenceRepository = disputeEvidenceRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
        this.trustScoreService = trustScoreService;
        this.notificationService = notificationService;
        this.inputValidator = inputValidator;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public ReviewDto createReview(Long reviewerId, ReviewCreateRequest req) {
        if (req.transactionId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Transaction ID is required.");
        }
        if (req.rating() == null || req.rating() < 1 || req.rating() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RATING", "Rating must be between 1 and 5 stars.");
        }
        inputValidator.requireNonBlank(req.comment(), "Written feedback comment is required.");

        TransactionRecord tx = transactionRecordRepository.findById(req.transactionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found."));

        if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_NOT_COMPLETED", "Reviews can only be submitted after a transaction is COMPLETED.");
        }

        boolean isInitiator = tx.getInitiatorUser().getId().equals(reviewerId);
        boolean isCounterparty = tx.getCounterpartyUser().getId().equals(reviewerId);
        if (!isInitiator && !isCounterparty) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_A_PARTICIPANT", "Only participants in this completed transaction can submit a review.");
        }

        if (reviewRepository.existsByTransactionIdAndReviewerId(tx.getId(), reviewerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_REVIEW", "You have already submitted a review for this transaction.");
        }

        User reviewer = isInitiator ? tx.getInitiatorUser() : tx.getCounterpartyUser();
        User reviewee = isInitiator ? tx.getCounterpartyUser() : tx.getInitiatorUser();

        if (reviewer.getId().equals(reviewee.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_REVIEW_FORBIDDEN", "Users cannot review themselves.");
        }

        Review r = new Review();
        r.setTransaction(tx);
        r.setItem(tx.getPrimaryItem());
        r.setReviewer(reviewer);
        r.setReviewee(reviewee);
        r.setRating(req.rating());
        r.setComment(inputValidator.sanitizeText(req.comment()));
        r.setFlagged(false);
        Review saved = reviewRepository.save(r);

        trustScoreService.recalculateTrustScore(
            reviewee.getId(),
            "NEW_REVIEW_RECEIVED",
            "Received a " + req.rating() + "-star review from " + reviewer.getFullName() + " (" + tx.getTransactionCode() + ")."
        );

        notificationService.createNotification(
            reviewee,
            "NEW_REVIEW",
            "New " + req.rating() + "-Star Review Received!",
            reviewer.getFullName() + " left you a review on transaction " + tx.getTransactionCode() + ".",
            "/reviews",
            saved.getId()
        );

        return dtoMapper.toReviewDto(saved);
    }

    @Transactional(readOnly = true)
    public ReviewDto getReviewById(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Review not found."));
        return dtoMapper.toReviewDto(r);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getUserReviews(Long userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(dtoMapper::toReviewDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getItemReviews(Long itemId) {
        return reviewRepository.findByItemIdOrderByCreatedAtDesc(itemId)
            .stream()
            .map(dtoMapper::toReviewDto)
            .toList();
    }

    @Transactional
    public DisputeDto createDispute(Long reporterId, DisputeCreateRequest req) {
        inputValidator.requireNonBlank(req.category(), "Dispute category is required.");
        inputValidator.requireNonBlank(req.title(), "Dispute title is required.");
        inputValidator.requireNonBlank(req.description(), "Detailed description is required.");

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reporter not found."));

        TransactionRecord tx = null;
        Item item = null;
        User reportedUser = null;

        if (req.transactionId() != null) {
            tx = transactionRecordRepository.findById(req.transactionId()).orElse(null);
            if (tx != null) {
                item = tx.getPrimaryItem();
                reportedUser = tx.getInitiatorUser().getId().equals(reporterId)
                    ? tx.getCounterpartyUser()
                    : tx.getInitiatorUser();
            }
        }
        if (item == null && req.itemId() != null) {
            item = itemRepository.findById(req.itemId()).orElse(null);
        }
        if (reportedUser == null && req.reportedUserId() != null) {
            reportedUser = userRepository.findById(req.reportedUserId()).orElse(null);
        }
        if (reportedUser == null && item != null) {
            reportedUser = item.getOwner();
        }
        if (reportedUser == null) {
            reportedUser = reporter; // Fallback for general platform issue
        }

        Dispute d = new Dispute();
        d.setTransaction(tx);
        d.setItem(item);
        d.setReporter(reporter);
        d.setReportedUser(reportedUser);
        d.setCategory(inputValidator.sanitizeText(req.category()));
        d.setTitle(inputValidator.sanitizeText(req.title()));
        d.setDescription(inputValidator.sanitizeText(req.description()));
        d.setStatus(DisputeStatus.OPEN);
        Dispute saved = disputeRepository.save(d);

        if (req.evidenceComment() != null && !req.evidenceComment().isBlank()) {
            DisputeEvidence ev = new DisputeEvidence();
            ev.setDispute(saved);
            ev.setSubmittedBy(reporter);
            ev.setEvidenceType(req.evidenceUrl() != null && !req.evidenceUrl().isBlank() ? "IMAGE_OR_LINK" : "COMMENT");
            ev.setEvidenceUrl(req.evidenceUrl());
            ev.setComment(inputValidator.sanitizeText(req.evidenceComment()));
            disputeEvidenceRepository.save(ev);
        }

        return toDisputeDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DisputeDto> getMyDisputes(Long userId) {
        return disputeRepository.findByParticipant(userId)
            .stream()
            .map(this::toDisputeDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DisputeDto> getAllDisputes() {
        return disputeRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toDisputeDto)
            .toList();
    }

    @Transactional
    public DisputeDto resolveDispute(Long disputeId, Long moderatorId, DisputeResolveRequest req) {
        Dispute d = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found."));
        User moderator = userRepository.findById(moderatorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Moderator not found."));

        DisputeStatus newStatus = req.status() != null
            ? DisputeStatus.valueOf(req.status().toUpperCase())
            : DisputeStatus.RESOLVED;

        d.setStatus(newStatus);
        d.setAssignedModerator(moderator);
        d.setResolutionNotes(inputValidator.sanitizeText(req.resolutionNotes()));
        if (newStatus == DisputeStatus.RESOLVED || newStatus == DisputeStatus.REJECTED) {
            d.setResolvedAt(Instant.now());
        }
        Dispute saved = disputeRepository.save(d);

        AuditLog log = new AuditLog();
        log.setActorUser(moderator);
        log.setActorRole(moderator.getRole().name());
        log.setActionType("DISPUTE_STATUS_UPDATED_" + newStatus.name());
        log.setTargetEntity("DISPUTE");
        log.setTargetId(saved.getId());
        log.setDetails("Dispute #" + saved.getId() + " updated to " + newStatus.name() + ": " + d.getResolutionNotes());
        auditLogRepository.save(log);

        notificationService.createNotification(
            d.getReporter(),
            "DISPUTE_UPDATE",
            "Dispute #" + saved.getId() + " Status: " + newStatus.name(),
            "Moderator resolution: " + d.getResolutionNotes(),
            "/disputes",
            saved.getId()
        );

        return toDisputeDto(saved);
    }

    public DisputeDto toDisputeDto(Dispute d) {
        List<Map<String, Object>> evList = disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(d.getId())
            .stream()
            .map(ev -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", ev.getId());
                m.put("submittedBy", ev.getSubmittedBy().getFullName());
                m.put("evidenceType", ev.getEvidenceType());
                m.put("evidenceUrl", ev.getEvidenceUrl());
                m.put("comment", ev.getComment());
                m.put("createdAt", ev.getCreatedAt().toString());
                return m;
            })
            .toList();

        return new DisputeDto(
            d.getId(),
            d.getTransaction() != null ? d.getTransaction().getId() : null,
            d.getTransaction() != null ? d.getTransaction().getTransactionCode() : "N/A",
            d.getItem() != null ? d.getItem().getId() : null,
            d.getItem() != null ? d.getItem().getTitle() : "General Platform Report",
            d.getReporter().getId(),
            d.getReporter().getFullName(),
            d.getReportedUser().getId(),
            d.getReportedUser().getFullName(),
            d.getCategory(),
            d.getTitle(),
            d.getDescription(),
            d.getStatus().name(),
            d.getResolutionNotes(),
            d.getResolvedAt() != null ? d.getResolvedAt().toString() : null,
            d.getCreatedAt().toString(),
            evList
        );
    }
}
