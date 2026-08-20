package com.bemo.hr.trade.pos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pos_sessions")
public class PosSession {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "session_number", nullable = false, length = 50)
    private String sessionNumber;

    @Column(name = "terminal_id", nullable = false, length = 36)
    private String terminalId;

    @Column(name = "cashier_user_id", nullable = false, length = 36)
    private String cashierUserId;

    @Column(name = "opened_at", nullable = false)
    private long openedAt;

    @Column(name = "closed_at")
    private Long closedAt;

    @Column(name = "opening_float", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingFloat;

    @Column(name = "closing_actual_cash", precision = 15, scale = 2)
    private BigDecimal closingActualCash;

    @Column(name = "closing_calculated_cash", precision = 15, scale = 2)
    private BigDecimal closingCalculatedCash;

    @Column(name = "closing_actual_card", precision = 15, scale = 2)
    private BigDecimal closingActualCard;

    @Column(name = "closing_calculated_card", precision = 15, scale = 2)
    private BigDecimal closingCalculatedCard;

    @Column(name = "cash_variance", precision = 15, scale = 2)
    private BigDecimal cashVariance;

    @Column(name = "card_variance", precision = 15, scale = 2)
    private BigDecimal cardVariance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PosSessionStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PosSession() {
    }

    public PosSession(String sessionNumber, String terminalId, String cashierUserId, BigDecimal openingFloat) {
        this.id = UUID.randomUUID().toString();
        this.sessionNumber = sessionNumber;
        this.terminalId = terminalId;
        this.cashierUserId = cashierUserId;
        this.openedAt = System.currentTimeMillis();
        this.openingFloat = openingFloat != null ? openingFloat : BigDecimal.ZERO;
        this.closingCalculatedCash = this.openingFloat;
        this.closingCalculatedCard = BigDecimal.ZERO;
        this.status = PosSessionStatus.OPEN;
    }

    public void addSaleTotals(BigDecimal cashAmount, BigDecimal cardAmount) {
        if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.closingCalculatedCash = this.closingCalculatedCash.add(cashAmount);
        }
        if (cardAmount != null && cardAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.closingCalculatedCard = this.closingCalculatedCard.add(cardAmount);
        }
    }

    public void addReturnTotals(BigDecimal cashAmount, BigDecimal cardAmount) {
        if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.closingCalculatedCash = this.closingCalculatedCash.subtract(cashAmount);
        }
        if (cardAmount != null && cardAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.closingCalculatedCard = this.closingCalculatedCard.subtract(cardAmount);
        }
    }

    public void close(BigDecimal actualCash, BigDecimal actualCard, String notes) {
        this.closedAt = System.currentTimeMillis();
        this.closingActualCash = actualCash != null ? actualCash : BigDecimal.ZERO;
        this.closingActualCard = actualCard != null ? actualCard : BigDecimal.ZERO;
        this.cashVariance = this.closingActualCash.subtract(this.closingCalculatedCash);
        this.cardVariance = this.closingActualCard.subtract(this.closingCalculatedCard);
        this.status = PosSessionStatus.CLOSED;
        this.notes = notes;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getSessionNumber() {
        return sessionNumber;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getCashierUserId() {
        return cashierUserId;
    }

    public long getOpenedAt() {
        return openedAt;
    }

    public Long getClosedAt() {
        return closedAt;
    }

    public BigDecimal getOpeningFloat() {
        return openingFloat;
    }

    public BigDecimal getClosingActualCash() {
        return closingActualCash;
    }

    public BigDecimal getClosingCalculatedCash() {
        return closingCalculatedCash;
    }

    public BigDecimal getClosingActualCard() {
        return closingActualCard;
    }

    public BigDecimal getClosingCalculatedCard() {
        return closingCalculatedCard;
    }

    public BigDecimal getCashVariance() {
        return cashVariance;
    }

    public BigDecimal getCardVariance() {
        return cardVariance;
    }

    public PosSessionStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
