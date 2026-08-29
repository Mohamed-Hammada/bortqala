package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PharmacyDispenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyDispenseLineRepository extends JpaRepository<PharmacyDispenseLine, String> {

    List<PharmacyDispenseLine> findAllByAppIdAndDispenseRecordId(String appId, String dispenseRecordId);

    List<PharmacyDispenseLine> findAllByAppIdAndPrescriptionLineId(String appId, String prescriptionLineId);
}
