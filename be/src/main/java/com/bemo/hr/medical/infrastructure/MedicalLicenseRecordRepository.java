package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.MedicalLicenseRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalLicenseRecordRepository extends JpaRepository<MedicalLicenseRecord, String> {
    List<MedicalLicenseRecord> findByPractitionerId(String practitionerId);
    List<MedicalLicenseRecord> findAllByOrderByExpiryDateAsc();
}
