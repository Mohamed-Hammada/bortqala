package com.bemo.hr.finance.paylink.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_links")
public class PaymentLink {

    public enum Kind { INVOICE, PARTY_BALANCE, CUSTOM }
    public enum Status { PENDING, PAID, EXPIRED, CANCELLED }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 20)
    private String kind;
    @Column(name = "ref_id", length = 36)
    private String refId;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 36)
    private String token;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "gateway_ref", length = 200)
    private String gatewayRef;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "company_name", length = 255)
    private String companyName;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected PaymentLink() {}

    public PaymentLink(String appId, Kind kind, String refId, BigDecimal amount,
                       String description, String companyName, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.kind = kind.name();
        this.refId = refId;
        this.amount = amount;
        this.currency = "EGP";
        this.token = UUID.randomUUID().toString();
        this.status = Status.PENDING.name();
        this.description = description;
        this.companyName = companyName;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void confirm(String gatewayRef, Instant paidAt) {
        if (status != Status.PENDING.name())
            throw new BusinessRuleException("Link is not in PENDING status.",
                    "PAYLINK_INVALID_STATE", HttpStatus.CONFLICT);
        if (isExpired())
            throw new BusinessRuleException("This payment link has expired.",
                    "PAYLINK_EXPIRED", HttpStatus.GONE);
        this.status = Status.PAID.name();
        this.gatewayRef = gatewayRef;
        this.paidAt = paidAt;
    }

    public void cancel() {
        if (status != Status.PENDING.name())
            throw new BusinessRuleException("Only PENDING links can be cancelled.",
                    "PAYLINK_INVALID_STATE", HttpStatus.CONFLICT);
        this.status = Status.CANCELLED.name();
    }

    public void expire() {
        if (status == Status.PENDING.name()) this.status = Status.EXPIRED.name();
    }

    public boolean canReceivePayment() {
        return status == Status.PENDING.name() && !isExpired();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public Kind getKind() { return Kind.valueOf(kind); }
    public String getRefId() { return refId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getToken() { return token; }
    public Status getStatus() { return Status.valueOf(status); }
    public String getGatewayRef() { return gatewayRef; }
    public String getDescription() { return description; }
    public String getCompanyName() { return companyName; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public Long getVersion() { return version; }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
