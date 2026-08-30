package com.bemo.hr.platform.api;

import com.bemo.hr.platform.application.BulkUpdateService;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bulk-update")
public class BulkUpdateController {

    private final BulkUpdateService bulkUpdateService;

    public BulkUpdateController(BulkUpdateService bulkUpdateService) {
        this.bulkUpdateService = bulkUpdateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public PlatformApi.BulkUpdateResponse execute(
            @Valid @RequestBody PlatformApi.BulkUpdateRequest request,
            Authentication auth) {
        return bulkUpdateService.execute(TenantContext.require(), request, auth.getName());
    }
}
