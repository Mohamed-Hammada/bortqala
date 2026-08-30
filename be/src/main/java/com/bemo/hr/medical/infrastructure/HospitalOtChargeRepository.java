package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalOtCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalOtChargeRepository extends JpaRepository<HospitalOtCharge, String> {

    Optional<HospitalOtCharge> findByAppIdAndId(String appId, String id);

    List<HospitalOtCharge> findAllByAppIdAndOtScheduleIdOrderByChargedAtAsc(String appId, String otScheduleId);
}
