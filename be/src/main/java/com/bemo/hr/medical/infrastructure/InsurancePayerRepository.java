package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.InsurancePayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePayerRepository extends JpaRepository<InsurancePayer, String> {

    Optional<InsurancePayer> findByAppIdAndId(String appId, String id);

    List<InsurancePayer> findAllByAppIdOrderByNameAsc(String appId);

    List<InsurancePayer> findAllByAppIdAndActiveTrueOrderByNameAsc(String appId);
}
