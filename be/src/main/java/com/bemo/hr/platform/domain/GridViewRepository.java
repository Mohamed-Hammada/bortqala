package com.bemo.hr.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GridViewRepository extends JpaRepository<GridView, String> {
    List<GridView> findByAppIdAndUserIdAndPageKeyOrderByCreatedAtDesc(String appId, String userId, String pageKey);
    List<GridView> findByAppIdAndPageKeyOrderByCreatedAtDesc(String appId, String pageKey);
}
