package com.bemo.hr.compliance.privacy.api;

import com.bemo.hr.compliance.privacy.application.PrivacyApi;
import com.bemo.hr.compliance.privacy.application.PrivacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/privacy")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    @GetMapping("/requests")
    ResponseEntity<List<PrivacyApi.Response>> listRequests() {
        return ResponseEntity.ok(privacyService.listRequests().stream()
                .map(PrivacyApi.Response::from).toList());
    }

    @GetMapping("/requests/{id}")
    ResponseEntity<PrivacyApi.Response> getRequest(@PathVariable String id) {
        return ResponseEntity.ok(PrivacyApi.Response.from(privacyService.getRequest(id)));
    }

    @PostMapping("/requests")
    ResponseEntity<PrivacyApi.Response> createRequest(@Valid @RequestBody PrivacyApi.CreateRequest request) {
        return ResponseEntity.ok(PrivacyApi.Response.from(privacyService.createRequest(request)));
    }

    @PostMapping("/requests/{id}/decide")
    ResponseEntity<PrivacyApi.Response> decideRequest(@PathVariable String id,
                                                       @Valid @RequestBody PrivacyApi.DecideRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(PrivacyApi.Response.from(
                privacyService.decideRequest(id, request, authentication.getName())));
    }

    @GetMapping("/export/{subjectType}/{subjectRef}")
    ResponseEntity<String> exportData(@PathVariable String subjectType, @PathVariable String subjectRef) {
        return ResponseEntity.ok(privacyService.exportSubjectData(subjectType, subjectRef));
    }

    @PostMapping("/erase/{subjectType}/{subjectRef}")
    ResponseEntity<List<String>> eraseData(@PathVariable String subjectType, @PathVariable String subjectRef,
                                            Authentication authentication) {
        return ResponseEntity.ok(privacyService.eraseSubjectData(subjectType, subjectRef, authentication.getName()));
    }

    @GetMapping("/consents/{subjectRef}")
    ResponseEntity<List<PrivacyApi.ConsentResponse>> listConsents(@PathVariable String subjectRef) {
        return ResponseEntity.ok(privacyService.listConsents(subjectRef).stream()
                .map(PrivacyApi.ConsentResponse::from).toList());
    }

    @PostMapping("/consents")
    ResponseEntity<PrivacyApi.ConsentResponse> grantConsent(@Valid @RequestBody PrivacyApi.ConsentRequest request) {
        return ResponseEntity.ok(PrivacyApi.ConsentResponse.from(privacyService.grantConsent(request)));
    }

    @PostMapping("/consents/withdraw")
    ResponseEntity<PrivacyApi.ConsentResponse> withdrawConsent(@Valid @RequestBody PrivacyApi.ConsentWithdrawRequest request) {
        return ResponseEntity.ok(PrivacyApi.ConsentResponse.from(privacyService.withdrawConsent(request)));
    }

    @GetMapping("/retention-policies")
    ResponseEntity<List<PrivacyApi.RetentionPolicyResponse>> listRetentionPolicies() {
        return ResponseEntity.ok(privacyService.listRetentionPolicies().stream()
                .map(PrivacyApi.RetentionPolicyResponse::from).toList());
    }

    @PostMapping("/retention-policies")
    ResponseEntity<PrivacyApi.RetentionPolicyResponse> createRetentionPolicy(
            @Valid @RequestBody PrivacyApi.RetentionPolicyRequest request) {
        return ResponseEntity.ok(PrivacyApi.RetentionPolicyResponse.from(privacyService.createRetentionPolicy(request)));
    }

    @PostMapping("/retention-policies/{id}/dry-run")
    ResponseEntity<List<PrivacyApi.DryRunResult>> dryRunRetention(@PathVariable String id) {
        return ResponseEntity.ok(privacyService.dryRunRetention());
    }
}
