# Dashboard attendance calculation diagnostic

This file is generated while applying the corrective patch.
The patch deliberately does not fabricate `present`/`late` from raw punches because lateness must use the configured attendance category/schedule/grace policy.

Candidate source files containing attendance evaluation terms:
- be/src/main/java/com/bemo/hr/employee/application/EmployeeCodeDedupService.java
- be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java
- be/src/main/java/com/bemo/hr/payroll/application/PayrollSnapshotService.java
- be/src/main/java/com/bemo/hr/payroll/domain/PayrollInputSnapshot.java
- be/src/main/java/com/bemo/hr/reporting/api/AttendanceExceptionApi.java
- be/src/main/java/com/bemo/hr/reporting/api/ReportingApi.java
- be/src/main/java/com/bemo/hr/reporting/application/AttendanceExceptionService.java
- be/src/main/java/com/bemo/hr/reporting/application/DashboardService.java
- be/src/main/java/com/bemo/hr/reporting/application/ReportingService.java
- be/src/main/java/com/bemo/hr/reporting/domain/AttendanceException.java
- be/src/main/java/com/bemo/hr/reporting/domain/AttendancePolicy.java
- be/src/main/java/com/bemo/hr/reporting/domain/AttendancePolicyScope.java
- be/src/main/java/com/bemo/hr/reporting/domain/AttendanceReportDecision.java
- be/src/main/java/com/bemo/hr/reporting/domain/DailyAttendanceCalculator.java
- be/src/main/java/com/bemo/hr/reporting/domain/DailyAttendanceResult.java
- be/src/main/java/com/bemo/hr/reporting/domain/DayAnomalyResultSnapshot.java
- be/src/main/java/com/bemo/hr/reporting/infrastructure/ApachePoiReportExporter.java
- be/src/main/java/com/bemo/hr/reporting/infrastructure/AttendancePolicyRepository.java
- be/src/main/java/com/bemo/hr/reporting/infrastructure/DailyAttendanceResultRepository.java
- be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java
- fe/src/app/features/reports/business-reports.catalog.ts
- fe/src/app/features/reports/report-review.page.html
- fe/src/app/features/reports/report-review.page.spec.ts
- fe/src/app/features/reports/report-review.page.ts
- fe/src/app/features/reports/reports.models.ts
- fe/src/app/features/workforce/pages/manual-attendance/manual-attendance.component.ts

If the dashboard still shows zero after imported punches are mapped to employees, inspect these candidates and verify that the import completion path invokes/rebuilds the daily attendance result/evaluation used by the dashboard.
