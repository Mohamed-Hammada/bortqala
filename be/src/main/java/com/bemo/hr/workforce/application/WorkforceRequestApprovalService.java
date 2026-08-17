package com.bemo.hr.workforce.application;

import com.bemo.hr.workforce.domain.WorkforceRequestApproval;
import com.bemo.hr.workforce.infrastructure.WorkforceRequestApprovalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class WorkforceRequestApprovalService {

    private final WorkforceRequestApprovalRepository repository;

    public WorkforceRequestApprovalService(WorkforceRequestApprovalRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkforceRequestApproval submitDecision(String requestId, String approverUserId, WorkforceRequestApproval.Decision decision, String comment) {
        WorkforceRequestApproval approval = new WorkforceRequestApproval(requestId, approverUserId, decision, comment);
        return repository.save(approval);
    }

    @Transactional(readOnly = true)
    public List<WorkforceRequestApproval> getApprovalsForRequest(String requestId) {
        return repository.findByRequestId(requestId);
    }
}
