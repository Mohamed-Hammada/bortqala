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
@RequestMapping("/api/v1/workforce/contractors")
@RequiredArgsConstructor
public class ContractorController {
    private final ContractorService contractorService;
    private final WorkforceMasterDataExcelExporter excelExporter;

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_TEAM)
    public ResponseEntity<byte[]> exportExcel() {
        return excel("contractors.xlsx", excelExporter.contractors(contractorService.list()));
    }

    private ResponseEntity<byte[]> excel(String filename, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_TEAM)
    public List<WorkforceApi.ContractorResponse> list() {
        return contractorService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_TEAM)
    public WorkforceApi.ContractorResponse getById(@PathVariable String id) {
        return contractorService.getById(id);
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.ContractorResponse create(@Valid @RequestBody WorkforceApi.ContractorRequest request) {
        return contractorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceApi.ContractorResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.ContractorRequest request) {
        return contractorService.update(id, request);
    }
}
