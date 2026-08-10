package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="ar_collection_tasks") @Getter
public class CollectionTask {
    public enum Status { OPEN, CONTACTED, PROMISED, CLOSED }
    @Id private String id;
    @TenantId @Column(name="app_id",nullable=false) private String appId;
    @Column(name="invoice_id",nullable=false,length=36) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(name="owner_user_id",length=100) private String ownerUserId;
    @Column(name="next_action_date") private LocalDate nextActionDate;
    @Column(length=500) private String note;
    @Version private long version;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected CollectionTask() { }
    public CollectionTask(String invoiceId){id=UUID.randomUUID().toString();this.invoiceId=invoiceId;status=Status.OPEN;}
    public void update(Status status,String owner,LocalDate next,String note){this.status=status;ownerUserId=blank(owner);nextActionDate=next;this.note=blank(note);}
    public void close(){status=Status.CLOSED;nextActionDate=null;}
    private static String blank(String value){return value==null||value.isBlank()?null:value.strip();}
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
}
