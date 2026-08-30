package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    Optional<Patient> findByAppIdAndId(String appId, String id);

    Optional<Patient> findByAppIdAndMrn(String appId, String mrn);

    Optional<Patient> findByAppIdAndNationalId(String appId, String nationalId);

    List<Patient> findAllByAppIdAndPhone(String appId, String phone);

    @Query("SELECT p FROM Patient p WHERE p.appId = :appId AND (" +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.phone LIKE CONCAT('%', :query, '%') OR " +
           "p.mrn LIKE CONCAT('%', :query, '%') OR " +
           "p.nationalId LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY p.createdAt DESC")
    Page<Patient> searchPatients(@Param("appId") String appId, @Param("query") String query, Pageable pageable);

    Page<Patient> findAllByAppIdOrderByCreatedAtDesc(String appId, Pageable pageable);

    List<Patient> findAllByAppId(String appId);

    boolean existsByAppIdAndMrn(String appId, String mrn);
}
