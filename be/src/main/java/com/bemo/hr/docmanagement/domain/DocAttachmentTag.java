package com.bemo.hr.docmanagement.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "doc_attachment_tags",
       uniqueConstraints = @UniqueConstraint(columnNames = {"attachment_id", "tag_id"}))
public class DocAttachmentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attachment_id", nullable = false)
    private String attachmentId;

    @Column(name = "tag_id", nullable = false)
    private String tagId;

    public DocAttachmentTag() {
    }

    public DocAttachmentTag(String attachmentId, String tagId) {
        this.attachmentId = attachmentId;
        this.tagId = tagId;
    }

    public Long getId() { return id; }
    public String getAttachmentId() { return attachmentId; }
    public String getTagId() { return tagId; }
}
