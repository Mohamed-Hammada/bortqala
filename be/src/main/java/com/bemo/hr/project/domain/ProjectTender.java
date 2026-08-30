package com.bemo.hr.project.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_tenders")
public class ProjectTender {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "tender_number", length = 64, nullable = false)
    private String tenderNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "title_en")
    private String titleEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "tender_type", length = 30, nullable = false)
    private TenderType tenderType;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "client_party_id", length = 36)
    private String clientPartyId;

    @Column(name = "submission_deadline", nullable = false)
    private long submissionDeadline;

    @Column(name = "estimated_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal estimatedValue;

    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    @Column(name = "technical_weight_percent", nullable = false)
    private int technicalWeightPercent;

    @Column(name = "financial_weight_percent", nullable = false)
    private int financialWeightPercent;

    @Column(name = "bid_bond_required", nullable = false)
    private boolean bidBondRequired;

    @Column(name = "bid_bond_amount", precision = 18, scale = 2)
    private BigDecimal bidBondAmount;

    @Column(name = "bid_bond_validity_days")
    private Integer bidBondValidityDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private TenderStatus status;

    @Column(name = "awarded_bidder_id", length = 36)
    private String awardedBidderId;

    @Column(name = "awarded_amount", precision = 18, scale = 2)
    private BigDecimal awardedAmount;

    @Column(name = "awarded_at")
    private Long awardedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectTender() {
    }

    public ProjectTender(String tenderNumber, String title, String titleEn,
                         TenderType tenderType, String projectId, String clientPartyId,
                         long submissionDeadline, BigDecimal estimatedValue, String currencyCode,
                         int technicalWeightPercent, int financialWeightPercent,
                         boolean bidBondRequired, BigDecimal bidBondAmount, Integer bidBondValidityDays,
                         String notes) {
        this.id = UUID.randomUUID().toString();
        this.tenderNumber = tenderNumber != null ? tenderNumber.strip() : "TND";
        this.title = title != null ? title.strip() : "Tender";
        this.titleEn = titleEn != null ? titleEn.strip() : null;
        this.tenderType = tenderType != null ? tenderType : TenderType.EXTERNAL;
        this.projectId = projectId;
        this.clientPartyId = clientPartyId;
        this.submissionDeadline = submissionDeadline;
        this.estimatedValue = estimatedValue != null ? estimatedValue : BigDecimal.ZERO;
        this.currencyCode = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode.strip() : "EGP";
        this.technicalWeightPercent = technicalWeightPercent > 0 ? technicalWeightPercent : 70;
        this.financialWeightPercent = financialWeightPercent > 0 ? financialWeightPercent : 30;
        this.bidBondRequired = bidBondRequired;
        this.bidBondAmount = bidBondAmount != null ? bidBondAmount : BigDecimal.ZERO;
        this.bidBondValidityDays = bidBondValidityDays != null ? bidBondValidityDays : 90;
        this.status = TenderStatus.DRAFT;
        this.notes = notes != null ? notes.strip() : null;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDraft(String title, String titleEn, TenderType tenderType,
                            String projectId, String clientPartyId, long submissionDeadline,
                            BigDecimal estimatedValue, String currencyCode,
                            int technicalWeightPercent, int financialWeightPercent,
                            boolean bidBondRequired, BigDecimal bidBondAmount, Integer bidBondValidityDays,
                            String notes) {
        if (this.status != TenderStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_EDIT_NON_DRAFT_TENDER");
        }
        if (title != null && !title.isBlank()) this.title = title.strip();
        if (titleEn != null && !titleEn.isBlank()) this.titleEn = titleEn.strip();
        if (tenderType != null) this.tenderType = tenderType;
        this.projectId = projectId;
        this.clientPartyId = clientPartyId;
        this.submissionDeadline = submissionDeadline;
        if (estimatedValue != null) this.estimatedValue = estimatedValue;
        if (currencyCode != null && !currencyCode.isBlank()) this.currencyCode = currencyCode.strip();
        if (technicalWeightPercent > 0) this.technicalWeightPercent = technicalWeightPercent;
        if (financialWeightPercent > 0) this.financialWeightPercent = financialWeightPercent;
        this.bidBondRequired = bidBondRequired;
        if (bidBondAmount != null) this.bidBondAmount = bidBondAmount;
        if (bidBondValidityDays != null) this.bidBondValidityDays = bidBondValidityDays;
        this.notes = notes != null ? notes.strip() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    public void publish() {
        if (this.status != TenderStatus.DRAFT) {
            throw new BusinessRuleException("TENDER_ALREADY_PUBLISHED");
        }
        this.status = TenderStatus.PUBLISHED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void startEvaluation() {
        if (this.status != TenderStatus.PUBLISHED) {
            throw new BusinessRuleException("TENDER_NOT_PUBLISHED");
        }
        this.status = TenderStatus.EVALUATION;
        this.updatedAt = System.currentTimeMillis();
    }

    public void award(String bidderId, BigDecimal amount) {
        if (this.status != TenderStatus.EVALUATION && this.status != TenderStatus.PUBLISHED) {
            throw new BusinessRuleException("TENDER_NOT_IN_EVALUATION");
        }
        this.awardedBidderId = bidderId;
        this.awardedAmount = amount;
        this.awardedAt = System.currentTimeMillis();
        this.status = TenderStatus.AWARDED;
        this.updatedAt = this.awardedAt;
    }

    public void cancel() {
        if (this.status == TenderStatus.AWARDED) {
            throw new BusinessRuleException("CANNOT_CANCEL_AWARDED_TENDER");
        }
        this.status = TenderStatus.CANCELLED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateEstimatedValue(BigDecimal newEstimatedValue) {
        if (newEstimatedValue != null) {
            this.estimatedValue = newEstimatedValue;
            this.updatedAt = System.currentTimeMillis();
        }
    }
}
