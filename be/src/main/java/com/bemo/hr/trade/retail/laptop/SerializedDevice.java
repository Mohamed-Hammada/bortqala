package com.bemo.hr.trade.retail.laptop;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "retail_serialized_devices")
public class SerializedDevice {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "serial_number", length = 100, nullable = false)
    private String serialNumber;

    @Column(name = "imei", length = 50)
    private String imei;

    @Column(name = "brand", length = 60, nullable = false)
    private String brand;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "cpu", length = 80, nullable = false)
    private String cpu;

    @Column(name = "cpu_generation", length = 40)
    private String cpuGeneration;

    @Column(name = "ram_gb", nullable = false)
    private int ramGb;

    @Column(name = "ram_type", length = 30)
    private String ramType;

    @Column(name = "storage_gb", nullable = false)
    private int storageGb;

    @Column(name = "storage_type", length = 30, nullable = false)
    private String storageType;

    @Column(name = "gpu", length = 80)
    private String gpu;

    @Column(name = "screen_size_inch", precision = 4, scale = 1)
    private BigDecimal screenSizeInch;

    @Column(name = "resolution", length = 30)
    private String resolution;

    @Column(name = "refresh_rate_hz")
    private Integer refreshRateHz;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "keyboard_layout", length = 30)
    private String keyboardLayout;

    @Column(name = "color", length = 40)
    private String color;

    @Column(name = "condition_grade", length = 30, nullable = false)
    private String conditionGrade = "NEW";

    @Column(name = "purchase_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "IN_STOCK";

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "purchase_date")
    private Instant purchaseDate;

    @Column(name = "supplier_warranty_months")
    private Integer supplierWarrantyMonths = 12;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "sale_date")
    private Instant saleDate;

    @Column(name = "customer_warranty_months")
    private Integer customerWarrantyMonths = 12;

    @Column(name = "warranty_end_date")
    private Instant warrantyEndDate;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SerializedDevice() {
    }

    public SerializedDevice(String serialNumber, String brand, String model, String cpu, int ramGb,
                            int storageGb, String storageType, BigDecimal purchasePrice, BigDecimal sellingPrice,
                            String conditionGrade, String supplierId) {
        this.id = UUID.randomUUID().toString();
        this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber must not be null").strip();
        this.brand = Objects.requireNonNull(brand, "brand must not be null").strip();
        this.model = Objects.requireNonNull(model, "model must not be null").strip();
        this.cpu = Objects.requireNonNull(cpu, "cpu must not be null").strip();
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.storageType = Objects.requireNonNull(storageType, "storageType must not be null").strip();
        this.purchasePrice = Objects.requireNonNull(purchasePrice, "purchasePrice must not be null");
        this.sellingPrice = Objects.requireNonNull(sellingPrice, "sellingPrice must not be null");
        this.conditionGrade = conditionGrade != null ? conditionGrade.strip() : "NEW";
        this.supplierId = supplierId;
        this.purchaseDate = Instant.now();
        this.status = "IN_STOCK";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void sellToCustomer(String customerId, String customerName, int warrantyMonths, BigDecimal finalSellingPrice) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerWarrantyMonths = warrantyMonths;
        this.saleDate = Instant.now();
        this.warrantyEndDate = this.saleDate.plus(java.time.Duration.ofDays(warrantyMonths * 30L));
        this.status = "SOLD";
        if (finalSellingPrice != null) {
            this.sellingPrice = finalSellingPrice;
        }
        this.updatedAt = Instant.now();
    }

    public void processReturn(String returnReason) {
        this.status = "RETURNED";
        this.updatedAt = Instant.now();
    }

    public boolean isInStock() {
        return "IN_STOCK".equalsIgnoreCase(this.status);
    }

    public boolean isWarrantyActive() {
        return this.warrantyEndDate != null && Instant.now().isBefore(this.warrantyEndDate);
    }

    public BigDecimal getMargin() {
        return sellingPrice.subtract(purchasePrice);
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getCpu() {
        return cpu;
    }

    public int getRamGb() {
        return ramGb;
    }

    public int getStorageGb() {
        return storageGb;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getGpu() {
        return gpu;
    }

    public void setGpu(String gpu) {
        this.gpu = gpu;
    }

    public String getConditionGrade() {
        return conditionGrade;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Instant getSaleDate() {
        return saleDate;
    }

    public Instant getWarrantyEndDate() {
        return warrantyEndDate;
    }
}
