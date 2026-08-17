package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.JournalSourceMetadataService;
import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/journals/metadata")
public class JournalSourceMetadataController {

    private final JournalSourceMetadataService metadataService;

    public JournalSourceMetadataController(JournalSourceMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping("/attach")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public JournalSourceMetadata attachSourceMetadata(@RequestBody AttachMetadataPayload payload) {
        return metadataService.attachSourceMetadata(payload.journalId(), payload.sourceDocumentType(), payload.sourceDocumentId());
    }

    @GetMapping("/{journalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public JournalSourceMetadata getMetadata(@PathVariable String journalId) {
        return metadataService.getMetadata(journalId);
    }

    public record AttachMetadataPayload(String journalId, String sourceDocumentType, String sourceDocumentId) {
    }
}
