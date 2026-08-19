package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/workers")
@RequiredArgsConstructor
public class WorkerController {
    private final WorkerService workerService;
    private final WorkforceMasterDataExcelExporter excelExporter;

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_TEAM)
    public ResponseEntity<byte[]> exportExcel() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("workers.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(excelExporter.workers(workerService.list()));
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_TEAM)
    public List<WorkforceApi.WorkerResponse> list(@RequestParam(required = false) String contractorId) {
        if (contractorId != null && !contractorId.isBlank()) {
            return workerService.listByContractor(contractorId);
        }
        return workerService.list();
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.WorkerResponse create(@Valid @RequestBody WorkforceApi.WorkerRequest request) {
        return workerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceApi.WorkerResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.WorkerRequest request) {
        return workerService.update(id, request);
    }
}
