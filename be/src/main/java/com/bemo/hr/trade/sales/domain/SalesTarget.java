package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_targets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_id", "scope", "target_ref_id", "period"})
})
@Getter
@NoArgsConstructor
public class SalesTarget {

    public enum Scope { REP, TEAM, BRANCH }
    public enum Metric { REVENUE, QUANTITY }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Scope scope;

    @Column(name = "target_ref_id", nullable = false, length = 36)
    private String targetRefId;

    @Column(nullable = false, length = 7)
    private String period; // YYYY-MM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Metric metric;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetValue;

    @Version
    private Long version;

    public SalesTarget(String id, String appId, Scope scope, String targetRefId,
                       String period, Metric metric, BigDecimal targetValue) {
        this.id = id;
        this.appId = appId;
        this.scope = scope;
        this.targetRefId = targetRefId;
        this.period = period;
        this.metric = metric;
        this.targetValue = targetValue;
    }
}
