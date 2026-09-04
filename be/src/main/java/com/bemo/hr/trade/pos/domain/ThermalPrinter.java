package com.bemo.hr.trade.pos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "pos_thermal_printers")
public class ThermalPrinter {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "branch_id", length = 64)
    private String branchId;

    @Column(name = "terminal_id", length = 64)
    private String terminalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 32)
    private ThermalPrinterConnectionType connectionType;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "port")
    private Integer port;

    @Column(name = "bluetooth_mac", length = 64)
    private String bluetoothMac;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_width", nullable = false, length = 32)
    private ThermalPaperWidth paperWidth;

    @Column(name = "character_code_page", length = 32)
    private String characterCodePage;

    @Column(name = "header_text", length = 500)
    private String headerText;

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "open_drawer", nullable = false)
    private boolean openDrawer;

    @Column(name = "cut_paper", nullable = false)
    private boolean cutPaper;

    @Column(name = "print_qr_code", nullable = false)
    private boolean printQrCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ThermalPrinter() {
    }

    public ThermalPrinter(String name, String branchId, String terminalId, ThermalPrinterConnectionType connectionType,
                          String ipAddress, Integer port, String bluetoothMac, ThermalPaperWidth paperWidth,
                          String characterCodePage, String headerText, String footerText, boolean openDrawer,
                          boolean cutPaper, boolean printQrCode, boolean isDefault) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.branchId = branchId;
        this.terminalId = terminalId;
        this.connectionType = connectionType != null ? connectionType : ThermalPrinterConnectionType.NETWORK;
        this.ipAddress = ipAddress;
        this.port = port != null ? port : 9100;
        this.bluetoothMac = bluetoothMac;
        this.paperWidth = paperWidth != null ? paperWidth : ThermalPaperWidth.MM_80;
        this.characterCodePage = characterCodePage != null ? characterCodePage : "CP864";
        this.headerText = headerText;
        this.footerText = footerText;
        this.openDrawer = openDrawer;
        this.cutPaper = cutPaper;
        this.printQrCode = printQrCode;
        this.isDefault = isDefault;
        this.active = true;
    }

    public void update(String name, String branchId, String terminalId, ThermalPrinterConnectionType connectionType,
                       String ipAddress, Integer port, String bluetoothMac, ThermalPaperWidth paperWidth,
                       String characterCodePage, String headerText, String footerText, boolean openDrawer,
                       boolean cutPaper, boolean printQrCode, boolean isDefault, boolean active) {
        this.name = name;
        this.branchId = branchId;
        this.terminalId = terminalId;
        this.connectionType = connectionType;
        this.ipAddress = ipAddress;
        this.port = port != null ? port : 9100;
        this.bluetoothMac = bluetoothMac;
        this.paperWidth = paperWidth;
        this.characterCodePage = characterCodePage != null ? characterCodePage : "CP864";
        this.headerText = headerText;
        this.footerText = footerText;
        this.openDrawer = openDrawer;
        this.cutPaper = cutPaper;
        this.printQrCode = printQrCode;
        this.isDefault = isDefault;
        this.active = active;
    }

    public void markDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getName() {
        return name;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public ThermalPrinterConnectionType getConnectionType() {
        return connectionType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public String getBluetoothMac() {
        return bluetoothMac;
    }

    public ThermalPaperWidth getPaperWidth() {
        return paperWidth;
    }

    public String getCharacterCodePage() {
        return characterCodePage;
    }

    public String getHeaderText() {
        return headerText;
    }

    public String getFooterText() {
        return footerText;
    }

    public boolean isOpenDrawer() {
        return openDrawer;
    }

    public boolean isCutPaper() {
        return cutPaper;
    }

    public boolean isPrintQrCode() {
        return printQrCode;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
