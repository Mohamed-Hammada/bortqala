package com.bemo.hr.product.catalog;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/catalog")
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/products")
    public PublicCatalogApi.ProductPageResponse searchProducts(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String brandSlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        return publicCatalogService.searchProducts(categorySlug, brandSlug, minPrice, maxPrice, search, page, size, sortBy);
    }

    @GetMapping("/products/{slug}")
    public PublicCatalogApi.PublicProductDetail getProduct(@PathVariable String slug) {
        return publicCatalogService.getProductBySlug(slug);
    }

    @GetMapping("/categories")
    public List<PublicCatalogApi.CategorySummary> getCategories() {
        return publicCatalogService.getCategories();
    }

    @GetMapping("/brands")
    public List<PublicCatalogApi.BrandSummary> getBrands() {
        return publicCatalogService.getBrands();
    }
}
