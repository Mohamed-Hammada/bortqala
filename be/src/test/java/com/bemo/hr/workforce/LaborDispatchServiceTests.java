package com.bemo.hr.workforce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LaborDispatchServiceTests {

    private LaborDispatchRepository dispatchRepository;
    private WorkerAssignmentRepository assignmentRepository;
    private LaborDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchRepository = mock(LaborDispatchRepository.class);
        assignmentRepository = mock(WorkerAssignmentRepository.class);
        dispatchService = new LaborDispatchService(dispatchRepository, assignmentRepository);
    }

    @Test
    void createsDispatchAndAssignsWorkerSuccessfully() {
        when(dispatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dispatchRepository.findById("disp-1")).thenReturn(Optional.of(new LaborDispatch("req-1", "cont-1", LocalDate.of(2026, 2, 1))));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LaborDispatch dispatch = dispatchService.createDispatch("req-1", "cont-1", LocalDate.of(2026, 2, 1));
        assertThat(dispatch).isNotNull();

        WorkerAssignment assignment = dispatchService.assignWorker(
                "disp-1",
                "worker-10",
                "req-line-1",
                "cont-1",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 15),
                new BigDecimal("150.00"),
                new BigDecimal("8.00")
        );

        assertThat(assignment).isNotNull();
        assertThat(assignment.getStatus()).isEqualTo(WorkerAssignment.Status.PROPOSED);
    }

    @Test
    void assignmentAcceptAndReplaceFlow() {
        WorkerAssignment assignment = new WorkerAssignment(
                "disp-1", "worker-10", "req-line-1", "cont-1",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15),
                new BigDecimal("150.00"), new BigDecimal("8.00")
        );

        when(assignmentRepository.findById("ass-1")).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dispatchService.acceptAssignment("ass-1");
        assertThat(assignment.getStatus()).isEqualTo(WorkerAssignment.Status.ACCEPTED);
    }
}
