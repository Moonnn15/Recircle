package com.recircle.entity;

import com.recircle.enums.ExchangeStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "exchange_offers")
public class ExchangeOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_item_id", nullable = false)
    private Item requestedItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offered_item_id", nullable = false)
    private Item offeredItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "proposal_message", nullable = false, length = 600)
    private String proposalMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExchangeStatus status = ExchangeStatus.PENDING;

    @Column(name = "proposer_handover_confirmed", nullable = false)
    private boolean proposerHandoverConfirmed = false;

    @Column(name = "receiver_handover_confirmed", nullable = false)
    private boolean receiverHandoverConfirmed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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
    public Item getRequestedItem() { return requestedItem; }
    public void setRequestedItem(Item requestedItem) { this.requestedItem = requestedItem; }
    public Item getOfferedItem() { return offeredItem; }
    public void setOfferedItem(Item offeredItem) { this.offeredItem = offeredItem; }
    public User getProposer() { return proposer; }
    public void setProposer(User proposer) { this.proposer = proposer; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public String getProposalMessage() { return proposalMessage; }
    public void setProposalMessage(String proposalMessage) { this.proposalMessage = proposalMessage; }
    public ExchangeStatus getStatus() { return status; }
    public void setStatus(ExchangeStatus status) { this.status = status; }
    public boolean isProposerHandoverConfirmed() { return proposerHandoverConfirmed; }
    public void setProposerHandoverConfirmed(boolean proposerHandoverConfirmed) { this.proposerHandoverConfirmed = proposerHandoverConfirmed; }
    public boolean isReceiverHandoverConfirmed() { return receiverHandoverConfirmed; }
    public void setReceiverHandoverConfirmed(boolean receiverHandoverConfirmed) { this.receiverHandoverConfirmed = receiverHandoverConfirmed; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
