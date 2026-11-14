package com.recircle.entity;

import com.recircle.enums.PaymentStatus;
import com.recircle.enums.SharingType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 40)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 25)
    private SharingType transactionType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_item_id", nullable = false)
    private Item primaryItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "secondary_item_id")
    private Item secondaryItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "initiator_user_id", nullable = false)
    private User initiatorUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "counterparty_user_id", nullable = false)
    private User counterpartyUser;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "monetary_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal monetaryAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "returned_on_time")
    private Boolean returnedOnTime;

    @Column(name = "notes", length = 500)
    private String notes;

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
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public SharingType getTransactionType() { return transactionType; }
    public void setTransactionType(SharingType transactionType) { this.transactionType = transactionType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public Item getPrimaryItem() { return primaryItem; }
    public void setPrimaryItem(Item primaryItem) { this.primaryItem = primaryItem; }
    public Item getSecondaryItem() { return secondaryItem; }
    public void setSecondaryItem(Item secondaryItem) { this.secondaryItem = secondaryItem; }
    public User getInitiatorUser() { return initiatorUser; }
    public void setInitiatorUser(User initiatorUser) { this.initiatorUser = initiatorUser; }
    public User getCounterpartyUser() { return counterpartyUser; }
    public void setCounterpartyUser(User counterpartyUser) { this.counterpartyUser = counterpartyUser; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getMonetaryAmount() { return monetaryAmount; }
    public void setMonetaryAmount(BigDecimal monetaryAmount) { this.monetaryAmount = monetaryAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Boolean getReturnedOnTime() { return returnedOnTime; }
    public void setReturnedOnTime(Boolean returnedOnTime) { this.returnedOnTime = returnedOnTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
