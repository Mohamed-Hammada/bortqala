package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.domain.BiometricDevice;

import java.time.Instant;
import java.util.List;

public interface BiometricDeviceClient {
    DeviceResponse fetch(BiometricDevice device, DeviceCredentials credentials);

    record DeviceCredentials(String username, String password) {
    }

    record DevicePunch(String deviceUserId, String employeeName, Instant punchedAt, String rawLine) {
    }

    record DeviceResponse(byte[] rawContent, List<DevicePunch> punches) {
    }
}
