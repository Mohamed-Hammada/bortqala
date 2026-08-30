package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ExamTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, String> {

    Optional<ExamTemplate> findByAppIdAndId(String appId, String id);

    List<ExamTemplate> findAllByAppIdAndActiveOrderBySpecialtyAscNameAsc(String appId, Boolean active);

    List<ExamTemplate> findAllByAppIdAndSpecialtyAndActiveOrderByNameAsc(String appId, String specialty, Boolean active);
}
