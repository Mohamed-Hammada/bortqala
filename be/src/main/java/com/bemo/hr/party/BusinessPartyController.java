package com.bemo.hr.party;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
class BusinessPartyController {
    private final BusinessPartyService businessPartyService;

    @GetMapping
    List<BusinessPartyApi.Response> list() { return businessPartyService.list(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    BusinessPartyApi.Response create(@Valid @RequestBody BusinessPartyApi.Request request) {
        return businessPartyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    BusinessPartyApi.Response update(@PathVariable String id, @Valid @RequestBody BusinessPartyApi.Request request) {
        return businessPartyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) { businessPartyService.deactivate(id); }

    @PostMapping("/cleanup-phone")
    @PreAuthorize("hasRole('ADMIN')")
    java.util.Map<String, Integer> cleanupInvalidPhone() {
        return java.util.Map.of("cleaned", businessPartyService.cleanupInvalidPhone());
    }
}
