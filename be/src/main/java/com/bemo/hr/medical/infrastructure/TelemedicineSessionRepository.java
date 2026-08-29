package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.TelemedicineSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemedicineSessionRepository extends JpaRepository<TelemedicineSession, String> {
    List<TelemedicineSession> findByPatientIdOrderByScheduledTimeDesc(String patientId);
    List<TelemedicineSession> findByDoctorIdOrderByScheduledTimeDesc(String doctorId);
}
