package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_rooms")
@Getter
@Setter
@NoArgsConstructor
public class HospitalRoom {

    public enum Type {
        STANDARD, ICU, ISOLATION, VIP
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "ward_id", length = 36, nullable = false)
    private String wardId;

    @Column(name = "room_number", length = 30, nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 20, nullable = false)
    private Type roomType = Type.STANDARD;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalRoom(String wardId, String roomNumber, Type roomType) {
        this.id = UUID.randomUUID().toString();
        this.wardId = wardId;
        this.roomNumber = roomNumber;
        this.roomType = roomType != null ? roomType : Type.STANDARD;
        this.active = true;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }
}
