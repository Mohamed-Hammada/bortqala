package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.treasury.ChequeLayout;
import com.bemo.hr.finance.domain.treasury.CommercialCheque;
import com.bemo.hr.finance.infrastructure.ChequeLayoutRepository;
import com.bemo.hr.finance.infrastructure.CommercialChequeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.util.ArabicAmountInWords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChequePrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("ar", "EG"));
    private static final DecimalFormatSymbols ARABIC_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);
    private static final DecimalFormat DIGIT_FMT = new DecimalFormat("#,##0.00", ARABIC_SYMBOLS);

    private final CommercialChequeRepository chequeRepository;
    private final ChequeLayoutRepository layoutRepository;

    public ChequePrintService(CommercialChequeRepository chequeRepository,
                              ChequeLayoutRepository layoutRepository) {
        this.chequeRepository = chequeRepository;
        this.layoutRepository = layoutRepository;
    }

    public List<ChequeLayout> listLayouts() {
        return layoutRepository.findAllByActiveTrueOrderByBankCode();
    }

    public ChequeLayout getLayout(String bankCode) {
        return layoutRepository.findByBankCode(bankCode)
                .orElseThrow(() -> new BusinessRuleException(
                        "Cheque layout not found for bank: " + bankCode,
                        "CHEQUE_LAYOUT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    public ChequePrintData getPrintData(String chequeId) {
        CommercialCheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Cheque not found", "CHEQUE_NOT_FOUND", HttpStatus.NOT_FOUND));

        String bankCode = cheque.getBankName() != null ? cheque.getBankName().strip() : "DEFAULT";
        ChequeLayout layout = layoutRepository.findByBankCode(bankCode).orElse(null);

        String amountInWords = ArabicAmountInWords.convert(cheque.getAmount());
        String amountInDigits = DIGIT_FMT.format(cheque.getAmount());
        String issueDate = formatDate(cheque.getIssueDate());
        String dueDate = formatDate(cheque.getDueDate());

        return new ChequePrintData(
                cheque.getId(),
                cheque.getChequeNumber(),
                cheque.getDrawerPayeeName(),
                cheque.getBankName(),
                amountInWords,
                amountInDigits,
                cheque.getCurrency(),
                issueDate,
                dueDate,
                cheque.getStatus().name(),
                layout
        );
    }

    public String renderPrintView(String chequeId) {
        ChequePrintData data = getPrintData(chequeId);
        ChequeLayout layout = data.layout();

        int payeeX = layout != null ? layout.getPayeeX() : 50;
        int payeeY = layout != null ? layout.getPayeeY() : 80;
        int payeeWidth = layout != null ? layout.getPayeeWidth() : 400;
        int dateX = layout != null ? layout.getDateX() : 550;
        int dateY = layout != null ? layout.getDateY() : 30;
        int amountWordsX = layout != null ? layout.getAmountWordsX() : 50;
        int amountWordsY = layout != null ? layout.getAmountWordsY() : 130;
        int amountWordsWidth = layout != null ? layout.getAmountWordsWidth() : 450;
        int amountDigitsX = layout != null ? layout.getAmountDigitsX() : 520;
        int amountDigitsY = layout != null ? layout.getAmountDigitsY() : 130;
        int amountDigitsWidth = layout != null ? layout.getAmountDigitsWidth() : 120;
        int chequeNumberX = layout != null ? layout.getChequeNumberX() : 550;
        int chequeNumberY = layout != null ? layout.getChequeNumberY() : 80;
        boolean crossingLines = layout != null && layout.isCrossingLines();

        String bankName = data.bankName() != null ? data.bankName() : "";
        String safeBankName = escapeHtml(bankName);

        String crossingHtml = "";
        if (crossingLines) {
            crossingHtml = """
                <div style="position:absolute;top:40%%;left:10%%;width:80%%;border-top:2px solid #333;border-bottom:2px solid #333;padding:4px 0;text-align:center;font-size:11px;color:#666;">
                    %s
                </div>
                """.formatted(safeBankName);
        }

        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="UTF-8">
<title>%s</title>
<style>
@import url('https://fonts.googleapis.com/css2?family=Noto+Kufi+Arabic:wght@400;700&display=swap');
* { margin:0; padding:0; box-sizing:border-box; }
body { font-family:'Noto Kufi Arabic','Tahoma',sans-serif; background:#f5f5f5; display:flex; justify-content:center; padding:40px 20px; }
.cheque-container { position:relative; width:700px; height:300px; background:#fff; border:2px solid #333; border-radius:4px; padding:20px; box-shadow:2px 2px 8px rgba(0,0,0,0.1); }
.cheque-field { position:absolute; border:1px dashed #ccc; padding:4px 8px; font-size:14px; line-height:1.4; }
.cheque-label { font-size:10px; color:#999; display:block; margin-bottom:2px; }
.bank-header { position:absolute; top:10px; left:10px; font-size:12px; color:#666; }
.amount-digits-box { font-size:18px; font-weight:700; border:1px solid #333; border-radius:3px; text-align:center; direction:ltr; padding:4px 8px; }
.amount-words-box { font-size:14px; border-bottom:1px solid #333; padding-bottom:4px; }
.payee-box { font-size:16px; font-weight:700; border-bottom:2px solid #333; padding-bottom:4px; }
.date-box { font-size:13px; text-align:center; }
.cheque-number-box { font-size:14px; font-weight:700; text-align:center; direction:ltr; }
.watermark { position:absolute; bottom:10px; left:50%%; transform:translateX(-50%%); font-size:9px; color:#ccc; }
@media print {
  body { background:#fff; padding:0; }
  .cheque-container { box-shadow:none; border:1px solid #000; }
}
</style>
</head>
<body>
<div class="cheque-container">
  <div class="bank-header">%s</div>

  <div class="cheque-field cheque-number-box" style="top:%dpx;left:%dpx;">
    <span class="cheque-label">رقم الشيك</span>
    %s
  </div>

  <div class="cheque-field date-box" style="top:%dpx;left:%dpx;">
    <span class="cheque-label">التاريخ</span>
    %s
  </div>

  <div class="cheque-field payee-box" style="top:%dpx;left:%dpx;width:%dpx;">
    <span class="cheque-label">ادفع لأمر / استلم من</span>
    %s
  </div>

  <div class="cheque-field amount-words-box" style="top:%dpx;left:%dpx;width:%dpx;">
    <span class="cheque-label">المبلغ بالحروف</span>
    %s
  </div>

  <div class="cheque-field amount-digits-box" style="top:%dpx;left:%dpx;width:%dpx;">
    <span class="cheque-label">المبلغ بالأرقام</span>
    %s %s
  </div>

  %s

  <div class="watermark">Bemo ERP</div>
</div>
</body>
</html>
""".formatted(
                data.chequeNumber() + " — " + bankName,
                safeBankName,
                chequeNumberY, chequeNumberX, escapeHtml(data.chequeNumber()),
                dateY, dateX, data.issueDate(),
                payeeY, payeeX, payeeWidth, escapeHtml(data.drawerPayeeName()),
                amountWordsY, amountWordsX, amountWordsWidth, data.amountInWords(),
                amountDigitsY, amountDigitsX, amountDigitsWidth, data.amountInDigits(), data.currency(),
                crossingHtml
        );
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String formatDate(long epochMs) {
        if (epochMs <= 0) return "—";
        return DATE_FMT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Africa/Cairo")));
    }

    public record ChequePrintData(
            String chequeId,
            String chequeNumber,
            String drawerPayeeName,
            String bankName,
            String amountInWords,
            String amountInDigits,
            String currency,
            String issueDate,
            String dueDate,
            String status,
            ChequeLayout layout
    ) {}
}
