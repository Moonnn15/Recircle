package com.recircle.service;

import com.recircle.dto.ApiDtos.*;
import com.recircle.entity.*;
import com.recircle.enums.*;
import com.recircle.exception.ApiException;
import com.recircle.mapper.DtoMapper;
import com.recircle.repository.*;
import com.recircle.validation.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements complete business logic and concurrency-safe validation for:
 * 1. Borrowing & Lending Management
 * 2. Exchange Management
 * 3. Rental Management (with backend cost calculation & simulated payment status)
 * 4. Donation Management
 * 5. Unified Transaction History Ledger
 */
@Service
public class SharingWorkflowService {

    private static final List<BorrowStatus> ACTIVE_BORROW_STATUSES = List.of(
        BorrowStatus.APPROVED, BorrowStatus.ACTIVE, BorrowStatus.RETURN_REQUESTED
    );

    private static final List<RentalStatus> ACTIVE_RENTAL_STATUSES = List.of(
        RentalStatus.APPROVED, RentalStatus.ACTIVE
    );

    private static final List<ExchangeStatus> ACTIVE_EXCHANGE_STATUSES = List.of(
        ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED, ExchangeStatus.HANDOVER_PENDING
    );

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ExchangeOfferRepository exchangeOfferRepository;
    private final RentalRepository rentalRepository;
    private final DonationRequestRepository donationRequestRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final TrustScoreService trustScoreService;
    private final NotificationService notificationService;
    private final InputValidator inputValidator;
    private final DtoMapper dtoMapper;

