package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "exam_answers")
@Getter
@Setter
@NoArgsConstructor
public class ExamAnswer {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "visit_id", length = 36, nullable = false)
    private String visitId;

    @Column(name = "template_id", length = 36, nullable = false)
    private String templateId;

    @Column(name = "answers_json", length = 4000, nullable = false)
    private String answersJson;

    @Column(name = "recorded_at", nullable = false)
    private Long recordedAt;

    @Column(name = "recorded_by", length = 160)
    private String recordedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ExamAnswer(String visitId, String templateId, String answersJson, String recordedBy) {
        this.id = UUID.randomUUID().toString();
        this.visitId = visitId;
        this.templateId = templateId;
        this.answersJson = answersJson;
        this.recordedBy = recordedBy;
        this.recordedAt = System.currentTimeMillis();
    }
}
