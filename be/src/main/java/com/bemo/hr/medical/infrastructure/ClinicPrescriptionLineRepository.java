package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ClinicPrescriptionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicPrescriptionLineRepository extends JpaRepository<ClinicPrescriptionLine, String> {

    List<ClinicPrescriptionLine> findAllByAppIdAndVisitIdOrderByCreatedAtAsc(String appId, String visitId);

    void deleteAllByAppIdAndVisitId(String appId, String visitId);
}
