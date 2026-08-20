package com.bemo.hr.shared.job.api;

import com.bemo.hr.shared.job.application.IdempotentScheduledJobService;
import com.bemo.hr.shared.job.domain.ScheduledJobExecutionRecord;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/jobs")
public class IdempotentScheduledJobController {

    private final IdempotentScheduledJobService jobService;

    public IdempotentScheduledJobController(IdempotentScheduledJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/executions/{jobName}")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_VIEWER)
    public List<ScheduledJobExecutionRecord> getExecutions(@PathVariable String jobName) {
        return jobService.getExecutions(jobName);
    }
}
