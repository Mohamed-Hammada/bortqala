package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tender_bidders")
public class TenderBidder {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "tender_id", length = 36, nullable = false)
    private String tenderId;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(name = "bidder_name", nullable = false)
    private String bidderName;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private BidderStatus status;

    @Column(name = "invitation_date")
    private Long invitationDate;

    @Column(name = "submission_date")
    private Long submissionDate;

    @Column(name = "technical_score", precision = 5, scale = 2)
    private BigDecimal technicalScore;

    @Column(name = "financial_score", precision = 5, scale = 2)
    private BigDecimal financialScore;

    @Column(name = "combined_score", precision = 5, scale = 2)
    private BigDecimal combinedScore;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @Column(name = "total_bid_amount", precision = 18, scale = 2)
    private BigDecimal totalBidAmount;

    @Column(name = "bid_bond_received", nullable = false)
    private boolean bidBondReceived;

    @Column(name = "bid_bond_number", length = 100)
    private String bidBondNumber;

    @Column(name = "bid_bond_expiry_date")
    private LocalDate bidBondExpiryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected TenderBidder() {
    }

    public TenderBidder(String tenderId, String partyId, String bidderName,
                        String contactEmail, String contactPhone, String notes) {
        this.id = UUID.randomUUID().toString();
        this.tenderId = tenderId;
        this.partyId = partyId;
        this.bidderName = bidderName != null ? bidderName.strip() : "Bidder";
        this.contactEmail = contactEmail != null ? contactEmail.strip() : null;
        this.contactPhone = contactPhone != null ? contactPhone.strip() : null;
        this.status = BidderStatus.INVITED;
        this.invitationDate = System.currentTimeMillis();
        this.bidBondReceived = false;
        this.notes = notes != null ? notes.strip() : null;
        this.createdAt = this.invitationDate;
    }

    public void recordSubmission(BigDecimal totalAmount) {
        this.totalBidAmount = totalAmount;
        this.submissionDate = System.currentTimeMillis();
        this.status = BidderStatus.SUBMITTED;
    }

    public void recordEvaluation(BigDecimal techScore, BigDecimal finScore, BigDecimal combinedScore, int rankOrder) {
        this.technicalScore = techScore;
        this.financialScore = finScore;
        this.combinedScore = combinedScore;
        this.rankOrder = rankOrder;
    }

    public void updateBidBond(boolean received, String number, LocalDate expiryDate) {
        this.bidBondReceived = received;
        this.bidBondNumber = number != null ? number.strip() : null;
        this.bidBondExpiryDate = expiryDate;
    }

    public void disqualify(String reason) {
        this.status = BidderStatus.DISQUALIFIED;
        this.notes = (this.notes != null ? (this.notes + "\n") : "") + "Disqualified: " + reason;
    }
}
