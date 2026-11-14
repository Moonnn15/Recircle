package com.recircle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trust_score_history")
public class TrustScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "previous_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal previousScore;

    @Column(name = "new_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal newScore;

    @Column(name = "delta", nullable = false, precision = 5, scale = 2)
    private BigDecimal delta;

    @Column(name = "trigger_event", nullable = false, length = 80)
    private String triggerEvent;

    @Column(name = "reason_summary", nullable = false, length = 300)
    private String reasonSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getPreviousScore() { return previousScore; }
    public void setPreviousScore(BigDecimal previousScore) { this.previousScore = previousScore; }
    public BigDecimal getNewScore() { return newScore; }
    public void setNewScore(BigDecimal newScore) { this.newScore = newScore; }
    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    public String getReasonSummary() { return reasonSummary; }
    public void setReasonSummary(String reasonSummary) { this.reasonSummary = reasonSummary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
