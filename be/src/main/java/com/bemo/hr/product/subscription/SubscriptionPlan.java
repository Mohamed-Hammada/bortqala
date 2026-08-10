package com.bemo.hr.product.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;

@Entity @Table(name="subscription_plans") @Getter
public class SubscriptionPlan {
    @Id @Column(name="plan_code",length=40) private String code;
    @Column(name="name_ar",nullable=false,length=120) private String nameAr;
    @Column(name="name_en",nullable=false,length=120) private String nameEn;
    @Column(name="feature_keys_json",nullable=false,length=4000) private String featureKeysJson;
    @Column(name="limits_json",nullable=false,length=2000) private String limitsJson;
    @Column(nullable=false) private boolean active;
    @Version private long version;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected SubscriptionPlan() {}
    public SubscriptionPlan(String code,String nameAr,String nameEn,String features,String limits){this.code=code;update(nameAr,nameEn,features,limits,true);}
    public void update(String nameAr,String nameEn,String features,String limits,boolean active){this.nameAr=nameAr.strip();this.nameEn=nameEn.strip();featureKeysJson=features;limitsJson=limits;this.active=active;updatedAt=Instant.now();}
}
