package com.bemo.hr.workforce;

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
    @PreAuthorize("@auth.hasPermission('workers.read')")
    public ResponseEntity<byte[]> exportExcel() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("workers.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(excelExporter.workers(workerService.list()));
    }

    @GetMapping
    @PreAuthorize("@auth.hasPermission('workers.read')")
    public List<WorkforceApi.WorkerResponse> list(@RequestParam(required = false) String contractorId) {
        if (contractorId != null && !contractorId.isBlank()) {
            return workerService.listByContractor(contractorId);
        }
        return workerService.list();
    }

    @PostMapping
    @PreAuthorize("@auth.hasPermission('workers.create')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.WorkerResponse create(@Valid @RequestBody WorkforceApi.WorkerRequest request) {
        return workerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@auth.hasPermission('workers.edit')")
    public WorkforceApi.WorkerResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.WorkerRequest request) {
        return workerService.update(id, request);
    }
}
