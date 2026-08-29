package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientMrnSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientMrnSequenceRepository extends JpaRepository<PatientMrnSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PatientMrnSequence s WHERE s.appId = :appId")
    Optional<PatientMrnSequence> findByAppIdForUpdate(@Param("appId") String appId);

    Optional<PatientMrnSequence> findByAppId(String appId);
}
