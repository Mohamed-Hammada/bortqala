package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Entity
@Table(name = "tender_clarifications")
public class TenderClarification {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "tender_id", length = 36, nullable = false)
    private String tenderId;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "asked_by_party_id", length = 36)
    private String askedByPartyId;

    @Column(name = "asked_at", nullable = false)
    private long askedAt;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_by_user_id", length = 36)
    private String answeredByUserId;

    @Column(name = "answered_at")
    private Long answeredAt;

    @Column(name = "is_public_addendum", nullable = false)
    private boolean publicAddendum;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected TenderClarification() {
    }

    public TenderClarification(String tenderId, String question, String askedByPartyId, boolean publicAddendum) {
        this.id = UUID.randomUUID().toString();
        this.tenderId = tenderId;
        this.question = question != null ? question.strip() : "";
        this.askedByPartyId = askedByPartyId;
        this.askedAt = System.currentTimeMillis();
        this.publicAddendum = publicAddendum;
        this.createdAt = this.askedAt;
    }

    public void provideAnswer(String answer, String answeredByUserId, boolean publicAddendum) {
        this.answer = answer != null ? answer.strip() : null;
        this.answeredByUserId = answeredByUserId;
        this.answeredAt = System.currentTimeMillis();
        this.publicAddendum = publicAddendum;
    }
}
