package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/contractors")
@RequiredArgsConstructor
public class ContractorController {
    private final ContractorService contractorService;

    @GetMapping
    public List<WorkforceApi.ContractorResponse> list() {
        return contractorService.list();
    }

    @GetMapping("/{id}")
    public WorkforceApi.ContractorResponse getById(@PathVariable String id) {
        return contractorService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.ContractorResponse create(@Valid @RequestBody WorkforceApi.ContractorRequest request) {
        return contractorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.ContractorResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.ContractorRequest request) {
        return contractorService.update(id, request);
    }
}
