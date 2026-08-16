package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.AttendanceExplorerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports/attendance")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
public class AttendanceExplorerController {
    private final AttendanceExplorerService attendanceExplorerService;

    public AttendanceExplorerController(AttendanceExplorerService attendanceExplorerService) {
        this.attendanceExplorerService = attendanceExplorerService;
    }

    @GetMapping("/months")
    List<AttendanceExplorerApi.MonthSummaryResponse> months() {
        return attendanceExplorerService.months();
    }

    @GetMapping("/months/{month}/employees")
    List<AttendanceExplorerApi.EmployeeSummaryResponse> employees(@PathVariable String month) {
        return attendanceExplorerService.employees(month);
    }

    @GetMapping("/employees/{deviceUserId}")
    AttendanceExplorerApi.EmployeeAttendanceResponse employee(
            @PathVariable String deviceUserId,
            @RequestParam(required = false) String month) {
        return attendanceExplorerService.employee(deviceUserId, month);
    }
}
