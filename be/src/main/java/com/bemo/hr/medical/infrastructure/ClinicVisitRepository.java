package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ClinicVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicVisitRepository extends JpaRepository<ClinicVisit, String> {

    Optional<ClinicVisit> findByAppIdAndId(String appId, String id);

    List<ClinicVisit> findAllByAppIdAndVisitDateOrderByTokenAsc(String appId, String visitDate);

    List<ClinicVisit> findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByTokenAsc(String appId, String doctorEmployeeId, String visitDate);

    List<ClinicVisit> findAllByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);

    @Query("SELECT COALESCE(MAX(v.token), 0) FROM ClinicVisit v WHERE v.appId = :appId AND v.doctorEmployeeId = :doctorEmployeeId AND v.visitDate = :visitDate")
    Integer findMaxTokenForDoctorAndDate(@Param("appId") String appId, @Param("doctorEmployeeId") String doctorEmployeeId, @Param("visitDate") String visitDate);

    @Query("SELECT v FROM ClinicVisit v WHERE v.appId = :appId AND v.doctorEmployeeId = :doctorEmployeeId AND v.status = 'DONE' AND v.visitDate LIKE CONCAT(:periodPrefix, '%')")
    List<ClinicVisit> findCompletedVisitsForDoctorInPeriod(@Param("appId") String appId, @Param("doctorEmployeeId") String doctorEmployeeId, @Param("periodPrefix") String periodPrefix);

    @Query("SELECT v FROM ClinicVisit v WHERE v.appId = :appId AND v.status = 'DONE' AND v.visitDate LIKE CONCAT(:periodPrefix, '%')")
    List<ClinicVisit> findCompletedVisitsInPeriod(@Param("appId") String appId, @Param("periodPrefix") String periodPrefix);
}
