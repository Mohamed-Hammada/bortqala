package com.bemo.hr.esign.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "signature_steps")
public class SignatureStep {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "packet_id", nullable = false)
    private String packetId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "signer_name", nullable = false, length = 200)
    private String signerName;

    @Column(name = "signer_user_id", length = 100)
    private String signerUserId;

    @Column(name = "role_label", length = 100)
    private String roleLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StepStatus status;

    @Column(name = "signed_at")
    private Long signedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "content_sha256", length = 100)
    private String contentSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30)
    private SignatureMethod method;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    @Version
    @Column(nullable = false)
    private long version;

    protected SignatureStep() {
    }

    public SignatureStep(String packetId, int stepOrder, String signerName,
                         String signerUserId, String roleLabel) {
        this.id = UUID.randomUUID().toString();
        this.packetId = packetId;
        this.stepOrder = stepOrder;
        this.signerName = signerName;
        this.signerUserId = signerUserId;
        this.roleLabel = roleLabel;
        this.status = StepStatus.PENDING;
    }

    public void sign(String contentSha256, SignatureMethod method, String ipAddress) {
        this.status = StepStatus.SIGNED;
        this.signedAt = System.currentTimeMillis();
        this.contentSha256 = contentSha256;
        this.method = method;
        this.ipAddress = ipAddress;
    }

    public void decline(String reason) {
        this.status = StepStatus.DECLINED;
        this.declineReason = reason;
        this.signedAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() {
        // idempotent
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getPacketId() { return packetId; }
    public int getStepOrder() { return stepOrder; }
    public String getSignerName() { return signerName; }
    public String getSignerUserId() { return signerUserId; }
    public String getRoleLabel() { return roleLabel; }
    public StepStatus getStatus() { return status; }
    public Long getSignedAt() { return signedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getContentSha256() { return contentSha256; }
    public SignatureMethod getMethod() { return method; }
    public String getDeclineReason() { return declineReason; }
    public long getVersion() { return version; }
}
