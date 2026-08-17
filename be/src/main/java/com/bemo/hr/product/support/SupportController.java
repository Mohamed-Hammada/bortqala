package com.bemo.hr.product.support;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SupportController {
    private final SupportService service;

    @GetMapping("/tickets")
    List<SupportApi.TicketResponse> tickets() {
        return service.tickets();
    }

    @PostMapping("/tickets")
    SupportApi.TicketResponse create(@Valid @RequestBody SupportApi.TicketRequest r, Authentication a) {
        return service.create(r, a.getName());
    }

    @GetMapping("/tickets/{id}/updates")
    List<SupportApi.UpdateResponse> updates(@PathVariable String id) {
        return service.updates(id);
    }

    @PutMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    SupportApi.TicketResponse update(@PathVariable String id, @Valid @RequestBody SupportApi.TicketUpdateRequest r, Authentication a) {
        return service.update(id, r, a.getName());
    }

    @PostMapping("/feedback")
    SupportApi.FeedbackResponse feedback(@Valid @RequestBody SupportApi.FeedbackRequest r, Authentication a) {
        return service.feedback(r, a.getName());
    }

    @GetMapping("/feedback")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    List<SupportApi.FeedbackResponse> feedback() {
        return service.feedback();
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    SupportApi.HealthResponse health() {
        return service.latest();
    }

    @PostMapping("/health/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    SupportApi.HealthResponse calculate(@Valid @RequestBody SupportApi.HealthRequest r, Authentication a) {
        return service.calculate(r, a.getName());
    }
}
