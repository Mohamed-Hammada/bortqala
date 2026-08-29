package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalNursingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalNursingNoteRepository extends JpaRepository<HospitalNursingNote, String> {

    Optional<HospitalNursingNote> findByAppIdAndId(String appId, String id);

    List<HospitalNursingNote> findAllByAppIdAndAdmissionIdOrderByRecordedAtDesc(String appId, String admissionId);
}
