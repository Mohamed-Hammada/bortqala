package com.bemo.hr.compliance.privacy.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsentRegistryRepository extends JpaRepository<ConsentRegistry, String> {
    List<ConsentRegistry> findByAppIdAndSubjectRefAndWithdrawnAtIsNull(String appId, String subjectRef);
    List<ConsentRegistry> findByAppIdAndSubjectRef(String appId, String subjectRef);
}
