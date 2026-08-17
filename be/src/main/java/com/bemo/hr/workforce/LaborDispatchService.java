package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class LaborDispatchService {

    private final LaborDispatchRepository laborDispatchRepository;
    private final WorkerAssignmentRepository workerAssignmentRepository;
    private final AuditService auditService;

    public LaborDispatchService(LaborDispatchRepository laborDispatchRepository,
                                WorkerAssignmentRepository workerAssignmentRepository,
                                AuditService auditService) {
        this.laborDispatchRepository = laborDispatchRepository;
        this.workerAssignmentRepository = workerAssignmentRepository;
        this.auditService = auditService;
    }

    private static void requireText(String value, String code) {
        if (value == null || value.isBlank()) throw rule("A required dispatch value is missing", code);
    }

    private static BusinessRuleException rule(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.CONFLICT);
    }

    @Transactional
    public LaborDispatch createDispatch(String requestId, String contractorId, LocalDate dispatchDate, String actor) {
        log.debug("createDispatch called with requestId={}, contractorId={}, dispatchDate={}", requestId, contractorId, dispatchDate);
        requireText(requestId, "DISPATCH_REQUEST_REQUIRED");
        requireText(contractorId, "DISPATCH_CONTRACTOR_REQUIRED");
        if (dispatchDate == null) {
            throw rule("Dispatch date is required", "DISPATCH_DATE_REQUIRED");
        }
        LaborDispatch dispatch = new LaborDispatch(requestId, contractorId, dispatchDate);
        LaborDispatch saved = laborDispatchRepository.save(dispatch);
        log.info("LaborDispatch {} created successfully", saved.getId());
        auditService.record("CREATE", "LABOR_DISPATCH", saved.getId(), actor,
                "{\"requestId\":\"" + requestId + "\",\"contractorId\":\"" + contractorId + "\"}", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LaborDispatch> listDispatches() {
        log.debug("listDispatches called");
        return laborDispatchRepository.findAllByOrderByDispatchDateDescCreatedAtDesc();
    }

    @Transactional
    public LaborDispatch dispatch(String id, String actor) {
        return transition(id, actor, "DISPATCH", LaborDispatch::dispatch);
    }

    @Transactional
    public LaborDispatch accept(String id, String actor) {
        return transition(id, actor, "ACCEPT", LaborDispatch::accept);
    }

    @Transactional
    public LaborDispatch cancel(String id, String actor) {
        return transition(id, actor, "CANCEL", LaborDispatch::cancel);
    }

    @Transactional
    public WorkerAssignment assignWorker(String dispatchId, String workerId, String requestLineId, String contractorId,
                                         LocalDate fromDate, LocalDate toDate, BigDecimal agreedRate, BigDecimal agreedHours,
                                         String actor) {
        log.debug("assignWorker called with dispatchId={}, workerId={}", dispatchId, workerId);
        LaborDispatch dispatch = laborDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new BusinessRuleException("Dispatch not found", "DISPATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (dispatch.getStatus() == LaborDispatch.Status.CANCELLED) {
            throw rule("Assignments cannot be added to a cancelled dispatch", "DISPATCH_CANCELLED");
        }
        requireText(workerId, "DISPATCH_WORKER_REQUIRED");
        requireText(contractorId, "DISPATCH_CONTRACTOR_REQUIRED");
        if (!dispatch.getContractorId().equals(contractorId)) {
            throw rule("Assignment contractor must match the dispatch contractor", "DISPATCH_CONTRACTOR_MISMATCH");
        }
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw rule("Assignment dates are invalid", "ASSIGNMENT_DATE_RANGE_INVALID");
        }
        if (agreedRate == null || agreedRate.signum() < 0 || agreedHours == null || agreedHours.signum() <= 0) {
            throw rule("Assignment rate and hours are invalid", "ASSIGNMENT_TERMS_INVALID");
        }

        WorkerAssignment assignment = new WorkerAssignment(dispatchId, workerId, requestLineId, contractorId, fromDate, toDate, agreedRate, agreedHours);
        WorkerAssignment saved = workerAssignmentRepository.save(assignment);
        log.info("WorkerAssignment {} created successfully", saved.getId());
        auditService.record("CREATE", "WORKER_ASSIGNMENT", saved.getId(), actor,
                "{\"dispatchId\":\"" + dispatchId + "\",\"workerId\":\"" + workerId + "\"}", null);
        return saved;
    }

    @Transactional
    public WorkerAssignment acceptAssignment(String assignmentId, String actor) {
        WorkerAssignment assignment = getAssignment(assignmentId);
        return transitionAssignment(assignment, actor, "ACCEPT", assignment::accept);
    }

    @Transactional
    public WorkerAssignment rejectAssignment(String assignmentId, String reason, String actor) {
        WorkerAssignment assignment = getAssignment(assignmentId);
        if (reason == null || reason.isBlank())
            throw rule("Assignment rejection reason is required", "ASSIGNMENT_REJECTION_REASON_REQUIRED");
        return transitionAssignment(assignment, actor, "REJECT", () -> assignment.reject(reason));
    }

    @Transactional
    public WorkerAssignment replaceAssignment(String assignmentId, String newWorkerId, BigDecimal agreedRate, BigDecimal agreedHours,
                                              String actor) {
        log.debug("replaceAssignment called with assignmentId={}, newWorkerId={}", assignmentId, newWorkerId);
        WorkerAssignment oldAssignment = getAssignment(assignmentId);
        transitionAssignment(oldAssignment, actor, "REPLACE", oldAssignment::replace);

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
        WorkerAssignment saved = workerAssignmentRepository.save(newAssignment);
        log.info("WorkerAssignment {} replaced successfully (new id={})", assignmentId, saved.getId());
        auditService.record("CREATE", "WORKER_ASSIGNMENT", saved.getId(), actor,
                "{\"replacementFor\":\"" + assignmentId + "\",\"workerId\":\"" + newWorkerId + "\"}", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkerAssignment> getAssignmentsByDispatch(String dispatchId) {
        log.debug("getAssignmentsByDispatch called with dispatchId={}", dispatchId);
        if (!laborDispatchRepository.existsById(dispatchId)) {
            throw new BusinessRuleException("Dispatch not found", "DISPATCH_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return workerAssignmentRepository.findByDispatchId(dispatchId);
    }

    private WorkerAssignment getAssignment(String id) {
        return workerAssignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Assignment not found", "ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private LaborDispatch transition(String id, String actor, String action,
                                     java.util.function.Consumer<LaborDispatch> transition) {
        LaborDispatch dispatch = laborDispatchRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Dispatch not found", "DISPATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
        LaborDispatch.Status previous = dispatch.getStatus();
        try {
            transition.accept(dispatch);
        } catch (IllegalStateException exception) {
            throw rule(exception.getMessage(), "DISPATCH_STATUS_INVALID");
        }
        LaborDispatch saved = laborDispatchRepository.save(dispatch);
        log.info("LaborDispatch {} {} successfully ({} -> {})", id, action, previous, saved.getStatus());
        auditService.record(action, "LABOR_DISPATCH", id, actor,
                "{\"from\":\"" + previous + "\",\"to\":\"" + saved.getStatus() + "\"}", null);
        return saved;
    }

    private WorkerAssignment transitionAssignment(WorkerAssignment assignment, String actor, String action, Runnable transition) {
        WorkerAssignment.Status previous = assignment.getStatus();
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw rule(exception.getMessage(), "ASSIGNMENT_STATUS_INVALID");
        }
        WorkerAssignment saved = workerAssignmentRepository.save(assignment);
        log.info("WorkerAssignment {} {} successfully ({} -> {})", saved.getId(), action, previous, saved.getStatus());
        auditService.record(action, "WORKER_ASSIGNMENT", saved.getId(), actor,
                "{\"from\":\"" + previous + "\",\"to\":\"" + saved.getStatus() + "\"}", null);
        return saved;
    }
}
