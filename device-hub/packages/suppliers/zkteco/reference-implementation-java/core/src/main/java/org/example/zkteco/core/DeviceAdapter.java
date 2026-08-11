package org.example.zkteco.core;

import java.time.Instant;
import java.util.List;

public interface DeviceAdapter {
    DeviceProtocol protocol();
    boolean supports(DeviceEndpoint endpoint);
    DeviceProbeResult probe(DeviceEndpoint endpoint);

    default List<PersonRecord> readUsers(DeviceEndpoint endpoint) {
        throw new UnsupportedOperationException(protocol() + " does not implement USER_READ");
    }
    default List<AttendanceEvent> readAttendance(DeviceEndpoint endpoint, Instant sinceExclusive) {
        throw new UnsupportedOperationException(protocol() + " does not implement ATTENDANCE_READ");
    }
    default DeviceCommandResult execute(DeviceEndpoint endpoint, DeviceCommand command) {
        throw new UnsupportedOperationException(protocol() + " does not implement RAW_COMMAND");
    }
}
