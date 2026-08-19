package com.bemo.hr.project.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_progress_claims")
public class ProjectProgressClaim {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "claim_number", length = 64, nullable = false)
    private String claimNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", length = 30, nullable = false)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_kind", length = 30, nullable = false)
    private ClaimKind claimKind;

    @Column(name = "claim_sequence_number", nullable = false)
    private int claimSequenceNumber;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "submission_date")
    private Long submissionDate;

    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    @Column(name = "previous_gross_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal previousGrossAmount;

    @Column(name = "current_gross_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentGrossAmount;

    @Column(name = "cumulative_gross_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal cumulativeGrossAmount;

    @Column(name = "previous_retention_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal previousRetentionAmount;

    @Column(name = "current_retention_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentRetentionAmount;

    @Column(name = "cumulative_retention_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal cumulativeRetentionAmount;

    @Column(name = "previous_advance_recovery_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal previousAdvanceRecoveryAmount;

    @Column(name = "current_advance_recovery_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentAdvanceRecoveryAmount;

    @Column(name = "cumulative_advance_recovery_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal cumulativeAdvanceRecoveryAmount;

    @Column(name = "current_tax_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentTaxAmount;

    @Column(name = "current_deductions_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentDeductionsAmount;

    @Column(name = "current_net_payable_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentNetPayableAmount;

    @Column(name = "cumulative_net_paid_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal cumulativeNetPaidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ClaimStatus status;

    @Column(name = "certified_by_user_id", length = 36)
    private String certifiedByUserId;

    @Column(name = "certified_at")
    private Long certifiedAt;

    @Column(name = "certification_notes", columnDefinition = "TEXT")
    private String certificationNotes;

    @Column(name = "posted_finance_journal_id", length = 36)
    private String postedFinanceJournalId;

    @Column(name = "posted_invoice_id", length = 36)
    private String postedInvoiceId;

    @Column(name = "posted_at")
    private Long postedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectProgressClaim() {
    }

    public ProjectProgressClaim(String claimNumber, ClaimType claimType, ClaimKind claimKind,
                                int claimSequenceNumber, String projectId, String partyId,
                                LocalDate periodStartDate, LocalDate periodEndDate,
                                String currencyCode, String notes) {
        this.id = UUID.randomUUID().toString();
        this.claimNumber = claimNumber != null ? claimNumber.strip() : "IPC";
        this.claimType = claimType != null ? claimType : ClaimType.OWNER_IPC;
        this.claimKind = claimKind != null ? claimKind : ClaimKind.INTERIM;
        this.claimSequenceNumber = claimSequenceNumber > 0 ? claimSequenceNumber : 1;
        this.projectId = projectId;
        this.partyId = partyId;
        this.periodStartDate = periodStartDate != null ? periodStartDate : LocalDate.now().minusMonths(1);
        this.periodEndDate = periodEndDate != null ? periodEndDate : LocalDate.now();
        this.currencyCode = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode.strip() : "EGP";
        this.previousGrossAmount = BigDecimal.ZERO;
        this.currentGrossAmount = BigDecimal.ZERO;
        this.cumulativeGrossAmount = BigDecimal.ZERO;
        this.previousRetentionAmount = BigDecimal.ZERO;
        this.currentRetentionAmount = BigDecimal.ZERO;
        this.cumulativeRetentionAmount = BigDecimal.ZERO;
        this.previousAdvanceRecoveryAmount = BigDecimal.ZERO;
        this.currentAdvanceRecoveryAmount = BigDecimal.ZERO;
        this.cumulativeAdvanceRecoveryAmount = BigDecimal.ZERO;
        this.currentTaxAmount = BigDecimal.ZERO;
        this.currentDeductionsAmount = BigDecimal.ZERO;
        this.currentNetPayableAmount = BigDecimal.ZERO;
        this.cumulativeNetPaidAmount = BigDecimal.ZERO;
        this.status = ClaimStatus.DRAFT;
        this.notes = notes != null ? notes.strip() : null;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDraft(ClaimKind claimKind, String partyId, LocalDate periodStartDate,
                            LocalDate periodEndDate, String currencyCode, String notes) {
        if (this.status != ClaimStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_EDIT_NON_DRAFT_CLAIM");
        }
        if (claimKind != null) this.claimKind = claimKind;
        this.partyId = partyId;
        if (periodStartDate != null) this.periodStartDate = periodStartDate;
        if (periodEndDate != null) this.periodEndDate = periodEndDate;
        if (currencyCode != null && !currencyCode.isBlank()) this.currencyCode = currencyCode.strip();
        this.notes = notes != null ? notes.strip() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateTotals(BigDecimal prevGross, BigDecimal currGross, BigDecimal cumGross,
                             BigDecimal prevRet, BigDecimal currRet, BigDecimal cumRet,
                             BigDecimal prevAdv, BigDecimal currAdv, BigDecimal cumAdv,
                             BigDecimal taxAmt, BigDecimal dedAmt, BigDecimal netPayable) {
        if (this.status != ClaimStatus.DRAFT && this.status != ClaimStatus.SUBMITTED && this.status != ClaimStatus.REVIEWED) {
            throw new BusinessRuleException("CLAIM_LOCKED_CANNOT_UPDATE_TOTALS");
        }
        this.previousGrossAmount = prevGross != null ? prevGross : BigDecimal.ZERO;
        this.currentGrossAmount = currGross != null ? currGross : BigDecimal.ZERO;
        this.cumulativeGrossAmount = cumGross != null ? cumGross : BigDecimal.ZERO;
        this.previousRetentionAmount = prevRet != null ? prevRet : BigDecimal.ZERO;
        this.currentRetentionAmount = currRet != null ? currRet : BigDecimal.ZERO;
        this.cumulativeRetentionAmount = cumRet != null ? cumRet : BigDecimal.ZERO;
        this.previousAdvanceRecoveryAmount = prevAdv != null ? prevAdv : BigDecimal.ZERO;
        this.currentAdvanceRecoveryAmount = currAdv != null ? currAdv : BigDecimal.ZERO;
        this.cumulativeAdvanceRecoveryAmount = cumAdv != null ? cumAdv : BigDecimal.ZERO;
        this.currentTaxAmount = taxAmt != null ? taxAmt : BigDecimal.ZERO;
        this.currentDeductionsAmount = dedAmt != null ? dedAmt : BigDecimal.ZERO;
        this.currentNetPayableAmount = netPayable != null ? netPayable : BigDecimal.ZERO;
        this.updatedAt = System.currentTimeMillis();
    }

    public void submit() {
        if (this.status != ClaimStatus.DRAFT) {
            throw new BusinessRuleException("CLAIM_ALREADY_SUBMITTED");
        }
        this.status = ClaimStatus.SUBMITTED;
        this.submissionDate = System.currentTimeMillis();
        this.updatedAt = this.submissionDate;
    }

    public void review() {
        if (this.status != ClaimStatus.SUBMITTED) {
            throw new BusinessRuleException("CLAIM_NOT_IN_SUBMITTED_STATE");
        }
        this.status = ClaimStatus.REVIEWED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void certify(String userId, String notes) {
        if (this.status != ClaimStatus.REVIEWED && this.status != ClaimStatus.SUBMITTED) {
            throw new BusinessRuleException("CLAIM_NOT_READY_FOR_CERTIFICATION");
        }
        this.status = ClaimStatus.CERTIFIED;
        this.certifiedByUserId = userId;
        this.certifiedAt = System.currentTimeMillis();
        this.certificationNotes = notes != null ? notes.strip() : null;
        this.updatedAt = this.certifiedAt;
    }

    public void markPostedFinance(String journalId, String invoiceId) {
        if (this.status != ClaimStatus.CERTIFIED) {
            throw new BusinessRuleException("CLAIM_NOT_CERTIFIED");
        }
        this.status = ClaimStatus.POSTED_FINANCE;
        this.postedFinanceJournalId = journalId;
        this.postedInvoiceId = invoiceId;
        this.postedAt = System.currentTimeMillis();
        this.updatedAt = this.postedAt;
    }

    public void markPaid() {
        if (this.status != ClaimStatus.POSTED_FINANCE) {
            throw new BusinessRuleException("CLAIM_NOT_POSTED_TO_FINANCE");
        }
        this.status = ClaimStatus.PAID;
        this.cumulativeNetPaidAmount = this.cumulativeNetPaidAmount.add(this.currentNetPayableAmount);
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel() {
        if (this.status == ClaimStatus.POSTED_FINANCE || this.status == ClaimStatus.PAID) {
            throw new BusinessRuleException("CANNOT_CANCEL_POSTED_OR_PAID_CLAIM");
        }
        this.status = ClaimStatus.CANCELLED;
        this.updatedAt = System.currentTimeMillis();
    }
}
