package com.bemo.hr.reporting.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class AttendanceReportRefreshService {
    private final ReportingService reportingService;

    public boolean needsRefresh(YearMonth period, boolean reportExists) {
        return false;
    }

    public boolean refreshMonth(int year, int month, String actor) {
        return reportingService.recalculateMonth(year, month, actor);
    }
}

