package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.BiometricDeviceIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiometricDeviceIntegrationRepository extends JpaRepository<BiometricDeviceIntegration, String> {
    List<BiometricDeviceIntegration> findAllByOrderByNameAsc();
    Optional<BiometricDeviceIntegration> findByBiometricDeviceId(String biometricDeviceId);
    Optional<BiometricDeviceIntegration> findByHubDeviceId(String hubDeviceId);
}
