package com.bemo.hr.shared.outbox;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/outbox")
public class OutboxController {

    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @GetMapping("/events")
    @PreAuthorize("@auth.hasPermission('system:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public OutboxApi.OutboxPageResponse listEvents(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return outboxService.listEvents(status, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("@auth.hasPermission('system:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public OutboxApi.OutboxStatsResponse getStats() {
        return outboxService.getStats();
    }

    @PostMapping("/events/{id}/retry")
    @PreAuthorize("@auth.hasPermission('system:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retryEvent(@PathVariable String id) {
        outboxService.retryEvent(id);
    }
}
