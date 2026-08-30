package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalOtSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalOtScheduleRepository extends JpaRepository<HospitalOtSchedule, String> {

    Optional<HospitalOtSchedule> findByAppIdAndId(String appId, String id);

    List<HospitalOtSchedule> findAllByAppIdOrderByPlannedStartDesc(String appId);

    List<HospitalOtSchedule> findAllByAppIdAndTheaterNameOrderByPlannedStartAsc(String appId, String theaterName);

    List<HospitalOtSchedule> findAllByAppIdAndPatientIdOrderByPlannedStartDesc(String appId, String patientId);
}
