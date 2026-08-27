package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "cheque_layouts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_id", "bank_code"})
})
@Getter
public class ChequeLayout {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "payee_x", nullable = false)
    private int payeeX;

    @Column(name = "payee_y", nullable = false)
    private int payeeY;

    @Column(name = "payee_width", nullable = false)
    private int payeeWidth;

    @Column(name = "date_x", nullable = false)
    private int dateX;

    @Column(name = "date_y", nullable = false)
    private int dateY;

    @Column(name = "amount_words_x", nullable = false)
    private int amountWordsX;

    @Column(name = "amount_words_y", nullable = false)
    private int amountWordsY;

    @Column(name = "amount_words_width", nullable = false)
    private int amountWordsWidth;

    @Column(name = "amount_digits_x", nullable = false)
    private int amountDigitsX;

    @Column(name = "amount_digits_y", nullable = false)
    private int amountDigitsY;

    @Column(name = "amount_digits_width", nullable = false)
    private int amountDigitsWidth;

    @Column(name = "cheque_number_x", nullable = false)
    private int chequeNumberX;

    @Column(name = "cheque_number_y", nullable = false)
    private int chequeNumberY;

    @Column(name = "crossing_lines", nullable = false)
    private boolean crossingLines;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected ChequeLayout() {}

    public ChequeLayout(String bankCode, String bankName,
                        int payeeX, int payeeY, int payeeWidth,
                        int dateX, int dateY,
                        int amountWordsX, int amountWordsY, int amountWordsWidth,
                        int amountDigitsX, int amountDigitsY, int amountDigitsWidth,
                        int chequeNumberX, int chequeNumberY,
                        boolean crossingLines) {
        this.id = UUID.randomUUID().toString();
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.payeeX = payeeX;
        this.payeeY = payeeY;
        this.payeeWidth = payeeWidth;
        this.dateX = dateX;
        this.dateY = dateY;
        this.amountWordsX = amountWordsX;
        this.amountWordsY = amountWordsY;
        this.amountWordsWidth = amountWordsWidth;
        this.amountDigitsX = amountDigitsX;
        this.amountDigitsY = amountDigitsY;
        this.amountDigitsWidth = amountDigitsWidth;
        this.chequeNumberX = chequeNumberX;
        this.chequeNumberY = chequeNumberY;
        this.crossingLines = crossingLines;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public void update(int payeeX, int payeeY, int payeeWidth,
                       int dateX, int dateY,
                       int amountWordsX, int amountWordsY, int amountWordsWidth,
                       int amountDigitsX, int amountDigitsY, int amountDigitsWidth,
                       int chequeNumberX, int chequeNumberY,
                       boolean crossingLines) {
        this.payeeX = payeeX;
        this.payeeY = payeeY;
        this.payeeWidth = payeeWidth;
        this.dateX = dateX;
        this.dateY = dateY;
        this.amountWordsX = amountWordsX;
        this.amountWordsY = amountWordsY;
        this.amountWordsWidth = amountWordsWidth;
        this.amountDigitsX = amountDigitsX;
        this.amountDigitsY = amountDigitsY;
        this.amountDigitsWidth = amountDigitsWidth;
        this.chequeNumberX = chequeNumberX;
        this.chequeNumberY = chequeNumberY;
        this.crossingLines = crossingLines;
        this.updatedAt = System.currentTimeMillis();
    }
}
