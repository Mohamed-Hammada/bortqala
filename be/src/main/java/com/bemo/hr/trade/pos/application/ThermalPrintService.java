package com.bemo.hr.trade.pos.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.pos.api.ThermalPrinterApi;
import com.bemo.hr.trade.pos.domain.*;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import com.bemo.hr.trade.pos.infrastructure.ThermalPrinterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class ThermalPrintService {

    private final ThermalPrinterRepository printerRepository;
    private final PosTransactionRepository transactionRepository;
    private final AuditService auditService;

    public ThermalPrintService(ThermalPrinterRepository printerRepository,
                               PosTransactionRepository transactionRepository,
                               AuditService auditService) {
        this.printerRepository = printerRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ThermalPrinterApi.PrinterResponse> listPrinters() {
        return printerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThermalPrinterApi.PrinterResponse getPrinter(String id) {
        return printerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessRuleException("Thermal printer not found", "THERMAL_PRINTER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ThermalPrinterApi.PrinterResponse savePrinter(ThermalPrinterApi.SavePrinterRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new BusinessRuleException("Printer name is required", "THERMAL_PRINTER_NAME_REQUIRED");
        }

        // If marked as default, unset other default printers
        if (request.isDefault()) {
            printerRepository.findByActiveTrue().stream()
                    .filter(ThermalPrinter::isDefault)
                    .forEach(p -> p.markDefault(false));
        }

        ThermalPrinter printer;
        if (request.id() != null && !request.id().trim().isEmpty()) {
            printer = printerRepository.findById(request.id())
                    .orElseThrow(() -> new BusinessRuleException("Thermal printer not found", "THERMAL_PRINTER_NOT_FOUND", HttpStatus.NOT_FOUND));
            printer.update(
                    request.name().trim(),
                    request.branchId(),
                    request.terminalId(),
                    request.connectionType() != null ? request.connectionType() : ThermalPrinterConnectionType.NETWORK,
                    request.ipAddress(),
                    request.port() != null ? request.port() : 9100,
                    request.bluetoothMac(),
                    request.paperWidth() != null ? request.paperWidth() : ThermalPaperWidth.MM_80,
                    request.characterCodePage() != null ? request.characterCodePage() : "CP864",
                    request.headerText(),
                    request.footerText(),
                    request.openDrawer(),
                    request.cutPaper(),
                    request.printQrCode(),
                    request.isDefault(),
                    request.active()
            );
        } else {
            printer = new ThermalPrinter(
                    request.name().trim(),
                    request.branchId(),
                    request.terminalId(),
                    request.connectionType() != null ? request.connectionType() : ThermalPrinterConnectionType.NETWORK,
                    request.ipAddress(),
                    request.port() != null ? request.port() : 9100,
                    request.bluetoothMac(),
                    request.paperWidth() != null ? request.paperWidth() : ThermalPaperWidth.MM_80,
                    request.characterCodePage() != null ? request.characterCodePage() : "CP864",
                    request.headerText(),
                    request.footerText(),
                    request.openDrawer(),
                    request.cutPaper(),
                    request.printQrCode(),
                    request.isDefault()
            );
        }

        ThermalPrinter saved = printerRepository.save(printer);
        return toResponse(saved);
    }

    @Transactional
    public void deletePrinter(String id) {
        if (!printerRepository.existsById(id)) {
            throw new BusinessRuleException("Thermal printer not found", "THERMAL_PRINTER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        printerRepository.deleteById(id);
    }

    public ThermalPrinterApi.TestPrintResponse testPrint(String printerId) {
        ThermalPrinter printer = resolvePrinter(printerId, null, null);
        EscPosCommandBuilder builder = createBuilder(printer);

        builder.initialize()
                .setCodePage(printer.getCharacterCodePage())
                .align(EscPosCommandBuilder.Alignment.CENTER)
                .fontSize(EscPosCommandBuilder.FontSize.DOUBLE_BOTH)
                .bold(true)
                .line("BEMO ERP")
                .fontSize(EscPosCommandBuilder.FontSize.NORMAL)
                .bold(false)
                .line("ESC/POS Thermal Printer Test")
                .separator('=')
                .align(EscPosCommandBuilder.Alignment.LEFT)
                .rowTwoColumns("Printer:", printer.getName())
                .rowTwoColumns("Type:", printer.getConnectionType().name())
                .rowTwoColumns("Width:", printer.getPaperWidth().name() + " (" + printer.getPaperWidth().getColumns() + " col)")
                .rowTwoColumns("Date:", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()))
                .separator('-')
                .rowThreeColumns("Item", "Qty", "Price")
                .separator('-')
                .rowThreeColumns("Sample Product 1", "2", "150.00")
                .rowThreeColumns("Sample Product 2", "1", "75.50")
                .separator('-')
                .rowTwoColumns("TOTAL:", "225.50 EGP")
                .separator('=')
                .qrCode("https://bemo.cloud/verify-printer?id=" + printer.getId())
                .barcode128("TEST-123456")
                .align(EscPosCommandBuilder.Alignment.CENTER)
                .line("Printer Test Successful ✓")
                .line("جاهز للعمل والطباعة المباشرة");

        if (printer.isOpenDrawer()) {
            builder.kickDrawer();
        }
        if (printer.isCutPaper()) {
            builder.cutPaper();
        }

        byte[] bytes = builder.toByteArray();
        boolean sent = false;
        String message = "Print payload generated";

        if (printer.getConnectionType() == ThermalPrinterConnectionType.NETWORK && printer.getIpAddress() != null) {
            sent = dispatchToNetworkPrinter(printer.getIpAddress(), printer.getPort(), bytes);
            message = sent ? "Test ticket sent to network printer" : "Network printer connection timeout, payload ready for client print";
        }

        return new ThermalPrinterApi.TestPrintResponse(
                printer.getId(),
                printer.getName(),
                builder.toBase64(),
                sent,
                message
        );
    }

    @Transactional(readOnly = true)
    public ThermalPrinterApi.ReceiptPrintDataResponse generateReceiptBytes(String transactionId, String printerId, boolean isReprint) {
        PosTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transaction not found", "POS_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        ThermalPrinter printer = resolvePrinter(printerId, transaction.getTerminalId(), null);
        EscPosCommandBuilder builder = createBuilder(printer);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String formattedDate = sdf.format(new Date(transaction.getCreatedAt()));

        builder.initialize()
                .setCodePage(printer.getCharacterCodePage())
                .align(EscPosCommandBuilder.Alignment.CENTER);

        // Header
        if (printer.getHeaderText() != null && !printer.getHeaderText().trim().isEmpty()) {
            builder.line(printer.getHeaderText().trim());
        } else {
            builder.fontSize(EscPosCommandBuilder.FontSize.DOUBLE_BOTH)
                    .bold(true)
                    .line("BEMO POS")
                    .fontSize(EscPosCommandBuilder.FontSize.NORMAL)
                    .bold(false);
        }

        // Duplicate reprint banner
        if (isReprint || transaction.getReprintCount() > 0) {
            builder.separator('*')
                    .bold(true)
                    .line("*** DUPLICATE / نسخة إضافية ***")
                    .line("Reprint Count: " + transaction.getReprintCount())
                    .bold(false)
                    .separator('*');
        }

        builder.separator('=')
                .align(EscPosCommandBuilder.Alignment.LEFT)
                .rowTwoColumns("Receipt #:", transaction.getTransactionNumber())
                .rowTwoColumns("Date:", formattedDate)
                .rowTwoColumns("Cashier:", transaction.getCashierUserId())
                .rowTwoColumns("Terminal:", transaction.getTerminalId())
                .separator('-');

        // Lines header
        builder.rowThreeColumns("Item", "Qty", "Total")
                .separator('-');

        // Line items
        if (transaction.getLines() != null) {
            for (PosTransactionLine line : transaction.getLines()) {
                String name = line.getItemName();
                String qty = line.getQuantity() != null ? line.getQuantity().stripTrailingZeros().toPlainString() : "1";
                String total = line.getLineTotal() != null ? line.getLineTotal().toPlainString() : "0.00";
                builder.rowThreeColumns(name, qty, total);
            }
        }

        builder.separator('-')
                .rowTwoColumns("Subtotal:", transaction.getSubtotal() != null ? transaction.getSubtotal().toPlainString() : "0.00");

        if (transaction.getDiscountAmount() != null && transaction.getDiscountAmount().signum() > 0) {
            builder.rowTwoColumns("Discount:", "-" + transaction.getDiscountAmount().toPlainString());
        }

        if (transaction.getTaxAmount() != null && transaction.getTaxAmount().signum() > 0) {
            builder.rowTwoColumns("VAT (14%):", transaction.getTaxAmount().toPlainString());
        }

        builder.separator('=')
                .fontSize(EscPosCommandBuilder.FontSize.DOUBLE_HEIGHT)
                .bold(true)
                .rowTwoColumns("TOTAL:", (transaction.getTotalAmount() != null ? transaction.getTotalAmount().toPlainString() : "0.00") + " EGP")
                .fontSize(EscPosCommandBuilder.FontSize.NORMAL)
                .bold(false)
                .separator('-');

        builder.rowTwoColumns("Payment Method:", transaction.getPaymentMethod().name());
        if (transaction.getCashTendered() != null && transaction.getCashTendered().signum() > 0) {
            builder.rowTwoColumns("Cash Tendered:", transaction.getCashTendered().toPlainString());
        }
        if (transaction.getChangeAmount() != null && transaction.getChangeAmount().signum() > 0) {
            builder.rowTwoColumns("Change Due:", transaction.getChangeAmount().toPlainString());
        }

        // Tax QR Code
        if (printer.isPrintQrCode()) {
            builder.separator('-');
            String qrTlv = EscPosCommandBuilder.buildTlvQrString(
                    "BEMO ERP POS",
                    "300-123-456",
                    formattedDate,
                    transaction.getTotalAmount() != null ? transaction.getTotalAmount().toPlainString() : "0.00",
                    transaction.getTaxAmount() != null ? transaction.getTaxAmount().toPlainString() : "0.00"
            );
            builder.qrCode(qrTlv);
        }

        // Barcode
        builder.barcode128(transaction.getTransactionNumber());

        // Footer
        builder.align(EscPosCommandBuilder.Alignment.CENTER);
        if (printer.getFooterText() != null && !printer.getFooterText().trim().isEmpty()) {
            builder.line(printer.getFooterText().trim());
        } else {
            builder.line("شكراً لزيارتكم - نتمنى رؤيتكم قريباً")
                    .line("Thank you for your business!");
        }

        if (printer.isOpenDrawer() && transaction.getPaymentMethod() == PosPaymentMethod.CASH) {
            builder.kickDrawer();
        }
        if (printer.isCutPaper()) {
            builder.cutPaper();
        }

        byte[] bytes = builder.toByteArray();
        boolean sent = false;
        String statusMessage = "Receipt payload generated successfully";

        if (printer.getConnectionType() == ThermalPrinterConnectionType.NETWORK && printer.getIpAddress() != null) {
            sent = dispatchToNetworkPrinter(printer.getIpAddress(), printer.getPort(), bytes);
            statusMessage = sent ? "Printed directly to network printer" : "Dispatched to client (printer offline or network unreachable)";
        }

        return new ThermalPrinterApi.ReceiptPrintDataResponse(
                transaction.getId(),
                transaction.getTransactionNumber(),
                printer.getId(),
                printer.getName(),
                printer.getConnectionType(),
                printer.getIpAddress(),
                printer.getPort(),
                printer.getPaperWidth(),
                builder.toBase64(),
                transaction.getReprintCount(),
                transaction.getLastReprintedAt(),
                sent,
                statusMessage
        );
    }

    @Transactional
    public ThermalPrinterApi.ReceiptPrintDataResponse reprintReceipt(String transactionId, ThermalPrinterApi.ReprintReceiptRequest request,
                                                                    String username, String ipAddress) {
        if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessRuleException("Reprint reason is required", "POS_REPRINT_REASON_REQUIRED");
        }

        PosTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transaction not found", "POS_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        transaction.recordReprint();
        transactionRepository.save(transaction);

        String detailsJson = String.format("{\"reason\":\"%s\",\"reprintCount\":%d,\"transactionNumber\":\"%s\"}",
                request.reason().replace("\"", "\\\""),
                transaction.getReprintCount(),
                transaction.getTransactionNumber());

        auditService.record(
                "POS_RECEIPT_REPRINT",
                "PosTransaction",
                transaction.getId(),
                username != null ? username : "SYSTEM",
                detailsJson,
                ipAddress
        );

        log.info("PosTransaction {} reprinted by {} (count: {}, reason: {})",
                transaction.getTransactionNumber(), username, transaction.getReprintCount(), request.reason());

        return generateReceiptBytes(transactionId, request.printerId(), true);
    }

    public boolean dispatchToNetworkPrinter(String ipAddress, int port, byte[] bytes) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port > 0 ? port : 9100), 2000);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
            log.info("Successfully dispatched {} bytes to printer at {}:{}", bytes.length, ipAddress, port);
            return true;
        } catch (Exception ex) {
            log.warn("Network print to {}:{} failed: {}", ipAddress, port, ex.getMessage());
            return false;
        }
    }

    private ThermalPrinter resolvePrinter(String printerId, String terminalId, String branchId) {
        if (printerId != null && !printerId.trim().isEmpty()) {
            return printerRepository.findById(printerId)
                    .orElseGet(this::fallbackDefaultPrinter);
        }
        if (terminalId != null && !terminalId.trim().isEmpty()) {
            var printerOpt = printerRepository.findFirstByTerminalIdAndActiveTrue(terminalId);
            if (printerOpt.isPresent()) {
                return printerOpt.get();
            }
        }
        if (branchId != null && !branchId.trim().isEmpty()) {
            var printerOpt = printerRepository.findFirstByBranchIdAndActiveTrue(branchId);
            if (printerOpt.isPresent()) {
                return printerOpt.get();
            }
        }
        return printerRepository.findFirstByIsDefaultTrueAndActiveTrue()
                .orElseGet(this::fallbackDefaultPrinter);
    }

    private ThermalPrinter fallbackDefaultPrinter() {
        return new ThermalPrinter(
                "Standard ESC/POS 80mm",
                null,
                null,
                ThermalPrinterConnectionType.USB,
                null,
                9100,
                null,
                ThermalPaperWidth.MM_80,
                "CP864",
                "BEMO POS",
                "Thank You / شكراً لزيارتكم",
                false,
                true,
                true,
                true
        );
    }

    private EscPosCommandBuilder createBuilder(ThermalPrinter printer) {
        return new EscPosCommandBuilder(
                printer.getPaperWidth() != null ? printer.getPaperWidth().getColumns() : 48,
                printer.getCharacterCodePage() != null ? printer.getCharacterCodePage() : "CP864"
        );
    }

    private ThermalPrinterApi.PrinterResponse toResponse(ThermalPrinter p) {
        return new ThermalPrinterApi.PrinterResponse(
                p.getId(),
                p.getName(),
                p.getBranchId(),
                p.getTerminalId(),
                p.getConnectionType(),
                p.getIpAddress(),
                p.getPort(),
                p.getBluetoothMac(),
                p.getPaperWidth(),
                p.getCharacterCodePage(),
                p.getHeaderText(),
                p.getFooterText(),
                p.isOpenDrawer(),
                p.isCutPaper(),
                p.isPrintQrCode(),
                p.isDefault(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
