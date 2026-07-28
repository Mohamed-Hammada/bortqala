package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/attendance")
@RequiredArgsConstructor
public class WorkforceAttendanceController {
    private final WorkforceAttendanceService attendanceService;

    @GetMapping
    public List<ManualAttendanceEntry> getByRange(@RequestParam String startDate, @RequestParam String endDate) {
        return attendanceService.getByDateRange(startDate, endDate);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    public List<ManualAttendanceEntry> saveBatch(@Valid @RequestBody WorkforceApi.BatchAttendanceRequest request) {
        return attendanceService.saveBatch(request);
    }
}
