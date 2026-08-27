package com.bemo.hr.knowledgebase.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface KbArticleRepository extends JpaRepository<KbArticle, String> {
    List<KbArticle> findByAppIdAndPublishedTrueOrderByViewsDesc(String appId);
    Optional<KbArticle> findByAppIdAndSlug(String appId, String slug);
    Optional<KbArticle> findByAppIdAndId(String appId, String id);

    @Query("SELECT a FROM KbArticle a WHERE a.appId = :appId AND a.published = true AND "
         + "(LOWER(a.titleAr) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(a.titleEn) LIKE LOWER(CONCAT('%',:q,'%')) "
         + "OR LOWER(a.bodyAr) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(a.bodyEn) LIKE LOWER(CONCAT('%',:q,'%')) "
         + "OR LOWER(a.tags) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY a.views DESC")
    List<KbArticle> search(@Param("appId") String appId, @Param("q") String query);

    List<KbArticle> findByAppIdOrderByCreatedAtDesc(String appId);
}
