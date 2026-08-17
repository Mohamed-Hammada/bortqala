package com.bemo.hr.product.trial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemoTenantTemplateRepository extends JpaRepository<DemoTenantTemplate, String> {
    Optional<DemoTenantTemplate> findFirstByCodeAndActiveTrueOrderByTemplateVersionDesc(String code);

    Optional<DemoTenantTemplate> findByCodeAndTemplateVersionAndActiveTrue(String code, int version);

    List<DemoTenantTemplate> findAllByActiveTrueOrderByCodeAscTemplateVersionDesc();
}
