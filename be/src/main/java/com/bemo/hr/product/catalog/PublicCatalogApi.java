package com.bemo.hr.product.catalog;

import java.math.BigDecimal;
import java.util.List;

public final class PublicCatalogApi {

    private PublicCatalogApi() {
    }

    public record PublicProductSummary(
            String id,
            String sku,
            String slug,
            String name,
            String nameAr,
            String categorySlug,
            String categoryName,
            String brandSlug,
            String brandName,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String currency,
            String stockStatus,
            String imageUrl,
            boolean isFeatured
    ) {
        public static PublicProductSummary from(CatalogProduct p) {
            return new PublicProductSummary(
                    p.getId(),
                    p.getSku(),
                    p.getSlug(),
                    p.getName(),
                    p.getNameAr(),
                    p.getCategorySlug(),
                    p.getCategoryName(),
                    p.getBrandSlug(),
                    p.getBrandName(),
                    p.getPublicPrice(),
                    p.getCompareAtPrice(),
                    p.getCurrency(),
                    p.getStockStatus(),
                    p.getImageUrl(),
                    p.isFeatured()
            );
        }
    }

    public record PublicProductDetail(
            String id,
            String sku,
            String slug,
            String name,
            String nameAr,
            String description,
            String categorySlug,
            String categoryName,
            String brandSlug,
            String brandName,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String currency,
            String stockStatus,
            String imageUrl,
            String specificationsJson,
            boolean isFeatured
    ) {
        public static PublicProductDetail from(CatalogProduct p) {
            return new PublicProductDetail(
                    p.getId(),
                    p.getSku(),
                    p.getSlug(),
                    p.getName(),
                    p.getNameAr(),
                    p.getDescription(),
                    p.getCategorySlug(),
                    p.getCategoryName(),
                    p.getBrandSlug(),
                    p.getBrandName(),
                    p.getPublicPrice(),
                    p.getCompareAtPrice(),
                    p.getCurrency(),
                    p.getStockStatus(),
                    p.getImageUrl(),
                    p.getSpecificationsJson(),
                    p.isFeatured()
            );
        }
    }

    public record CategorySummary(
            String slug,
            String name,
            long productCount
    ) {}

    public record BrandSummary(
            String slug,
            String name,
            long productCount
    ) {}

    public record ProductPageResponse(
            List<PublicProductSummary> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
