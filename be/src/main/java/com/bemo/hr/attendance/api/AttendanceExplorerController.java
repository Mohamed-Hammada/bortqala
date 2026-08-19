package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.AttendanceExplorerService;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports/attendance")
@PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
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
