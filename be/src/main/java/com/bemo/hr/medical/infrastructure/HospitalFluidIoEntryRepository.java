package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalFluidIoEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalFluidIoEntryRepository extends JpaRepository<HospitalFluidIoEntry, String> {

    Optional<HospitalFluidIoEntry> findByAppIdAndId(String appId, String id);

    List<HospitalFluidIoEntry> findAllByAppIdAndAdmissionIdOrderByEntryTimeAsc(String appId, String admissionId);
}
