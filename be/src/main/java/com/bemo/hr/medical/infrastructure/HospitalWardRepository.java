package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalWardRepository extends JpaRepository<HospitalWard, String> {

    Optional<HospitalWard> findByAppIdAndId(String appId, String id);

    List<HospitalWard> findAllByAppIdOrderByNameAsc(String appId);
}
