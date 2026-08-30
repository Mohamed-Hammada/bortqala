package com.bemo.hr.product.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogProductRepository extends JpaRepository<CatalogProduct, String> {

    Optional<CatalogProduct> findBySlugAndIsPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM CatalogProduct p WHERE p.isPublished = true " +
            "AND (:categorySlug IS NULL OR p.categorySlug = :categorySlug) " +
            "AND (:brandSlug IS NULL OR p.brandSlug = :brandSlug) " +
            "AND (:minPrice IS NULL OR p.publicPrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.publicPrice <= :maxPrice) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CatalogProduct> searchPublishedProducts(
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p.categorySlug, p.categoryName, COUNT(p.id) FROM CatalogProduct p WHERE p.isPublished = true GROUP BY p.categorySlug, p.categoryName")
    List<Object[]> findPublishedCategoriesWithCount();

    @Query("SELECT DISTINCT p.brandSlug, p.brandName, COUNT(p.id) FROM CatalogProduct p WHERE p.isPublished = true GROUP BY p.brandSlug, p.brandName")
    List<Object[]> findPublishedBrandsWithCount();
}
