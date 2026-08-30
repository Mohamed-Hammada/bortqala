package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalBedStay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalBedStayRepository extends JpaRepository<HospitalBedStay, String> {

    Optional<HospitalBedStay> findByAppIdAndId(String appId, String id);

    List<HospitalBedStay> findAllByAppIdAndAdmissionIdOrderByStartedAtAsc(String appId, String admissionId);

    Optional<HospitalBedStay> findByAppIdAndAdmissionIdAndEndedAtIsNull(String appId, String admissionId);
}
