package com.bemo.hr.attendance.application;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

public interface BiometricFileReader {
    ParsedFile read(String fileName, InputStream inputStream);

    record ParsedFile(List<PunchRow> rows, List<RowError> errors, int totalRows) { }
    record PunchRow(int rowNumber, String deviceUserId, String employeeName, Instant punchedAt, String rawLine) { }
    record RowError(int rowNumber, String message, String rawLine) { }
}
