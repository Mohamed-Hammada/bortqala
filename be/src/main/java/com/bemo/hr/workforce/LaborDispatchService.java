package com.bemo.hr.workforce;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LaborDispatchService {

    private final LaborDispatchRepository dispatchRepository;
    private final WorkerAssignmentRepository assignmentRepository;

    public LaborDispatchService(LaborDispatchRepository dispatchRepository,
                                WorkerAssignmentRepository assignmentRepository) {
        this.dispatchRepository = dispatchRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public LaborDispatch createDispatch(String requestId, String contractorId, LocalDate dispatchDate) {
        LaborDispatch dispatch = new LaborDispatch(requestId, contractorId, dispatchDate);
        return dispatchRepository.save(dispatch);
    }

    @Transactional
    public WorkerAssignment assignWorker(String dispatchId, String workerId, String requestLineId, String contractorId,
                                         LocalDate fromDate, LocalDate toDate, BigDecimal agreedRate, BigDecimal agreedHours) {
        LaborDispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new BusinessRuleException("Dispatch not found", "DISPATCH_NOT_FOUND", HttpStatus.NOT_FOUND));

        WorkerAssignment assignment = new WorkerAssignment(dispatchId, workerId, requestLineId, contractorId, fromDate, toDate, agreedRate, agreedHours);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public WorkerAssignment acceptAssignment(String assignmentId) {
        WorkerAssignment assignment = getAssignment(assignmentId);
        assignment.accept();
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public WorkerAssignment rejectAssignment(String assignmentId, String reason) {
        WorkerAssignment assignment = getAssignment(assignmentId);
        assignment.reject(reason);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public WorkerAssignment replaceAssignment(String assignmentId, String newWorkerId, BigDecimal agreedRate, BigDecimal agreedHours) {
        WorkerAssignment oldAssignment = getAssignment(assignmentId);
        oldAssignment.replace();
        assignmentRepository.save(oldAssignment);

        WorkerAssignment newAssignment = new WorkerAssignment(
                oldAssignment.getDispatchId(),
                newWorkerId,
                oldAssignment.getRequestLineId(),
                oldAssignment.getContractorId(),
                oldAssignment.getFromDate(),
                oldAssignment.getToDate(),
                agreedRate,
                agreedHours
        );
        return assignmentRepository.save(newAssignment);
    }

    @Transactional(readOnly = true)
    public List<WorkerAssignment> getAssignmentsByDispatch(String dispatchId) {
        return assignmentRepository.findByDispatchId(dispatchId);
    }

    private WorkerAssignment getAssignment(String id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Assignment not found", "ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
