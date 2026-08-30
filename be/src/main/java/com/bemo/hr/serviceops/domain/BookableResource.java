package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "srv_bookable_resources")
@Getter
@Setter
public class BookableResource {

    public enum Kind {
        ROOM,
        TRAINER,
        EQUIPMENT,
        VEHICLE
    }

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Kind kind;

    private Integer capacity;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected BookableResource() {}

    public BookableResource(String appId, String code, String name, String nameEn,
                            Kind kind, Integer capacity, String location) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
        this.kind = kind != null ? kind : Kind.ROOM;
        this.capacity = capacity != null ? capacity : 1;
        this.location = location;
        this.active = true;
        this.version = 0L;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
