package com.recircle.entity;

import com.recircle.enums.BorrowStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "borrow_requests")
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lender_id", nullable = false)
    private User lender;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "purpose", nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BorrowStatus status = BorrowStatus.PENDING;

    @Column(name = "owner_response_note", length = 400)
    private String ownerResponseNote;

    @Column(name = "handover_confirmed_at")
    private Instant handoverConfirmedAt;

    @Column(name = "return_requested_at")
    private Instant returnRequestedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "returned_on_time")
    private Boolean returnedOnTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public User getBorrower() { return borrower; }
    public void setBorrower(User borrower) { this.borrower = borrower; }
    public User getLender() { return lender; }
    public void setLender(User lender) { this.lender = lender; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public BorrowStatus getStatus() { return status; }
    public void setStatus(BorrowStatus status) { this.status = status; }
    public String getOwnerResponseNote() { return ownerResponseNote; }
    public void setOwnerResponseNote(String ownerResponseNote) { this.ownerResponseNote = ownerResponseNote; }
    public Instant getHandoverConfirmedAt() { return handoverConfirmedAt; }
    public void setHandoverConfirmedAt(Instant handoverConfirmedAt) { this.handoverConfirmedAt = handoverConfirmedAt; }
    public Instant getReturnRequestedAt() { return returnRequestedAt; }
    public void setReturnRequestedAt(Instant returnRequestedAt) { this.returnRequestedAt = returnRequestedAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public Boolean getReturnedOnTime() { return returnedOnTime; }
    public void setReturnedOnTime(Boolean returnedOnTime) { this.returnedOnTime = returnedOnTime; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
