package com.bemo.hr.trade.pos.api;

import com.bemo.hr.trade.pos.domain.ThermalPaperWidth;
import com.bemo.hr.trade.pos.domain.ThermalPrinterConnectionType;

public final class ThermalPrinterApi {

    private ThermalPrinterApi() {
    }

    public record SavePrinterRequest(
            String id,
            String name,
            String branchId,
            String terminalId,
            ThermalPrinterConnectionType connectionType,
            String ipAddress,
            Integer port,
            String bluetoothMac,
            ThermalPaperWidth paperWidth,
            String characterCodePage,
            String headerText,
            String footerText,
            boolean openDrawer,
            boolean cutPaper,
            boolean printQrCode,
            boolean isDefault,
            boolean active
    ) {}

    public record PrinterResponse(
            String id,
            String name,
            String branchId,
            String terminalId,
            ThermalPrinterConnectionType connectionType,
            String ipAddress,
            Integer port,
            String bluetoothMac,
            ThermalPaperWidth paperWidth,
            String characterCodePage,
            String headerText,
            String footerText,
            boolean openDrawer,
            boolean cutPaper,
            boolean printQrCode,
            boolean isDefault,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record ReprintReceiptRequest(
            String reason,
            String printerId
    ) {}

    public record ReceiptPrintDataResponse(
            String transactionId,
            String transactionNumber,
            String printerId,
            String printerName,
            ThermalPrinterConnectionType connectionType,
            String ipAddress,
            Integer port,
            ThermalPaperWidth paperWidth,
            String base64Bytes,
            int reprintCount,
            Long lastReprintedAt,
            boolean sentToPrinter,
            String statusMessage
    ) {}

    public record TestPrintResponse(
            String printerId,
            String printerName,
            String base64Bytes,
            boolean sentToPrinter,
            String message
    ) {}
}
