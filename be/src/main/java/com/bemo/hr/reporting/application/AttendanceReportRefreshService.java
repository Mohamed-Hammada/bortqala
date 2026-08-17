package com.bemo.hr.reporting.application;

import org.springframework.stereotype.Service;
import java.time.YearMonth;

/**
 * Compatibility shim for the earlier runtime patch.
 * Fresh imports now generate attendance reports directly in BiometricImportService.
 * Dashboard reads must never mutate/report-repair historical data.
 */
@Service
public class AttendanceReportRefreshService {
    public boolean needsRefresh(YearMonth period, boolean reportExists) {
        return false;
    }

    public boolean refreshMonth(int year, int month, String actor) {
        return false;
    }
}

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_NO_LAZY_REPAIR
