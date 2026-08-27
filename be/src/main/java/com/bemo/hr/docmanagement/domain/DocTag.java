package com.bemo.hr.docmanagement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "doc_tags")
public class DocTag {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color", length = 20)
    private String color;

    protected DocTag() {
    }

    public DocTag(String name, String color) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.color = color;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
