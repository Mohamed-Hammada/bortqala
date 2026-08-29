package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.DoctorRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRosterRepository extends JpaRepository<DoctorRoster, String> {

    List<DoctorRoster> findAllByAppIdAndDoctorEmployeeIdAndActiveTrue(String appId, String doctorEmployeeId);

    List<DoctorRoster> findAllByAppIdAndDoctorEmployeeIdAndWeekdayAndActiveTrue(String appId, String doctorEmployeeId, int weekday);

    Optional<DoctorRoster> findByAppIdAndId(String appId, String id);

    List<DoctorRoster> findAllByAppId(String appId);
}
