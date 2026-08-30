package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ClinicAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicAppointmentRepository extends JpaRepository<ClinicAppointment, String> {

    Optional<ClinicAppointment> findByAppIdAndId(String appId, String id);

    List<ClinicAppointment> findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByStartsAtAsc(String appId, String doctorEmployeeId, String visitDate);

    List<ClinicAppointment> findAllByAppIdAndVisitDateOrderByStartsAtAsc(String appId, String visitDate);

    List<ClinicAppointment> findAllByAppIdAndPatientIdOrderByStartsAtDesc(String appId, String patientId);

    @Query("SELECT a FROM ClinicAppointment a WHERE a.appId = :appId AND a.doctorEmployeeId = :doctorEmployeeId AND a.startsAt = :startsAt AND a.status != 'CANCELLED'")
    Optional<ClinicAppointment> findActiveByDoctorAndStartsAt(@Param("appId") String appId, @Param("doctorEmployeeId") String doctorEmployeeId, @Param("startsAt") long startsAt);

    @Query("SELECT a FROM ClinicAppointment a WHERE a.appId = :appId AND a.visitDate = :visitDate AND a.status IN ('BOOKED', 'CONFIRMED') AND a.reminderSentAt IS NULL")
    List<ClinicAppointment> findPendingRemindersForDate(@Param("appId") String appId, @Param("visitDate") String visitDate);

    @Query("SELECT a FROM ClinicAppointment a WHERE a.appId = :appId AND a.doctorEmployeeId = :doctorEmployeeId AND a.visitDate LIKE CONCAT(:periodPrefix, '%')")
    List<ClinicAppointment> findAllForDoctorInPeriod(@Param("appId") String appId, @Param("doctorEmployeeId") String doctorEmployeeId, @Param("periodPrefix") String periodPrefix);
}
