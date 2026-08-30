package com.bemo.hr.product.catalog;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
public class CatalogProduct {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "sku", length = 60, nullable = false)
    private String sku;

    @Column(name = "slug", length = 120, nullable = false)
    private String slug;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "name_ar", length = 200)
    private String nameAr;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "category_slug", length = 60, nullable = false)
    private String categorySlug;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @Column(name = "brand_slug", length = 60, nullable = false)
    private String brandSlug;

    @Column(name = "brand_name", length = 100, nullable = false)
    private String brandName;

    @Column(name = "public_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal publicPrice;

    @Column(name = "compare_at_price", precision = 15, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "EGP";

    @Column(name = "stock_status", length = 30, nullable = false)
    private String stockStatus = "IN_STOCK";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "specifications_json")
    private String specificationsJson;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = true;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogProduct() {
    }

    public CatalogProduct(String sku, String slug, String name, String nameAr, String categorySlug, String categoryName,
                          String brandSlug, String brandName, BigDecimal publicPrice, String currency, String specificationsJson) {
        this.id = UUID.randomUUID().toString();
        this.sku = Objects.requireNonNull(sku, "sku must not be null").strip();
        this.slug = Objects.requireNonNull(slug, "slug must not be null").strip().toLowerCase();
        this.name = Objects.requireNonNull(name, "name must not be null").strip();
        this.nameAr = nameAr != null ? nameAr.strip() : null;
        this.categorySlug = Objects.requireNonNull(categorySlug, "categorySlug must not be null").strip().toLowerCase();
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName must not be null").strip();
        this.brandSlug = Objects.requireNonNull(brandSlug, "brandSlug must not be null").strip().toLowerCase();
        this.brandName = Objects.requireNonNull(brandName, "brandName must not be null").strip();
        this.publicPrice = Objects.requireNonNull(publicPrice, "publicPrice must not be null");
        this.currency = currency != null ? currency.strip() : "EGP";
        this.specificationsJson = specificationsJson;
        this.isPublished = true;
        this.stockStatus = "IN_STOCK";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updatePrice(BigDecimal newPrice, BigDecimal compareAtPrice) {
        this.publicPrice = Objects.requireNonNull(newPrice, "newPrice must not be null");
        this.compareAtPrice = compareAtPrice;
        this.updatedAt = Instant.now();
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = Objects.requireNonNull(stockStatus, "stockStatus must not be null");
        this.updatedAt = Instant.now();
    }

    public void setPublished(boolean isPublished) {
        this.isPublished = isPublished;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getSku() {
        return sku;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getDescription() {
        return description;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getBrandSlug() {
        return brandSlug;
    }

    public String getBrandName() {
        return brandName;
    }

    public BigDecimal getPublicPrice() {
        return publicPrice;
    }

    public BigDecimal getCompareAtPrice() {
        return compareAtPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSpecificationsJson() {
        return specificationsJson;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
