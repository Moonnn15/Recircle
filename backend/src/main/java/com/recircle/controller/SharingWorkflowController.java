package com.recircle.controller;

import com.recircle.dto.ApiDtos.*;
import com.recircle.security.AuthContext;
import com.recircle.security.AuthPrincipal;
import com.recircle.service.SharingWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SharingWorkflowController {

    private final SharingWorkflowService sharingWorkflowService;

    public SharingWorkflowController(SharingWorkflowService sharingWorkflowService) {
        this.sharingWorkflowService = sharingWorkflowService;
    }

    // --- Borrowing & Lending ---

    @PostMapping("/borrow-requests")
    public ResponseEntity<BorrowRequestDto> createBorrowRequest(@RequestBody BorrowCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            sharingWorkflowService.createBorrowRequest(p.getUserId(), req)
        );
    }

    @GetMapping("/borrow-requests/my-requests")
    public ResponseEntity<List<BorrowRequestDto>> getMyBorrowRequests() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getMyBorrowRequests(p.getUserId()));
    }

    @GetMapping("/borrow-requests/received")
    public ResponseEntity<List<BorrowRequestDto>> getReceivedBorrowRequests() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getReceivedBorrowRequests(p.getUserId()));
    }

    @GetMapping("/borrow-requests/{id}")
    public ResponseEntity<BorrowRequestDto> getBorrowRequestById(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getBorrowRequestById(id, p.getUserId()));
    }

    @PutMapping("/borrow-requests/{id}/approve")
    public ResponseEntity<BorrowRequestDto> approveBorrow(
        @PathVariable Long id,
        @RequestBody(required = false) DecisionNoteRequest req
    ) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.approveBorrowRequest(
            id, p.getUserId(), req != null ? req.note() : null
        ));
    }

    @PutMapping("/borrow-requests/{id}/reject")
    public ResponseEntity<BorrowRequestDto> rejectBorrow(
        @PathVariable Long id,
        @RequestBody(required = false) DecisionNoteRequest req
    ) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.rejectBorrowRequest(
            id, p.getUserId(), req != null ? req.note() : null
        ));
    }

    @PutMapping("/borrow-requests/{id}/cancel")
    public ResponseEntity<BorrowRequestDto> cancelBorrow(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.cancelBorrowRequest(id, p.getUserId()));
    }

    @PutMapping("/borrow-requests/{id}/confirm-handover")
    public ResponseEntity<BorrowRequestDto> confirmBorrowHandover(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.confirmBorrowHandover(id, p.getUserId()));
    }

    @PutMapping("/borrow-requests/{id}/request-return")
    public ResponseEntity<BorrowRequestDto> requestBorrowReturn(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.requestBorrowReturn(id, p.getUserId()));
    }

    @PutMapping("/borrow-requests/{id}/confirm-return")
    public ResponseEntity<BorrowRequestDto> confirmBorrowReturn(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.confirmBorrowReturn(id, p.getUserId()));
    }

    // --- Exchanges ---

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeOfferDto> createExchange(@RequestBody ExchangeCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            sharingWorkflowService.createExchangeOffer(p.getUserId(), req)
        );
    }

    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeOfferDto>> getExchanges() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getUserExchanges(p.getUserId()));
    }

    @GetMapping("/exchanges/{id}")
    public ResponseEntity<ExchangeOfferDto> getExchangeById(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getExchangeById(id, p.getUserId()));
    }

    @PutMapping("/exchanges/{id}/accept")
    public ResponseEntity<ExchangeOfferDto> acceptExchange(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.acceptExchangeOffer(id, p.getUserId()));
    }

    @PutMapping("/exchanges/{id}/reject")
    public ResponseEntity<ExchangeOfferDto> rejectExchange(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.rejectExchangeOffer(id, p.getUserId()));
    }

    @PutMapping("/exchanges/{id}/cancel")
    public ResponseEntity<ExchangeOfferDto> cancelExchange(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.cancelExchangeOffer(id, p.getUserId()));
    }

    @PutMapping("/exchanges/{id}/confirm-handover")
    public ResponseEntity<ExchangeOfferDto> confirmExchangeHandover(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.confirmExchangeHandover(id, p.getUserId()));
    }

    @PutMapping("/exchanges/{id}/complete")
    public ResponseEntity<ExchangeOfferDto> completeExchange(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.completeExchange(id, p.getUserId()));
    }

    // --- Rentals ---

    @PostMapping("/rentals/calculate")
    public ResponseEntity<RentalCalculationDto> calculateRentalPost(@RequestBody RentalCalculateRequest req) {
        return ResponseEntity.ok(sharingWorkflowService.calculateRentalQuote(req));
    }

    @GetMapping("/rentals/calculate")
    public ResponseEntity<RentalCalculationDto> calculateRentalGet(
        @RequestParam Long itemId,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        return ResponseEntity.ok(sharingWorkflowService.calculateRentalQuote(
            new RentalCalculateRequest(itemId, startDate, endDate)
        ));
    }

    @PostMapping("/rentals")
    public ResponseEntity<RentalDto> createRental(@RequestBody RentalCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            sharingWorkflowService.createRentalRequest(p.getUserId(), req)
        );
    }

    @GetMapping("/rentals")
    public ResponseEntity<List<RentalDto>> getRentals() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getUserRentals(p.getUserId()));
    }

    @GetMapping("/rentals/{id}")
    public ResponseEntity<RentalDto> getRentalById(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getRentalById(id, p.getUserId()));
    }

    @PutMapping("/rentals/{id}/approve")
    public ResponseEntity<RentalDto> approveRental(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.approveRental(id, p.getUserId()));
    }

    @PutMapping("/rentals/{id}/reject")
    public ResponseEntity<RentalDto> rejectRental(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.rejectRental(id, p.getUserId()));
    }

    @PutMapping("/rentals/{id}/cancel")
    public ResponseEntity<RentalDto> cancelRental(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.cancelRental(id, p.getUserId()));
    }

    @PutMapping({"/rentals/{id}/confirm-handover", "/rentals/{id}/handover"})
    public ResponseEntity<RentalDto> confirmRentalHandover(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.confirmRentalHandover(id, p.getUserId()));
    }

    @PutMapping({"/rentals/{id}/confirm-return", "/rentals/{id}/complete"})
    public ResponseEntity<RentalDto> confirmRentalReturn(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.confirmRentalReturn(id, p.getUserId()));
    }

    // --- Donations ---

    @PostMapping("/donations")
    public ResponseEntity<DonationRequestDto> createDonationRequest(@RequestBody DonationCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            sharingWorkflowService.requestDonation(p.getUserId(), req.itemId(), req)
        );
    }

    @GetMapping({"/donations", "/donations/requests"})
    public ResponseEntity<List<DonationRequestDto>> getDonations() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getUserDonationRequests(p.getUserId()));
    }

    @GetMapping("/donations/requests/{id}")
    public ResponseEntity<DonationRequestDto> getDonationRequestById(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getDonationRequestById(id, p.getUserId()));
    }

    @PostMapping("/donations/{id}/request")
    public ResponseEntity<DonationRequestDto> requestDonationById(
        @PathVariable Long id,
        @RequestBody DonationCreateRequest req
    ) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            sharingWorkflowService.requestDonation(p.getUserId(), id, req)
        );
    }

    @PutMapping("/donations/requests/{id}/approve")
    public ResponseEntity<DonationRequestDto> approveDonation(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.approveDonationRequest(id, p.getUserId()));
    }

    @PutMapping("/donations/requests/{id}/reject")
    public ResponseEntity<DonationRequestDto> rejectDonation(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.rejectDonationRequest(id, p.getUserId()));
    }

    @PutMapping("/donations/requests/{id}/cancel")
    public ResponseEntity<DonationRequestDto> cancelDonation(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.cancelDonationRequest(id, p.getUserId()));
    }

    @PutMapping("/donations/requests/{id}/complete")
    public ResponseEntity<DonationRequestDto> completeDonation(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.completeDonationRequest(id, p.getUserId()));
    }

    // --- Unified Transactions ---

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getMyTransactions() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getUserTransactions(p.getUserId()));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(sharingWorkflowService.getTransactionById(id, p.getUserId()));
    }
}
