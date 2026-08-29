package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.LabTestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestItemRepository extends JpaRepository<LabTestItem, String> {

    Optional<LabTestItem> findByAppIdAndId(String appId, String id);

    Optional<LabTestItem> findByAppIdAndCode(String appId, String code);

    List<LabTestItem> findAllByAppIdOrderByCodeAsc(String appId);

    List<LabTestItem> findAllByAppIdAndCategoryOrderByCodeAsc(String appId, LabTestItem.Category category);
}