    public SharingWorkflowService(
        ItemRepository itemRepository,
        UserRepository userRepository,
        BorrowRequestRepository borrowRequestRepository,
        ExchangeOfferRepository exchangeOfferRepository,
        RentalRepository rentalRepository,
        DonationRequestRepository donationRequestRepository,
        TransactionRecordRepository transactionRecordRepository,
        TrustScoreService trustScoreService,
        NotificationService notificationService,
        InputValidator inputValidator,
        DtoMapper dtoMapper
    ) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.exchangeOfferRepository = exchangeOfferRepository;
        this.rentalRepository = rentalRepository;
        this.donationRequestRepository = donationRequestRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.trustScoreService = trustScoreService;
        this.notificationService = notificationService;
        this.inputValidator = inputValidator;
        this.dtoMapper = dtoMapper;
    }

    // ========================================================================
    // 1. BORROWING AND LENDING WORKFLOW
    // ========================================================================

    @Transactional
    public BorrowRequestDto createBorrowRequest(Long borrowerId, BorrowCreateRequest req) {
        if (req.itemId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item ID is required.");
        }
        inputValidator.requireNonBlank(req.startDate(), "Start date is required.");
        inputValidator.requireNonBlank(req.endDate(), "End date is required.");
        inputValidator.requireNonBlank(req.purpose(), "Borrowing purpose is required.");

        Item item = itemRepository.findByIdForUpdate(req.itemId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        if (item.isArchived()
            || item.getAvailabilityStatus() == ListingStatus.ARCHIVED
            || item.getAvailabilityStatus() == ListingStatus.UNAVAILABLE
            || item.getAvailabilityStatus() == ListingStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_UNAVAILABLE", "This item is not currently available for borrowing.");
        }
        if (item.getOwner().getId().equals(borrowerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_REQUEST_FORBIDDEN", "You cannot request your own item listing.");
        }

        LocalDate start = LocalDate.parse(req.startDate());
        LocalDate end = LocalDate.parse(req.endDate());
        if (end.isBefore(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "End date cannot be earlier than start date.");
        }
        long requestedDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (requestedDays > item.getMaxBorrowDays()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "DURATION_EXCEEDED",
                "Requested duration (" + requestedDays + " days) exceeds owner's maximum limit of " + item.getMaxBorrowDays() + " days."
            );
        }

        boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(
            item.getId(), start, end, ACTIVE_BORROW_STATUSES, null
        );
        boolean overlapRental = rentalRepository.existsOverlappingRental(
            item.getId(), start, end, ACTIVE_RENTAL_STATUSES, null
        );
        if (overlapBorrow || overlapRental) {
            throw new ApiException(HttpStatus.CONFLICT, "OVERLAPPING_RESERVATION", "This item already has a confirmed reservation during the selected dates.");
        }
        if (borrowRequestRepository.existsDuplicatePendingBorrow(item.getId(), borrowerId, start, end)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_BORROW_REQUEST", "You already have a pending borrow request for this item during the selected dates.");
        }

        User borrower = userRepository.findById(borrowerId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrower not found."));

        BorrowRequest br = new BorrowRequest();
        br.setItem(item);
        br.setBorrower(borrower);
        br.setLender(item.getOwner());
        br.setStartDate(start);
        br.setEndDate(end);
        br.setPurpose(inputValidator.sanitizeText(req.purpose()));
        br.setStatus(BorrowStatus.PENDING);
        BorrowRequest saved = borrowRequestRepository.save(br);

        createOrUpdateLedgerRecord(
            SharingType.BORROW,
            saved.getId(),
            item,
            null,
            borrower,
            item.getOwner(),
            "PENDING",
            BigDecimal.ZERO,
            PaymentStatus.NOT_REQUIRED,
            start,
            end,
            saved.getPurpose()
        );

        notificationService.createNotification(
            item.getOwner(),
            "BORROW_REQUEST_RECEIVED",
            "New Borrow Request for " + item.getTitle(),
            borrower.getFullName() + " requested to borrow '" + item.getTitle() + "' from " + start + " to " + end + ".",
            "/lending",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestDto> getMyBorrowRequests(Long borrowerId) {
        return borrowRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(borrowerId)
            .stream()
            .map(dtoMapper::toBorrowDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestDto> getReceivedBorrowRequests(Long lenderId) {
        return borrowRequestRepository.findByLenderIdOrderByCreatedAtDesc(lenderId)
            .stream()
            .map(dtoMapper::toBorrowDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public BorrowRequestDto getBorrowRequestById(Long requestId, Long userId) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        validateParticipantOrAdmin(userId, br.getBorrower().getId(), br.getLender().getId());
        return dtoMapper.toBorrowDto(br);
    }

    @Transactional
    public BorrowRequestDto approveBorrowRequest(Long requestId, Long ownerId, String note) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));

        if (!br.getLender().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can approve this borrow request.");
        }
        if (br.getStatus() != BorrowStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending borrow requests can be approved.");
        }

        // Lock item row for concurrency-safe date-range booking approval (Rule 3.A)
        Item item = itemRepository.findByIdForUpdate(br.getItem().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        // Concurrency & overlap recheck inside transaction
        boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(
            item.getId(), br.getStartDate(), br.getEndDate(), ACTIVE_BORROW_STATUSES, br.getId()
        );
        boolean overlapRental = rentalRepository.existsOverlappingRental(
            item.getId(), br.getStartDate(), br.getEndDate(), ACTIVE_RENTAL_STATUSES, null
        );
        if (overlapBorrow || overlapRental) {
            throw new ApiException(HttpStatus.CONFLICT, "OVERLAPPING_RESERVATION", "Conflicting approved reservation exists for these dates.");
        }

        br.setStatus(BorrowStatus.APPROVED);
        br.setOwnerResponseNote(note != null ? inputValidator.sanitizeText(note) : "Approved! Meet at campus pickup location.");
        BorrowRequest saved = borrowRequestRepository.save(br);

        // Keep listing status AVAILABLE so non-overlapping dates remain bookable; auto-reject overlapping PENDING requests
        List<BorrowRequest> conflictingPendings = borrowRequestRepository.findOverlappingPendingRequests(
            item.getId(), br.getStartDate(), br.getEndDate(), br.getId()
        );
        for (BorrowRequest pending : conflictingPendings) {
            pending.setStatus(BorrowStatus.REJECTED);
            pending.setOwnerResponseNote("Automatically declined due to another approved booking for overlapping dates.");
            borrowRequestRepository.save(pending);
            updateLedgerStatus(SharingType.BORROW, pending.getId(), "REJECTED", null, null);
            notificationService.createNotification(
                pending.getBorrower(),
                "BORROW_REQUEST_REJECTED",
                "Borrow Request Declined (Date Conflict): " + item.getTitle(),
                "Another request for '" + item.getTitle() + "' was approved for overlapping dates.",
                "/borrowing",
                pending.getId()
            );
        }

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "APPROVED", null, null);

        notificationService.createNotification(
            br.getBorrower(),
            "BORROW_REQUEST_APPROVED",
            "Borrow Request Approved: " + item.getTitle(),
            br.getLender().getFullName() + " approved your borrow request. Please confirm handover upon receiving the item.",
            "/borrowing",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional
    public BorrowRequestDto rejectBorrowRequest(Long requestId, Long ownerId, String note) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        if (!br.getLender().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can reject this request.");
        }
        if (br.getStatus() != BorrowStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be rejected.");
        }
        br.setStatus(BorrowStatus.REJECTED);
        br.setOwnerResponseNote(note != null ? inputValidator.sanitizeText(note) : "Declined due to schedule conflict.");
        BorrowRequest saved = borrowRequestRepository.save(br);

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "REJECTED", null, null);

        notificationService.createNotification(
            br.getBorrower(),
            "BORROW_REQUEST_REJECTED",
            "Borrow Request Declined: " + br.getItem().getTitle(),
            "Your request for '" + br.getItem().getTitle() + "' was declined.",
            "/borrowing",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional
    public BorrowRequestDto cancelBorrowRequest(Long requestId, Long userId) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        if (!br.getBorrower().getId().equals(userId) && !br.getLender().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized to cancel this request.");
        }
        if (br.getStatus() != BorrowStatus.PENDING && br.getStatus() != BorrowStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot cancel an already active or completed transaction.");
        }

        br.setStatus(BorrowStatus.CANCELLED);
        BorrowRequest saved = borrowRequestRepository.save(br);

        Item item = br.getItem();
        if (item.getAvailabilityStatus() == ListingStatus.RESERVED) {
            item.setAvailabilityStatus(ListingStatus.AVAILABLE);
            itemRepository.save(item);
        }

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "CANCELLED", null, null);
        User notifyTarget = br.getBorrower().getId().equals(userId) ? br.getLender() : br.getBorrower();
        notificationService.createNotification(
            notifyTarget,
            "BORROW_REQUEST_CANCELLED",
            "Borrow Request Cancelled: " + item.getTitle(),
            "The borrow request for '" + item.getTitle() + "' (" + br.getStartDate() + " to " + br.getEndDate() + ") was cancelled.",
            br.getBorrower().getId().equals(userId) ? "/lending" : "/borrowing",
            saved.getId()
        );
        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional
    public BorrowRequestDto confirmBorrowHandover(Long requestId, Long userId) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        if (!br.getBorrower().getId().equals(userId) && !br.getLender().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only transaction participants can confirm handover.");
        }
        if (br.getStatus() != BorrowStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request must be APPROVED before confirming handover.");
        }

        br.setStatus(BorrowStatus.ACTIVE);
        br.setHandoverConfirmedAt(Instant.now());
        BorrowRequest saved = borrowRequestRepository.save(br);

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "ACTIVE", null, null);

        User counterparty = br.getBorrower().getId().equals(userId) ? br.getLender() : br.getBorrower();
        notificationService.createNotification(
            counterparty,
            "HANDOVER_CONFIRMED",
            "Handover Confirmed: " + br.getItem().getTitle(),
            "Handover for '" + br.getItem().getTitle() + "' is confirmed and active. Due date: " + br.getEndDate() + ".",
            br.getBorrower().getId().equals(userId) ? "/lending" : "/borrowing",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional
    public BorrowRequestDto requestBorrowReturn(Long requestId, Long borrowerId) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        if (!br.getBorrower().getId().equals(borrowerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the borrower can initiate return confirmation.");
        }
        if (br.getStatus() != BorrowStatus.ACTIVE && br.getStatus() != BorrowStatus.OVERDUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only active or overdue borrowings can be marked for return.");
        }

        br.setStatus(BorrowStatus.RETURN_REQUESTED);
        br.setReturnRequestedAt(Instant.now());
        BorrowRequest saved = borrowRequestRepository.save(br);

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "RETURN_REQUESTED", null, null);

        notificationService.createNotification(
            br.getLender(),
            "RETURN_REQUESTED",
            "Return Initiated: " + br.getItem().getTitle(),
            br.getBorrower().getFullName() + " marked '" + br.getItem().getTitle() + "' as ready for return. Please confirm receipt.",
            "/lending",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    @Transactional
    public BorrowRequestDto confirmBorrowReturn(Long requestId, Long lenderId) {
        BorrowRequest br = borrowRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Borrow request not found."));
        if (!br.getLender().getId().equals(lenderId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can confirm final return.");
        }
        if (br.getStatus() != BorrowStatus.ACTIVE && br.getStatus() != BorrowStatus.RETURN_REQUESTED && br.getStatus() != BorrowStatus.OVERDUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Borrowing must be active or return-requested to complete.");
        }

        Instant now = Instant.now();
        boolean onTime = !LocalDate.now().isAfter(br.getEndDate());
        br.setStatus(BorrowStatus.COMPLETED);
        br.setReturnedAt(now);
        br.setReturnedOnTime(onTime);
        BorrowRequest saved = borrowRequestRepository.save(br);

        Item item = br.getItem();
        item.setAvailabilityStatus(ListingStatus.AVAILABLE);
        itemRepository.save(item);

        updateLedgerStatus(SharingType.BORROW, saved.getId(), "COMPLETED", now, onTime);

        trustScoreService.recalculateTrustScore(
            br.getBorrower().getId(),
            "BORROW_COMPLETED",
            onTime ? "Returned borrowed item '" + item.getTitle() + "' on time." : "Returned borrowed item '" + item.getTitle() + "' after due date."
        );
        trustScoreService.recalculateTrustScore(
            br.getLender().getId(),
            "LENDING_COMPLETED",
            "Completed lending transaction for '" + item.getTitle() + "'."
        );

        notificationService.createNotification(
            br.getBorrower(),
            "RETURN_CONFIRMED",
            "Return Confirmed! Leave a Review",
            br.getLender().getFullName() + " confirmed the return of '" + item.getTitle() + "'. You can now rate each other!",
            "/reviews",
            saved.getId()
        );

        return dtoMapper.toBorrowDto(saved);
    }

    // ========================================================================
    // 2. EXCHANGE MANAGEMENT WORKFLOW
    // ========================================================================

    @Transactional
    public ExchangeOfferDto createExchangeOffer(Long proposerId, ExchangeCreateRequest req) {
        if (req.requestedItemId() == null || req.offeredItemId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Both requested item and offered item must be selected.");
        }
        if (req.requestedItemId().equals(req.offeredItemId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_EXCHANGE_FORBIDDEN", "Cannot propose an exchange using the same item.");
        }
        inputValidator.requireNonBlank(req.proposalMessage(), "Exchange proposal message is required.");

        // US11: Lock both item records in ascending ID order to prevent deadlocks
        Long firstId = Math.min(req.requestedItemId(), req.offeredItemId());
        Long secondId = Math.max(req.requestedItemId(), req.offeredItemId());
        Item firstLocked = itemRepository.findByIdForUpdate(firstId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found: " + firstId));
        Item secondLocked = itemRepository.findByIdForUpdate(secondId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found: " + secondId));

        Item requestedItem = req.requestedItemId().equals(firstId) ? firstLocked : secondLocked;
        Item offeredItem = req.offeredItemId().equals(firstId) ? firstLocked : secondLocked;

        if (!offeredItem.getOwner().getId().equals(proposerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNERSHIP_ERROR", "You can only offer an item that you own.");
        }
        if (requestedItem.getOwner().getId().equals(proposerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_EXCHANGE_FORBIDDEN", "You cannot propose an exchange with your own listing.");
        }
        if (requestedItem.isArchived() || offeredItem.isArchived()
            || requestedItem.getAvailabilityStatus() != ListingStatus.AVAILABLE
            || offeredItem.getAvailabilityStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_UNAVAILABLE", "Both items must be AVAILABLE to initiate an exchange.");
        }

        boolean reqBusy = exchangeOfferRepository.existsActiveExchangeForItem(requestedItem.getId(), ACTIVE_EXCHANGE_STATUSES, null);
        boolean offBusy = exchangeOfferRepository.existsActiveExchangeForItem(offeredItem.getId(), ACTIVE_EXCHANGE_STATUSES, null);
        if (reqBusy || offBusy) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTIVE_EXCHANGE_CONFLICT", "One of the selected items is already involved in an active exchange.");
        }

        ExchangeOffer eo = new ExchangeOffer();
        eo.setRequestedItem(requestedItem);
        eo.setOfferedItem(offeredItem);
        eo.setProposer(offeredItem.getOwner());
        eo.setReceiver(requestedItem.getOwner());
        eo.setProposalMessage(inputValidator.sanitizeText(req.proposalMessage()));
        eo.setStatus(ExchangeStatus.PENDING);
        ExchangeOffer saved = exchangeOfferRepository.save(eo);

        createOrUpdateLedgerRecord(
            SharingType.EXCHANGE,
            saved.getId(),
            requestedItem,
            offeredItem,
            eo.getProposer(),
            eo.getReceiver(),
            "PENDING",
            BigDecimal.ZERO,
            PaymentStatus.NOT_REQUIRED,
            LocalDate.now(),
            null,
            eo.getProposalMessage()
        );

        notificationService.createNotification(
            eo.getReceiver(),
            "EXCHANGE_PROPOSAL_RECEIVED",
            "New Exchange Proposal for " + requestedItem.getTitle(),
            eo.getProposer().getFullName() + " offered '" + offeredItem.getTitle() + "' in exchange for '" + requestedItem.getTitle() + "'.",
            "/exchanges/" + saved.getId(),
            saved.getId()
        );

        return dtoMapper.toExchangeDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExchangeOfferDto> getUserExchanges(Long userId) {
        return exchangeOfferRepository.findByParticipant(userId)
            .stream()
            .map(dtoMapper::toExchangeDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExchangeOfferDto getExchangeById(Long exchangeId, Long userId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        validateParticipantOrAdmin(userId, eo.getProposer().getId(), eo.getReceiver().getId());
        return dtoMapper.toExchangeDto(eo);
    }

    @Transactional
    public ExchangeOfferDto acceptExchangeOffer(Long exchangeId, Long receiverId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        if (!eo.getReceiver().getId().equals(receiverId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the receiving item owner can accept this exchange proposal.");
        }
        if (eo.getStatus() != ExchangeStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending exchange offers can be accepted.");
        }

        // US11: Lock both item records in ascending ID order
        Long reqItemId = eo.getRequestedItem().getId();
        Long offItemId = eo.getOfferedItem().getId();
        Long firstId = Math.min(reqItemId, offItemId);
        Long secondId = Math.max(reqItemId, offItemId);
        Item firstLocked = itemRepository.findByIdForUpdate(firstId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));
        Item secondLocked = itemRepository.findByIdForUpdate(secondId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        Item reqItem = reqItemId.equals(firstId) ? firstLocked : secondLocked;
        Item offItem = offItemId.equals(firstId) ? firstLocked : secondLocked;

        if (reqItem.isArchived() || offItem.isArchived()
            || reqItem.getAvailabilityStatus() != ListingStatus.AVAILABLE
            || offItem.getAvailabilityStatus() != ListingStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "ITEM_UNAVAILABLE", "Both items must still be AVAILABLE to accept this exchange.");
        }

        eo.setStatus(ExchangeStatus.ACCEPTED);
        ExchangeOffer saved = exchangeOfferRepository.save(eo);

        reqItem.setAvailabilityStatus(ListingStatus.RESERVED);
        offItem.setAvailabilityStatus(ListingStatus.RESERVED);
        itemRepository.save(reqItem);
        itemRepository.save(offItem);

        // Reject competing pending exchanges involving either item
        List<ExchangeOffer> competing = exchangeOfferRepository.findCompetingPendingExchanges(reqItemId, offItemId, saved.getId());
        for (ExchangeOffer comp : competing) {
            comp.setStatus(ExchangeStatus.REJECTED);
            exchangeOfferRepository.save(comp);
            updateLedgerStatus(SharingType.EXCHANGE, comp.getId(), "REJECTED", null, null);
            notificationService.createNotification(
                comp.getProposer(),
                "EXCHANGE_REJECTED",
                "Exchange Proposal Declined (Item Reserved)",
                "One of the items in your exchange proposal was accepted in another swap.",
                "/exchanges",
                comp.getId()
            );
        }

        updateLedgerStatus(SharingType.EXCHANGE, saved.getId(), "ACCEPTED", null, null);

        notificationService.createNotification(
            eo.getProposer(),
            "EXCHANGE_ACCEPTED",
            "Exchange Proposal Accepted!",
            eo.getReceiver().getFullName() + " accepted your exchange offer! Both items are now reserved for handover.",
            "/exchanges/" + saved.getId(),
            saved.getId()
        );

        return dtoMapper.toExchangeDto(saved);
    }

    @Transactional
    public ExchangeOfferDto rejectExchangeOffer(Long exchangeId, Long userId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        if (!eo.getReceiver().getId().equals(userId) && !eo.getProposer().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized action.");
        }
        if (eo.getStatus() != ExchangeStatus.PENDING && eo.getStatus() != ExchangeStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot reject an already completed or cancelled exchange.");
        }
        if (eo.getStatus() == ExchangeStatus.ACCEPTED) {
            Item reqItem = eo.getRequestedItem();
            Item offItem = eo.getOfferedItem();
            reqItem.setAvailabilityStatus(ListingStatus.AVAILABLE);
            offItem.setAvailabilityStatus(ListingStatus.AVAILABLE);
            itemRepository.save(reqItem);
            itemRepository.save(offItem);
        }
        eo.setStatus(ExchangeStatus.REJECTED);
        ExchangeOffer saved = exchangeOfferRepository.save(eo);
        updateLedgerStatus(SharingType.EXCHANGE, saved.getId(), "REJECTED", null, null);

        User notifyUser = eo.getReceiver().getId().equals(userId) ? eo.getProposer() : eo.getReceiver();
        notificationService.createNotification(
            notifyUser,
            "EXCHANGE_REJECTED",
            "Exchange Proposal Declined",
            "The exchange proposal for '" + eo.getRequestedItem().getTitle() + "' was declined.",
            "/exchanges",
            saved.getId()
        );
        return dtoMapper.toExchangeDto(saved);
    }

    @Transactional
    public ExchangeOfferDto cancelExchangeOffer(Long exchangeId, Long userId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        if (!eo.getProposer().getId().equals(userId) && !eo.getReceiver().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only exchange participants can cancel this proposal.");
        }
        if (eo.getStatus() != ExchangeStatus.PENDING && eo.getStatus() != ExchangeStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot cancel an exchange that is already in handover or completed.");
        }
        if (eo.getStatus() == ExchangeStatus.ACCEPTED) {
            Item reqItem = eo.getRequestedItem();
            Item offItem = eo.getOfferedItem();
            reqItem.setAvailabilityStatus(ListingStatus.AVAILABLE);
            offItem.setAvailabilityStatus(ListingStatus.AVAILABLE);
            itemRepository.save(reqItem);
            itemRepository.save(offItem);
        }
        eo.setStatus(ExchangeStatus.CANCELLED);
        ExchangeOffer saved = exchangeOfferRepository.save(eo);
        updateLedgerStatus(SharingType.EXCHANGE, saved.getId(), "CANCELLED", null, null);

        User notifyUser = eo.getProposer().getId().equals(userId) ? eo.getReceiver() : eo.getProposer();
        notificationService.createNotification(
            notifyUser,
            "EXCHANGE_CANCELLED",
            "Exchange Proposal Cancelled",
            "The exchange proposal for '" + eo.getRequestedItem().getTitle() + "' was cancelled.",
            "/exchanges",
            saved.getId()
        );
        return dtoMapper.toExchangeDto(saved);
    }

    @Transactional
    public ExchangeOfferDto confirmExchangeHandover(Long exchangeId, Long userId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        if (eo.getStatus() != ExchangeStatus.ACCEPTED && eo.getStatus() != ExchangeStatus.HANDOVER_PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exchange must be ACCEPTED before confirming handover.");
        }

        if (eo.getProposer().getId().equals(userId)) {
            eo.setProposerHandoverConfirmed(true);
        } else if (eo.getReceiver().getId().equals(userId)) {
            eo.setReceiverHandoverConfirmed(true);
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only exchange participants can confirm handover.");
        }

        if (eo.isProposerHandoverConfirmed() && eo.isReceiverHandoverConfirmed()) {
            return finalizeExchangeCompletion(eo);
        } else {
            eo.setStatus(ExchangeStatus.HANDOVER_PENDING);
            ExchangeOffer saved = exchangeOfferRepository.save(eo);
            updateLedgerStatus(SharingType.EXCHANGE, saved.getId(), "HANDOVER_PENDING", null, null);

            User counterparty = eo.getProposer().getId().equals(userId) ? eo.getReceiver() : eo.getProposer();
            notificationService.createNotification(
                counterparty,
                "EXCHANGE_HANDOVER_CONFIRMED",
                "Exchange Handover Confirmed by Partner",
                "Your exchange partner confirmed handover for '" + eo.getRequestedItem().getTitle() + "'. Please confirm your handover to complete the swap.",
                "/exchanges/" + saved.getId(),
                saved.getId()
            );
            return dtoMapper.toExchangeDto(saved);
        }
    }

    @Transactional
    public ExchangeOfferDto completeExchange(Long exchangeId, Long userId) {
        ExchangeOffer eo = exchangeOfferRepository.findById(exchangeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange offer not found."));
        if (!eo.getProposer().getId().equals(userId) && !eo.getReceiver().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only exchange participants can complete the exchange.");
        }
        if (eo.getStatus() != ExchangeStatus.ACCEPTED && eo.getStatus() != ExchangeStatus.HANDOVER_PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exchange must be ACCEPTED or HANDOVER_PENDING before completion.");
        }
        if (eo.getProposer().getId().equals(userId)) {
            eo.setProposerHandoverConfirmed(true);
        } else {
            eo.setReceiverHandoverConfirmed(true);
        }
        if (!eo.isProposerHandoverConfirmed() || !eo.isReceiverHandoverConfirmed()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "DUAL_HANDOVER_REQUIRED",
                "Both exchange participants must confirm handover before completing the exchange."
            );
        }
        return finalizeExchangeCompletion(eo);
    }

    private ExchangeOfferDto finalizeExchangeCompletion(ExchangeOffer eo) {
        Instant now = Instant.now();
        eo.setStatus(ExchangeStatus.COMPLETED);
        eo.setCompletedAt(now);
        ExchangeOffer saved = exchangeOfferRepository.save(eo);

        Item reqItem = eo.getRequestedItem();
        Item offItem = eo.getOfferedItem();
        reqItem.setAvailabilityStatus(ListingStatus.COMPLETED);
        offItem.setAvailabilityStatus(ListingStatus.COMPLETED);
        itemRepository.save(reqItem);
        itemRepository.save(offItem);

        updateLedgerStatus(SharingType.EXCHANGE, saved.getId(), "COMPLETED", now, true);

        trustScoreService.recalculateTrustScore(eo.getProposer().getId(), "EXCHANGE_COMPLETED", "Completed campus item exchange.");
        trustScoreService.recalculateTrustScore(eo.getReceiver().getId(), "EXCHANGE_COMPLETED", "Completed campus item exchange.");

        notificationService.createNotification(
            eo.getProposer(),
            "EXCHANGE_COMPLETED",
            "Exchange Completed! Leave a Review",
            "Swap of '" + offItem.getTitle() + "' and '" + reqItem.getTitle() + "' is complete.",
            "/exchanges/" + saved.getId(),
            saved.getId()
        );
        notificationService.createNotification(
            eo.getReceiver(),
            "EXCHANGE_COMPLETED",
            "Exchange Completed! Leave a Review",
            "Swap of '" + offItem.getTitle() + "' and '" + reqItem.getTitle() + "' is complete.",
            "/exchanges/" + saved.getId(),
            saved.getId()
        );

        return dtoMapper.toExchangeDto(saved);
    }

    // ========================================================================
    // 3. RENTAL MANAGEMENT WORKFLOW (Backend Cost Calculation + Simulated Pay)
    // ========================================================================

    @Transactional(readOnly = true)
    public RentalCalculationDto calculateRentalQuote(RentalCalculateRequest req) {
        if (req.itemId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item ID is required.");
        }
        inputValidator.requireNonBlank(req.startDate(), "Rental start date is required.");
        inputValidator.requireNonBlank(req.endDate(), "Rental end date is required.");

        Item item = itemRepository.findById(req.itemId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        LocalDate start = LocalDate.parse(req.startDate());
        LocalDate end = LocalDate.parse(req.endDate());
        if (end.isBefore(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "End date cannot be earlier than start date.");
        }

        long diffDays = ChronoUnit.DAYS.between(start, end);
        int rentalDays = (int) (diffDays == 0 ? 1 : diffDays);
        BigDecimal dailyRate = item.getRentalPricePerDay().compareTo(BigDecimal.ZERO) > 0
            ? item.getRentalPricePerDay()
            : new BigDecimal("50.00");
        BigDecimal totalRentalAmount = dailyRate.multiply(BigDecimal.valueOf(rentalDays));
        BigDecimal deposit = item.getSecurityDeposit() != null ? item.getSecurityDeposit() : BigDecimal.ZERO;
        BigDecimal totalPayable = totalRentalAmount.add(deposit);

        boolean overlapRental = rentalRepository.existsOverlappingRental(
            item.getId(), start, end, ACTIVE_RENTAL_STATUSES, null
        );
        boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(
            item.getId(), start, end, ACTIVE_BORROW_STATUSES, null
        );
        boolean available = !item.isArchived()
            && item.getAvailabilityStatus() == ListingStatus.AVAILABLE
            && !overlapRental && !overlapBorrow;

        return new RentalCalculationDto(
            item.getId(),
            item.getTitle(),
            start.toString(),
            end.toString(),
            rentalDays,
            dailyRate,
            totalRentalAmount,
            deposit,
            totalPayable,
            available,
            "PENDING_OR_SIMULATED",
            "Payment gateway is not integrated. Payments and security deposits remain PENDING unless explicitly simulated for academic demonstration."
        );
    }

    @Transactional
    public RentalDto createRentalRequest(Long renterId, RentalCreateRequest req) {
        if (req.itemId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item ID is required.");
        }
        inputValidator.requireNonBlank(req.startDate(), "Rental start date is required.");
        inputValidator.requireNonBlank(req.endDate(), "Rental end date is required.");

        Item item = itemRepository.findByIdForUpdate(req.itemId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        if (item.getOwner().getId().equals(renterId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_RENTAL_FORBIDDEN", "You cannot rent your own item.");
        }
        if (item.isArchived()
            || item.getAvailabilityStatus() == ListingStatus.ARCHIVED
            || item.getAvailabilityStatus() == ListingStatus.UNAVAILABLE
            || item.getAvailabilityStatus() == ListingStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_UNAVAILABLE", "Item is not currently available for rent.");
        }

        LocalDate start = LocalDate.parse(req.startDate());
        LocalDate end = LocalDate.parse(req.endDate());
        if (end.isBefore(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "End date cannot be earlier than start date.");
        }

        // Calculate inclusive or exact days matching specification example (e.g., 4 days * ₹50 = ₹200)
        long diffDays = ChronoUnit.DAYS.between(start, end);
        int rentalDays = (int) (diffDays == 0 ? 1 : diffDays);

        boolean overlapRental = rentalRepository.existsOverlappingRental(
            item.getId(), start, end, ACTIVE_RENTAL_STATUSES, null
        );
        boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(
            item.getId(), start, end, ACTIVE_BORROW_STATUSES, null
        );
        if (overlapRental || overlapBorrow) {
            throw new ApiException(HttpStatus.CONFLICT, "OVERLAPPING_RENTAL", "This item already has a confirmed rental during the requested period.");
        }

        BigDecimal dailyRate = item.getRentalPricePerDay().compareTo(BigDecimal.ZERO) > 0
            ? item.getRentalPricePerDay()
            : new BigDecimal("50.00");
        BigDecimal totalRentalAmount = dailyRate.multiply(BigDecimal.valueOf(rentalDays));

        User renter = userRepository.findById(renterId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Renter not found."));

        Rental r = new Rental();
        r.setItem(item);
        r.setRenter(renter);
        r.setOwner(item.getOwner());
        r.setStartDate(start);
        r.setEndDate(end);
        r.setRentalDays(rentalDays);
        r.setDailyRate(dailyRate);
        r.setSecurityDeposit(item.getSecurityDeposit());
        r.setTotalRentalAmount(totalRentalAmount);
        // Rule 3.B: Represent payment as PENDING unless simulated demo payment is explicitly requested
        r.setPaymentStatus(req.simulateImmediatePayment() ? PaymentStatus.PAID_SIMULATED : PaymentStatus.PENDING);
        r.setStatus(RentalStatus.PENDING);
        Rental saved = rentalRepository.save(r);

        String paymentNote = req.simulateImmediatePayment()
            ? "Rental for " + rentalDays + " days @ ₹" + dailyRate + "/day (Simulated Demo Payment)"
            : "Rental for " + rentalDays + " days @ ₹" + dailyRate + "/day (Payment Pending - Gateway Not Integrated)";

        createOrUpdateLedgerRecord(
            SharingType.RENT,
            saved.getId(),
            item,
            null,
            renter,
            item.getOwner(),
            "PENDING",
            totalRentalAmount,
            saved.getPaymentStatus(),
            start,
            end,
            paymentNote
        );

        notificationService.createNotification(
            item.getOwner(),
            "RENTAL_REQUEST_RECEIVED",
            "New Rental Request: " + item.getTitle(),
            renter.getFullName() + " requested to rent '" + item.getTitle() + "' for " + rentalDays + " days (Total: ₹" + totalRentalAmount + ").",
            "/rentals/" + saved.getId(),
            saved.getId()
        );

        return dtoMapper.toRentalDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getUserRentals(Long userId) {
        return rentalRepository.findByParticipant(userId)
            .stream()
            .map(dtoMapper::toRentalDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public RentalDto getRentalById(Long rentalId, Long userId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        validateParticipantOrAdmin(userId, r.getRenter().getId(), r.getOwner().getId());
        return dtoMapper.toRentalDto(r);
    }

    @Transactional
    public RentalDto approveRental(Long rentalId, Long ownerId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        if (!r.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can approve this rental.");
        }
        if (r.getStatus() != RentalStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending rental requests can be approved.");
        }

        // Lock item row for concurrency-safe date-range booking approval (Rule 3.A)
        Item item = itemRepository.findByIdForUpdate(r.getItem().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        boolean overlapRental = rentalRepository.existsOverlappingRental(
            item.getId(), r.getStartDate(), r.getEndDate(), ACTIVE_RENTAL_STATUSES, r.getId()
        );
        boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(
            item.getId(), r.getStartDate(), r.getEndDate(), ACTIVE_BORROW_STATUSES, null
        );
        if (overlapRental || overlapBorrow) {
            throw new ApiException(HttpStatus.CONFLICT, "OVERLAPPING_RENTAL", "Conflicting approved rental or reservation exists for these dates.");
        }

        // Rule 3.B: Do NOT mark payment as PAID merely because owner approved the rental request!
        r.setStatus(RentalStatus.APPROVED);
        Rental saved = rentalRepository.save(r);

        // Rule 3.A: Keep listing status AVAILABLE so non-overlapping dates remain bookable; auto-reject overlapping PENDING rentals
        List<Rental> conflictingPendings = rentalRepository.findOverlappingPendingRentals(
            item.getId(), r.getStartDate(), r.getEndDate(), r.getId()
        );
        for (Rental pending : conflictingPendings) {
            pending.setStatus(RentalStatus.REJECTED);
            if (pending.getPaymentStatus() == PaymentStatus.PAID_SIMULATED) {
                pending.setPaymentStatus(PaymentStatus.REFUNDED_SIMULATED);
            }
            rentalRepository.save(pending);
            updateLedgerStatus(SharingType.RENT, pending.getId(), "REJECTED", null, null);
            notificationService.createNotification(
                pending.getRenter(),
                "RENTAL_REJECTED",
                "Rental Request Declined (Date Conflict): " + item.getTitle(),
                "Another rental booking for '" + item.getTitle() + "' was approved for overlapping dates.",
                "/rentals",
                pending.getId()
            );
        }

        updateLedgerStatus(SharingType.RENT, saved.getId(), "APPROVED", null, null);

        notificationService.createNotification(
            r.getRenter(),
            "RENTAL_APPROVED",
            "Rental Approved: " + item.getTitle(),
            "Your rental request for '" + item.getTitle() + "' has been approved!",
            "/rentals/" + saved.getId(),
            saved.getId()
        );

        return dtoMapper.toRentalDto(saved);
    }

    @Transactional
    public RentalDto rejectRental(Long rentalId, Long ownerId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        if (!r.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can reject this rental.");
        }
        if (r.getStatus() != RentalStatus.PENDING && r.getStatus() != RentalStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending or approved rentals can be rejected.");
        }
        r.setStatus(RentalStatus.REJECTED);
        if (r.getPaymentStatus() == PaymentStatus.PAID_SIMULATED) {
            r.setPaymentStatus(PaymentStatus.REFUNDED_SIMULATED);
        }
        Rental saved = rentalRepository.save(r);
        updateLedgerStatus(SharingType.RENT, saved.getId(), "REJECTED", null, null);

        notificationService.createNotification(
            r.getRenter(),
            "RENTAL_REJECTED",
            "Rental Request Declined: " + r.getItem().getTitle(),
            "Your rental request for '" + r.getItem().getTitle() + "' was declined by the owner.",
            "/rentals",
            saved.getId()
        );
        return dtoMapper.toRentalDto(saved);
    }

    @Transactional
    public RentalDto cancelRental(Long rentalId, Long userId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        if (!r.getRenter().getId().equals(userId) && !r.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only rental participants can cancel this rental.");
        }
        if (r.getStatus() != RentalStatus.PENDING && r.getStatus() != RentalStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot cancel an already active or completed rental.");
        }
        r.setStatus(RentalStatus.CANCELLED);
        if (r.getPaymentStatus() == PaymentStatus.PAID_SIMULATED) {
            r.setPaymentStatus(PaymentStatus.REFUNDED_SIMULATED);
        }
        Rental saved = rentalRepository.save(r);
        updateLedgerStatus(SharingType.RENT, saved.getId(), "CANCELLED", null, null);

        User notifyUser = r.getRenter().getId().equals(userId) ? r.getOwner() : r.getRenter();
        notificationService.createNotification(
            notifyUser,
            "RENTAL_CANCELLED",
            "Rental Booking Cancelled: " + r.getItem().getTitle(),
            "The rental request for '" + r.getItem().getTitle() + "' (" + r.getStartDate() + " to " + r.getEndDate() + ") was cancelled.",
            "/rentals",
            saved.getId()
        );
        return dtoMapper.toRentalDto(saved);
    }

    @Transactional
    public RentalDto confirmRentalHandover(Long rentalId, Long userId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        if (!r.getRenter().getId().equals(userId) && !r.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized.");
        }
        if (r.getStatus() != RentalStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rental must be APPROVED before confirming handover.");
        }
        r.setStatus(RentalStatus.ACTIVE);
        r.setHandoverConfirmedAt(Instant.now());
        Rental saved = rentalRepository.save(r);
        updateLedgerStatus(SharingType.RENT, saved.getId(), "ACTIVE", null, null);

        User counterparty = r.getRenter().getId().equals(userId) ? r.getOwner() : r.getRenter();
        notificationService.createNotification(
            counterparty,
            "RENTAL_HANDOVER_CONFIRMED",
            "Rental Handover Confirmed: " + r.getItem().getTitle(),
            "Handover for rented item '" + r.getItem().getTitle() + "' is confirmed. Return due on " + r.getEndDate() + ".",
            "/rentals/" + saved.getId(),
            saved.getId()
        );
        return dtoMapper.toRentalDto(saved);
    }

    @Transactional
    public RentalDto confirmRentalReturn(Long rentalId, Long ownerId) {
        Rental r = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rental not found."));
        if (!r.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item owner can confirm rental return.");
        }
        if (r.getStatus() != RentalStatus.ACTIVE && r.getStatus() != RentalStatus.OVERDUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rental must be ACTIVE or OVERDUE to confirm return.");
        }

        Instant now = Instant.now();
        boolean onTime = !LocalDate.now().isAfter(r.getEndDate());
        r.setStatus(RentalStatus.COMPLETED);
        r.setReturnedAt(now);
        r.setReturnedOnTime(onTime);
        Rental saved = rentalRepository.save(r);

        Item item = r.getItem();
        item.setAvailabilityStatus(ListingStatus.AVAILABLE);
        itemRepository.save(item);

        updateLedgerStatus(SharingType.RENT, saved.getId(), "COMPLETED", now, onTime);

        trustScoreService.recalculateTrustScore(r.getRenter().getId(), "RENTAL_COMPLETED", "Completed rental of '" + item.getTitle() + "'.");
        trustScoreService.recalculateTrustScore(r.getOwner().getId(), "RENTAL_COMPLETED", "Completed rental as owner of '" + item.getTitle() + "'.");

        notificationService.createNotification(
            r.getRenter(),
            "RENTAL_COMPLETED",
            "Rental Return Confirmed! Leave a Review",
            r.getOwner().getFullName() + " confirmed the return of rented item '" + item.getTitle() + "'.",
            "/reviews",
            saved.getId()
        );

        return dtoMapper.toRentalDto(saved);
    }

    // ========================================================================
    // 4. DONATION MANAGEMENT WORKFLOW
    // ========================================================================

    @Transactional
    public DonationRequestDto requestDonation(Long requesterId, Long itemId, DonationCreateRequest req) {
        Long targetItemId = itemId != null ? itemId : req.itemId();
        if (targetItemId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Donation item ID is required.");
        }
        inputValidator.requireNonBlank(req.motivationMessage(), "Please explain how this donated resource will help your studies.");

        Item item = itemRepository.findByIdForUpdate(targetItemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation listing not found."));

        if (item.getSharingType() != SharingType.DONATE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_A_DONATION", "This item is not listed as a donation.");
        }
        if (item.getOwner().getId().equals(requesterId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_REQUEST_FORBIDDEN", "You cannot request your own donated item.");
        }
        if (item.isArchived() || item.getAvailabilityStatus() == ListingStatus.ARCHIVED || item.getAvailabilityStatus() == ListingStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_UNAVAILABLE", "This donated item is no longer available.");
        }
        if (donationRequestRepository.existsByItemIdAndStatusIn(item.getId(), List.of(DonationStatus.APPROVED, DonationStatus.COMPLETED))) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ASSIGNED", "This donation has already been assigned to a recipient.");
        }
        if (donationRequestRepository.existsByItemIdAndRequesterIdAndStatusIn(item.getId(), requesterId, List.of(DonationStatus.PENDING))) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_DONATION_REQUEST", "You already have a pending request for this donated item.");
        }

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Requester not found."));

        DonationRequest dr = new DonationRequest();
        dr.setItem(item);
        dr.setRequester(requester);
        dr.setDonor(item.getOwner());
        dr.setMotivationMessage(inputValidator.sanitizeText(req.motivationMessage()));
        dr.setStatus(DonationStatus.PENDING);
        DonationRequest saved = donationRequestRepository.save(dr);

        createOrUpdateLedgerRecord(
            SharingType.DONATE,
            saved.getId(),
            item,
            null,
            requester,
            item.getOwner(),
            "PENDING",
            BigDecimal.ZERO,
            PaymentStatus.NOT_REQUIRED,
            LocalDate.now(),
            null,
            dr.getMotivationMessage()
        );

        notificationService.createNotification(
            item.getOwner(),
            "DONATION_REQUEST_RECEIVED",
            "New Donation Request: " + item.getTitle(),
            requester.getFullName() + " requested your donated item '" + item.getTitle() + "'.",
            "/donations/requests",
            saved.getId()
        );

        return dtoMapper.toDonationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationRequestDto> getUserDonationRequests(Long userId) {
        return donationRequestRepository.findByParticipant(userId)
            .stream()
            .map(dtoMapper::toDonationDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public DonationRequestDto getDonationRequestById(Long requestId, Long userId) {
        DonationRequest dr = donationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation request not found."));
        validateParticipantOrAdmin(userId, dr.getRequester().getId(), dr.getDonor().getId());
        return dtoMapper.toDonationDto(dr);
    }

    @Transactional
    public DonationRequestDto approveDonationRequest(Long requestId, Long donorId) {
        DonationRequest dr = donationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation request not found."));
        if (!dr.getDonor().getId().equals(donorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the donor can approve a recipient.");
        }
        if (dr.getStatus() != DonationStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending donation requests can be approved.");
        }

        // Lock item row to enforce single-recipient atomicity
        Item item = itemRepository.findByIdForUpdate(dr.getItem().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found."));

        if (donationRequestRepository.existsByItemIdAndStatusIn(item.getId(), List.of(DonationStatus.APPROVED, DonationStatus.COMPLETED))) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ASSIGNED", "A donation cannot be assigned to multiple recipients.");
        }

        dr.setStatus(DonationStatus.APPROVED);
        DonationRequest saved = donationRequestRepository.save(dr);

        item.setAvailabilityStatus(ListingStatus.RESERVED);
        itemRepository.save(item);

        // US13: Atomically reject all competing pending requests for this donated item
        List<DonationRequest> competingRequests = donationRequestRepository.findCompetingPendingDonations(item.getId(), saved.getId());
        for (DonationRequest comp : competingRequests) {
            comp.setStatus(DonationStatus.REJECTED);
            donationRequestRepository.save(comp);
            updateLedgerStatus(SharingType.DONATE, comp.getId(), "REJECTED", null, null);
            notificationService.createNotification(
                comp.getRequester(),
                "DONATION_REJECTED",
                "Donation Assigned to Another Student: " + item.getTitle(),
                "Thank you for your interest. The donor selected another recipient for '" + item.getTitle() + "'.",
                "/donations/requests",
                comp.getId()
            );
        }

        updateLedgerStatus(SharingType.DONATE, saved.getId(), "APPROVED", null, null);

        notificationService.createNotification(
            dr.getRequester(),
            "DONATION_APPROVED",
            "Donation Request Approved!",
            dr.getDonor().getFullName() + " selected you as the recipient for '" + item.getTitle() + "'!",
            "/donations/requests",
            saved.getId()
        );

        return dtoMapper.toDonationDto(saved);
    }

    @Transactional
    public DonationRequestDto rejectDonationRequest(Long requestId, Long donorId) {
        DonationRequest dr = donationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation request not found."));
        if (!dr.getDonor().getId().equals(donorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the donor can reject this request.");
        }
        if (dr.getStatus() != DonationStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending donation requests can be rejected.");
        }
        dr.setStatus(DonationStatus.REJECTED);
        DonationRequest saved = donationRequestRepository.save(dr);
        updateLedgerStatus(SharingType.DONATE, saved.getId(), "REJECTED", null, null);

        notificationService.createNotification(
            dr.getRequester(),
            "DONATION_REJECTED",
            "Donation Request Declined: " + dr.getItem().getTitle(),
            "Your donation request for '" + dr.getItem().getTitle() + "' was declined.",
            "/donations/requests",
            saved.getId()
        );
        return dtoMapper.toDonationDto(saved);
    }

    @Transactional
    public DonationRequestDto cancelDonationRequest(Long requestId, Long userId) {
        DonationRequest dr = donationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation request not found."));
        if (!dr.getRequester().getId().equals(userId) && !dr.getDonor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only donation participants can cancel this request.");
        }
        if (dr.getStatus() != DonationStatus.PENDING && dr.getStatus() != DonationStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot cancel a completed or already rejected donation request.");
        }
        if (dr.getStatus() == DonationStatus.APPROVED) {
            Item item = dr.getItem();
            item.setAvailabilityStatus(ListingStatus.AVAILABLE);
            itemRepository.save(item);
        }
        dr.setStatus(DonationStatus.CANCELLED);
        DonationRequest saved = donationRequestRepository.save(dr);
        updateLedgerStatus(SharingType.DONATE, saved.getId(), "CANCELLED", null, null);

        User notifyUser = dr.getRequester().getId().equals(userId) ? dr.getDonor() : dr.getRequester();
        notificationService.createNotification(
            notifyUser,
            "DONATION_CANCELLED",
            "Donation Request Cancelled: " + dr.getItem().getTitle(),
            "The donation request for '" + dr.getItem().getTitle() + "' was cancelled.",
            "/donations/requests",
            saved.getId()
        );
        return dtoMapper.toDonationDto(saved);
    }

    @Transactional
    public DonationRequestDto completeDonationRequest(Long requestId, Long userId) {
        DonationRequest dr = donationRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Donation request not found."));
        if (!dr.getDonor().getId().equals(userId) && !dr.getRequester().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only donation participants can confirm handover completion.");
        }
        if (dr.getStatus() != DonationStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Donation request must be APPROVED before confirming handover completion.");
        }

        Instant now = Instant.now();
        dr.setStatus(DonationStatus.COMPLETED);
        dr.setHandoverConfirmedAt(now);
        dr.setCompletedAt(now);
        DonationRequest saved = donationRequestRepository.save(dr);

        Item item = dr.getItem();
        item.setAvailabilityStatus(ListingStatus.COMPLETED);
        itemRepository.save(item);

        updateLedgerStatus(SharingType.DONATE, saved.getId(), "COMPLETED", now, null);

        trustScoreService.recalculateTrustScore(dr.getDonor().getId(), "DONATION_COMPLETED", "Donated campus resource '" + item.getTitle() + "'.");
        trustScoreService.recalculateTrustScore(dr.getRequester().getId(), "DONATION_RECEIVED", "Received campus donation '" + item.getTitle() + "'.");

        notificationService.createNotification(
            dr.getRequester(),
            "DONATION_COMPLETED",
            "Donation Handover Completed! Leave a Review",
            "Handover of donated resource '" + item.getTitle() + "' is complete.",
            "/reviews",
            saved.getId()
        );
        notificationService.createNotification(
            dr.getDonor(),
            "DONATION_COMPLETED",
            "Donation Handover Completed! Leave a Review",
            "Handover of donated resource '" + item.getTitle() + "' to " + dr.getRequester().getFullName() + " is complete.",
            "/reviews",
            saved.getId()
        );

        return dtoMapper.toDonationDto(saved);
    }

    // ========================================================================
    // 5. UNIFIED TRANSACTION LEDGER
    // ========================================================================

    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactions(Long userId) {
        return transactionRecordRepository.findByParticipant(userId)
            .stream()
            .map(t -> dtoMapper.toTransactionDto(t, userId))
            .toList();
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long txId, Long currentUserId) {
        TransactionRecord t = transactionRecordRepository.findById(txId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction record not found."));
        validateParticipantOrAdmin(currentUserId, t.getInitiatorUser().getId(), t.getCounterpartyUser().getId());
        return dtoMapper.toTransactionDto(t, currentUserId);
    }

    private void validateParticipantOrAdmin(Long currentUserId, Long party1Id, Long party2Id) {
        if (currentUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        if (currentUserId.equals(party1Id) || currentUserId.equals(party2Id)) {
            return;
        }
        User u = userRepository.findById(currentUserId).orElse(null);
        if (u != null && (u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.MODERATOR)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not authorized to view or modify this transaction.");
    }

    private TransactionRecord createOrUpdateLedgerRecord(
        SharingType type,
        Long refId,
        Item primaryItem,
        Item secondaryItem,
        User initiator,
        User counterparty,
        String status,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        LocalDate startDate,
        LocalDate dueDate,
        String notes
    ) {
        TransactionRecord tx = transactionRecordRepository
            .findByTransactionTypeAndReferenceId(type, refId)
            .orElseGet(TransactionRecord::new);

        if (tx.getTransactionCode() == null) {
            String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            tx.setTransactionCode("RC-" + type.name().substring(0, 3) + "-" + suffix);
        }
        tx.setTransactionType(type);
        tx.setReferenceId(refId);
        tx.setPrimaryItem(primaryItem);
        tx.setSecondaryItem(secondaryItem);
        tx.setInitiatorUser(initiator);
        tx.setCounterpartyUser(counterparty);
        tx.setStatus(status);
        tx.setMonetaryAmount(amount != null ? amount : BigDecimal.ZERO);
        tx.setPaymentStatus(paymentStatus != null ? paymentStatus : PaymentStatus.NOT_REQUIRED);
        tx.setStartDate(startDate);
        tx.setDueDate(dueDate);
        tx.setNotes(notes);
        return transactionRecordRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkItemDateAvailability(Long itemId, String startDateStr, String endDateStr) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item listing not found."));

        List<BorrowRequest> activeBorrows = borrowRequestRepository.findActiveReservationsForItem(itemId, ACTIVE_BORROW_STATUSES);
        List<Rental> activeRentals = rentalRepository.findActiveRentalsForItem(itemId, ACTIVE_RENTAL_STATUSES);

        List<Map<String, Object>> reservedRanges = new ArrayList<>();
        for (BorrowRequest b : activeBorrows) {
            reservedRanges.add(Map.of(
                "bookingId", b.getId(),
                "type", "BORROW",
                "status", b.getStatus().name(),
                "startDate", b.getStartDate().toString(),
                "endDate", b.getEndDate().toString()
            ));
        }
        for (Rental r : activeRentals) {
            reservedRanges.add(Map.of(
                "bookingId", r.getId(),
                "type", "RENT",
                "status", r.getStatus().name(),
                "startDate", r.getStartDate().toString(),
                "endDate", r.getEndDate().toString()
            ));
        }

        boolean listingBookable = !item.isArchived()
            && item.getAvailabilityStatus() != ListingStatus.ARCHIVED
            && item.getAvailabilityStatus() != ListingStatus.UNAVAILABLE
            && item.getAvailabilityStatus() != ListingStatus.COMPLETED;

        boolean availableForRange = listingBookable;
        if (startDateStr != null && !startDateStr.isBlank() && endDateStr != null && !endDateStr.isBlank()) {
            LocalDate start = LocalDate.parse(startDateStr);
            LocalDate end = LocalDate.parse(endDateStr);
            if (end.isBefore(start)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "End date cannot be earlier than start date.");
            }
            boolean overlapBorrow = borrowRequestRepository.existsOverlappingReservation(itemId, start, end, ACTIVE_BORROW_STATUSES, null);
            boolean overlapRental = rentalRepository.existsOverlappingRental(itemId, start, end, ACTIVE_RENTAL_STATUSES, null);
            availableForRange = listingBookable && !overlapBorrow && !overlapRental;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("itemId", item.getId());
        response.put("listingStatus", item.getAvailabilityStatus().name());
        response.put("requestedStartDate", startDateStr);
        response.put("requestedEndDate", endDateStr);
        response.put("availableForRequestedDates", availableForRange);
        response.put("reservedDateRanges", reservedRanges);
        return response;
    }

    private void updateLedgerStatus(SharingType type, Long refId, String status, Instant completedAt, Boolean returnedOnTime) {
        transactionRecordRepository.findByTransactionTypeAndReferenceId(type, refId).ifPresent(tx -> {
            tx.setStatus(status);
            if (completedAt != null) {
                tx.setCompletedAt(completedAt);
            }
            if (returnedOnTime != null) {
                tx.setReturnedOnTime(returnedOnTime);
            }
            if (type == SharingType.RENT && "REJECTED".equals(status) && tx.getPaymentStatus() == PaymentStatus.PAID_SIMULATED) {
                tx.setPaymentStatus(PaymentStatus.REFUNDED_SIMULATED);
            }
            transactionRecordRepository.save(tx);
        });
    }
}
