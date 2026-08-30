package com.bemo.hr.product.catalog;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceTests {

    @Mock
    private CatalogProductRepository catalogProductRepository;

    private PublicCatalogService service;

    @BeforeEach
    void setUp() {
        service = new PublicCatalogService(catalogProductRepository);
    }

    @Test
    void searchProducts_returnsPublishedOnly() {
        CatalogProduct p1 = new CatalogProduct("SKU-1", "dell-xps-15", "Dell XPS 15", "ديل اكس بي اس 15",
                "laptops", "Laptops", "dell", "Dell", new BigDecimal("45000"), "EGP", "{\"cpu\":\"i7\"}");

        Page<CatalogProduct> page = new PageImpl<>(List.of(p1));
        when(catalogProductRepository.searchPublishedProducts(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PublicCatalogApi.ProductPageResponse res = service.searchProducts("laptops", "dell", null, null, null, 0, 20, "newest");

        assertNotNull(res);
        assertEquals(1, res.items().size());
        assertEquals("dell-xps-15", res.items().get(0).slug());
        assertEquals(new BigDecimal("45000"), res.items().get(0).price());
    }

    @Test
    void getProductBySlug_found_returnsDetail() {
        CatalogProduct p = new CatalogProduct("SKU-1", "macbook-pro-16", "MacBook Pro 16", "ماك بوك برو 16",
                "laptops", "Laptops", "apple", "Apple", new BigDecimal("85000"), "EGP", "{\"cpu\":\"M3 Pro\"}");

        when(catalogProductRepository.findBySlugAndIsPublishedTrue("macbook-pro-16")).thenReturn(Optional.of(p));

        PublicCatalogApi.PublicProductDetail detail = service.getProductBySlug("macbook-pro-16");

        assertNotNull(detail);
        assertEquals("MacBook Pro 16", detail.name());
        assertEquals("apple", detail.brandSlug());
        assertEquals("{\"cpu\":\"M3 Pro\"}", detail.specificationsJson());
    }

    @Test
    void getProductBySlug_notFound_throwsException() {
        when(catalogProductRepository.findBySlugAndIsPublishedTrue("unknown-laptop")).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.getProductBySlug("unknown-laptop"));

        assertEquals("CATALOG_PRODUCT_NOT_FOUND", ex.getCode());
    }

    @Test
    void getCategoriesAndBrands_returnsAggregations() {
        when(catalogProductRepository.findPublishedCategoriesWithCount())
                .thenReturn(List.<Object[]>of(new Object[]{"laptops", "Laptops", 12L}));
        when(catalogProductRepository.findPublishedBrandsWithCount())
                .thenReturn(List.<Object[]>of(new Object[]{"dell", "Dell", 5L}));

        List<PublicCatalogApi.CategorySummary> categories = service.getCategories();
        List<PublicCatalogApi.BrandSummary> brands = service.getBrands();

        assertEquals(1, categories.size());
        assertEquals("laptops", categories.get(0).slug());
        assertEquals(12L, categories.get(0).productCount());

        assertEquals(1, brands.size());
        assertEquals("dell", brands.get(0).slug());
        assertEquals(5L, brands.get(0).productCount());
    }
}
