package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.BiometricDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, String> {
    List<BiometricDevice> findAllByOrderByNameAsc();
}
