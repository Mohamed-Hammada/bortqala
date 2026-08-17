package com.bemo.hr.finance.domain.posting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "posting_profile_lines")
public class PostingProfileLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(name = "profile_id", nullable = false, length = 36)
    private String profileId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(nullable = false, length = 10)
    private String side; // DEBIT or CREDIT

    @Column(name = "account_source", nullable = false, length = 50)
    private String accountSource;

    @Column(name = "fixed_account_id", length = 36)
    private String fixedAccountId;

    @Column(name = "amount_source", nullable = false, length = 50)
    private String amountSource;

    protected PostingProfileLine() {
    }

    public PostingProfileLine(String profileId, int lineNo, String side, String accountSource, String fixedAccountId, String amountSource) {
        this.id = UUID.randomUUID().toString();
        this.profileId = profileId;
        this.lineNo = lineNo;
        this.side = side;
        this.accountSource = accountSource;
        this.fixedAccountId = fixedAccountId;
        this.amountSource = amountSource;
    }

    public String getId() {
        return id;
    }

    public String getProfileId() {
        return profileId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public String getSide() {
        return side;
    }

    public String getAccountSource() {
        return accountSource;
    }

    public String getFixedAccountId() {
        return fixedAccountId;
    }

    public String getAmountSource() {
        return amountSource;
    }
}
