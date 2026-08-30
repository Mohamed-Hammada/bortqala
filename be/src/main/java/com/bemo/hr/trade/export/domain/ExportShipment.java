package com.bemo.hr.trade.export.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "export_shipments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"app_id", "shipment_number"})
})
public class ExportShipment {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "shipment_number", nullable = false, length = 50)
    private String shipmentNumber;

    @Column(name = "customer_party_id", nullable = false, length = 36)
    private String customerPartyId;

    @Column(name = "customer_party_name", length = 200)
    private String customerPartyName;

    @Column(name = "contract_ref", length = 100)
    private String contractRef;

    @Column(name = "container_no", length = 50)
    private String containerNo;

    @Column(name = "booking_no", length = 100)
    private String bookingNo;

    @Column(name = "acid_no", length = 100)
    private String acidNo;

    @Column(name = "port_of_loading", length = 200)
    private String portOfLoading;

    @Column(name = "port_of_discharge", length = 200)
    private String portOfDischarge;

    @Column(name = "etb_date")
    private Long etbDate;

    @Column(name = "eta_date")
    private Long etaDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExportShipmentStatus status;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "expected_fx_amount", precision = 19, scale = 2)
    private java.math.BigDecimal expectedFxAmount;

    @Column(name = "expected_fx_currency", length = 10)
    private String expectedFxCurrency;

    @Column(name = "realized_fx_amount", precision = 19, scale = 2)
    private java.math.BigDecimal realizedFxAmount;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineOrder ASC")
    private List<ExportShipmentLine> lines = new ArrayList<>();

    protected ExportShipment() {
    }

    public ExportShipment(String shipmentNumber, String customerPartyId, String customerPartyName) {
        this.id = UUID.randomUUID().toString();
        this.shipmentNumber = shipmentNumber.strip();
        this.customerPartyId = customerPartyId;
        this.customerPartyName = customerPartyName;
        this.status = ExportShipmentStatus.PREPARING;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public void transitionTo(ExportShipmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + status + " → " + target);
        }
        this.status = target;
    }

    public void addLine(ExportShipmentLine line) {
        lines.add(line);
        line.setShipment(this);
    }

    public java.math.BigDecimal totalQuantity() {
        return lines.stream()
                .map(l -> l.getQuantity() == null ? java.math.BigDecimal.ZERO : l.getQuantity())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getShipmentNumber() { return shipmentNumber; }
    public String getCustomerPartyId() { return customerPartyId; }
    public void setCustomerPartyId(String v) { this.customerPartyId = v; }
    public String getCustomerPartyName() { return customerPartyName; }
    public void setCustomerPartyName(String v) { this.customerPartyName = v; }
    public String getContractRef() { return contractRef; }
    public void setContractRef(String v) { this.contractRef = v; }
    public String getContainerNo() { return containerNo; }
    public void setContainerNo(String v) { this.containerNo = v; }
    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String v) { this.bookingNo = v; }
    public String getAcidNo() { return acidNo; }
    public void setAcidNo(String v) { this.acidNo = v; }
    public String getPortOfLoading() { return portOfLoading; }
    public void setPortOfLoading(String v) { this.portOfLoading = v; }
    public String getPortOfDischarge() { return portOfDischarge; }
    public void setPortOfDischarge(String v) { this.portOfDischarge = v; }
    public Long getEtbDate() { return etbDate; }
    public void setEtbDate(Long v) { this.etbDate = v; }
    public Long getEtaDate() { return etaDate; }
    public void setEtaDate(Long v) { this.etaDate = v; }
    public ExportShipmentStatus getStatus() { return status; }
    public void setStatus(ExportShipmentStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public java.math.BigDecimal getExpectedFxAmount() { return expectedFxAmount; }
    public void setExpectedFxAmount(java.math.BigDecimal v) { this.expectedFxAmount = v; }
    public String getExpectedFxCurrency() { return expectedFxCurrency; }
    public void setExpectedFxCurrency(String v) { this.expectedFxCurrency = v; }
    public java.math.BigDecimal getRealizedFxAmount() { return realizedFxAmount; }
    public void setRealizedFxAmount(java.math.BigDecimal v) { this.realizedFxAmount = v; }
    public List<ExportShipmentLine> getLines() { return lines; }
    public void setLines(List<ExportShipmentLine> lines) { this.lines = lines; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long v) { this.createdAt = v; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
