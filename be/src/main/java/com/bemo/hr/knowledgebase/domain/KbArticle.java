package com.bemo.hr.knowledgebase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "kb_articles")
public class KbArticle {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 200)
    private String slug;
    @Column(name = "title_ar", nullable = false, length = 300)
    private String titleAr;
    @Column(name = "title_en", nullable = false, length = 300)
    private String titleEn;
    @Column(name = "body_ar", length = 20000)
    private String bodyAr;
    @Column(name = "body_en", length = 20000)
    private String bodyEn;
    @Column(length = 500)
    private String tags;
    @Column(nullable = false)
    private boolean published;
    @Column(nullable = false)
    private long views;
    @Column(name = "helpful_up", nullable = false)
    private long helpfulUp;
    @Column(name = "helpful_down", nullable = false)
    private long helpfulDown;
    @Column(name = "author_user_id", length = 100)
    private String authorUserId;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected KbArticle() {}

    public KbArticle(String appId, String slug, String titleAr, String titleEn,
                     String bodyAr, String bodyEn, String tags, String authorUserId) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.slug = slug;
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.bodyAr = bodyAr;
        this.bodyEn = bodyEn;
        this.tags = tags;
        this.authorUserId = authorUserId;
        this.published = false;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSlug() { return slug; }
    public String getTitleAr() { return titleAr; }
    public String getTitleEn() { return titleEn; }
    public String getBodyAr() { return bodyAr; }
    public String getBodyEn() { return bodyEn; }
    public String getTags() { return tags; }
    public boolean isPublished() { return published; }
    public long getViews() { return views; }
    public long getHelpfulUp() { return helpfulUp; }
    public long getHelpfulDown() { return helpfulDown; }
    public String getAuthorUserId() { return authorUserId; }
    public Long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }

    public void setTitleAr(String v) { this.titleAr = v; }
    public void setTitleEn(String v) { this.titleEn = v; }
    public void setBodyAr(String v) { this.bodyAr = v; }
    public void setBodyEn(String v) { this.bodyEn = v; }
    public void setTags(String v) { this.tags = v; }
    public void setPublished(boolean v) { this.published = v; }
    public void incrementViews() { this.views++; }
    public void incrementHelpfulUp() { this.helpfulUp++; }
    public void incrementHelpfulDown() { this.helpfulDown++; }
}
