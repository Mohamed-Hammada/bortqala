package com.bemo.hr.product.catalog;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PublicCatalogService {

    private final CatalogProductRepository catalogProductRepository;

    public PublicCatalogService(CatalogProductRepository catalogProductRepository) {
        this.catalogProductRepository = catalogProductRepository;
    }

    @Transactional(readOnly = true)
    public PublicCatalogApi.ProductPageResponse searchProducts(
            String categorySlug, String brandSlug, BigDecimal minPrice, BigDecimal maxPrice,
            String search, int page, int size, String sortBy) {

        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));

        Sort sort = "price_asc".equalsIgnoreCase(sortBy) ? Sort.by(Sort.Direction.ASC, "publicPrice") :
                "price_desc".equalsIgnoreCase(sortBy) ? Sort.by(Sort.Direction.DESC, "publicPrice") :
                Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        String catSlug = (categorySlug != null && !categorySlug.isBlank()) ? categorySlug.strip().toLowerCase() : null;
        String brSlug = (brandSlug != null && !brandSlug.isBlank()) ? brandSlug.strip().toLowerCase() : null;
        String searchTerm = (search != null && !search.isBlank()) ? search.strip() : null;

        Page<CatalogProduct> productPage = catalogProductRepository.searchPublishedProducts(
                catSlug, brSlug, minPrice, maxPrice, searchTerm, pageable
        );

        List<PublicCatalogApi.PublicProductSummary> items = productPage.getContent().stream()
                .map(PublicCatalogApi.PublicProductSummary::from)
                .toList();

        return new PublicCatalogApi.ProductPageResponse(
                items,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PublicCatalogApi.PublicProductDetail getProductBySlug(String slug) {
        CatalogProduct product = catalogProductRepository.findBySlugAndIsPublishedTrue(slug.strip().toLowerCase())
                .orElseThrow(() -> new BusinessRuleException("Catalog product not found", "CATALOG_PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return PublicCatalogApi.PublicProductDetail.from(product);
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogApi.CategorySummary> getCategories() {
        return catalogProductRepository.findPublishedCategoriesWithCount().stream()
                .map(row -> new PublicCatalogApi.CategorySummary(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogApi.BrandSummary> getBrands() {
        return catalogProductRepository.findPublishedBrandsWithCount().stream()
                .map(row -> new PublicCatalogApi.BrandSummary(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }
}
