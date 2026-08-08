package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkerCategoryRepository extends JpaRepository<WorkerCategory, String> {
    Optional<WorkerCategory> findByCode(String code);
    List<WorkerCategory> findByStatus(String status);
    Optional<WorkerCategory> findByCategoryId(String categoryId);
    List<WorkerCategory> findByCategoryIdIn(Collection<String> categoryIds);
    boolean existsByCategoryId(String categoryId);
}
