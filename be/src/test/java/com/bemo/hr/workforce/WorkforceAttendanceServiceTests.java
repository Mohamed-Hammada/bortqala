package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceAttendanceServiceTests {

    @Mock
    private ManualAttendanceEntryRepository attendanceRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private AuditService auditService;
    private WorkforceAttendanceService service;

    @Test
    void validatesCellsThoroughlyIncludingDates() {
        // Fix V-02 and V-18 baseline
        var req = new WorkforceApi.BatchAttendanceRequest(java.util.List.of(
                new WorkforceApi.AttendanceCell("w1", "bad-date", java.math.BigDecimal.ONE, null, null, null, null, null, null, null)
        ));

        var resp = service.saveBatch(req);
        org.assertj.core.api.Assertions.assertThat(resp.errors()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(resp.errors().get(0).field()).isEqualTo("workDate");
    }

    @BeforeEach
    void setUp() {
        service = new WorkforceAttendanceService(attendanceRepository, workerRepository, auditService);
    }

    @Test
    void savesValidCellsTogetherAndReturnsPerCellSummaryWithoutDroppingValidChanges() {
        var worker1 = worker("W-1", "عامل أول");
        var worker2 = worker("W-2", "عامل ثان");
        var existing = new ManualAttendanceEntry(worker1.getId(), "2026-07-01", BigDecimal.ONE,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("250"), "MANUAL", null);
        when(workerRepository.findAllById(any())).thenReturn(List.of(worker1, worker2));
        when(attendanceRepository.findByWorkDateBetween("2026-07-01", "2026-07-02"))
                .thenReturn(List.of(existing));
        when(attendanceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveBatch(new WorkforceApi.BatchAttendanceRequest(List.of(
                cell(worker1.getId(), "2026-07-01", "0.5"),
                cell(worker1.getId(), "2026-07-02", "1"),
                cell(worker2.getId(), "2026-07-02", "0"),
                cell(worker2.getId(), "bad-date", "1")
        )));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.savedEntries()).hasSize(3);
        assertThat(response.errors()).singleElement().satisfies(error -> {
            assertThat(error.workerId()).isEqualTo(worker2.getId());
            assertThat(error.field()).isEqualTo("workDate");
        });
        assertThat(existing.getAttendanceValue()).isEqualByComparingTo("0.5");
        verify(attendanceRepository).saveAll(anyList());
    }

    private Worker worker(String code, String name) {
        return new Worker(code, name, "contractor", "category", new BigDecimal("250"),
                new BigDecimal("8"), null, "MANUAL", "ACTIVE", null, null, null);
    }

    private WorkforceApi.AttendanceCell cell(String workerId, String date, String value) {
        return new WorkforceApi.AttendanceCell(workerId, date, new BigDecimal(value), null, null,
                null, null, null, null, null);
    }
}
