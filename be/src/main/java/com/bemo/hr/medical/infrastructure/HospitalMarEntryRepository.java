package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalMarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalMarEntryRepository extends JpaRepository<HospitalMarEntry, String> {

    Optional<HospitalMarEntry> findByAppIdAndId(String appId, String id);

    List<HospitalMarEntry> findAllByAppIdAndAdmissionIdOrderByDueAtAsc(String appId, String admissionId);
}
