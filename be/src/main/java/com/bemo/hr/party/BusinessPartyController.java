package com.bemo.hr.party;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
class BusinessPartyController {
    private final BusinessPartyService businessPartyService;
    private final SupplierOnboardingService supplierOnboardingService;

    @GetMapping
    List<BusinessPartyApi.Response> list() {
        return businessPartyService.list();
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    BusinessPartyApi.Response create(@Valid @RequestBody BusinessPartyApi.Request request) {
        return businessPartyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    BusinessPartyApi.Response update(@PathVariable String id, @Valid @RequestBody BusinessPartyApi.Request request) {
        return businessPartyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        businessPartyService.deactivate(id);
    }

    @PostMapping("/cleanup-phone")
    @PreAuthorize(Roles.ADMIN_ONLY)
    java.util.Map<String, Integer> cleanupInvalidPhone() {
        return java.util.Map.of("cleaned", businessPartyService.cleanupInvalidPhone());
    }

    @PostMapping("/supplier-requests")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    BusinessPartyApi.Response createSupplierRequest(@Valid @RequestBody SupplierOnboardingApi.SupplierRequest request) {
        return supplierOnboardingService.createRequest(request);
    }

    @GetMapping("/supplier-duplicates")
    SupplierOnboardingApi.DuplicateResponse duplicates(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String taxId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String iban,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String excludeSupplierId) {
        return supplierOnboardingService.duplicates(taxId, iban, excludeSupplierId);
    }

    @GetMapping("/{id}/supplier-360")
    SupplierOnboardingApi.Supplier360 supplier360(@PathVariable String id) {
        return supplierOnboardingService.get360(id);
    }

    @PostMapping(value = "/{id}/documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    SupplierOnboardingApi.DocumentResponse addDocument(@PathVariable String id,
                                                       @Valid @org.springframework.web.bind.annotation.RequestPart("metadata") SupplierOnboardingApi.DocumentRequest request,
                                                       @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return supplierOnboardingService.addDocument(id, request, file);
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    org.springframework.http.ResponseEntity<byte[]> downloadDocument(@PathVariable String id, @PathVariable String documentId) {
        SupplierDocument document = supplierOnboardingService.downloadDocument(id, documentId);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getFileSize())
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename(document.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(document.contentCopy());
    }

    @PostMapping("/{id}/documents/{documentId}/verify")
    @PreAuthorize(Roles.ADMIN_ONLY)
    SupplierOnboardingApi.DocumentResponse verifyDocument(@PathVariable String id, @PathVariable String documentId) {
        return supplierOnboardingService.verifyDocument(id, documentId);
    }

    @PostMapping("/{id}/bank-accounts")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    SupplierOnboardingApi.BankAccountResponse addBank(@PathVariable String id,
                                                      @Valid @RequestBody SupplierOnboardingApi.BankAccountRequest request) {
        return supplierOnboardingService.addBankAccount(id, request);
    }

    @PostMapping("/{id}/bank-accounts/{accountId}/verify")
    @PreAuthorize(Roles.ADMIN_ONLY)
    SupplierOnboardingApi.BankAccountResponse verifyBank(@PathVariable String id, @PathVariable String accountId) {
        return supplierOnboardingService.verifyBankAccount(id, accountId);
    }

    @PostMapping("/{id}/onboarding/submit")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    BusinessPartyApi.Response submitOnboarding(@PathVariable String id) {
        return supplierOnboardingService.submit(id);
    }

    @PostMapping("/{id}/onboarding/approve")
    @PreAuthorize(Roles.ADMIN_ONLY)
    BusinessPartyApi.Response approveOnboarding(@PathVariable String id) {
        return supplierOnboardingService.approve(id);
    }

    @PostMapping("/{id}/onboarding/activate")
    @PreAuthorize(Roles.ADMIN_ONLY)
    BusinessPartyApi.Response activateOnboarding(@PathVariable String id) {
        return supplierOnboardingService.activate(id);
    }

    @PostMapping("/{id}/onboarding/suspend")
    @PreAuthorize(Roles.ADMIN_ONLY)
    BusinessPartyApi.Response suspend(@PathVariable String id, @RequestBody SupplierOnboardingApi.TransitionRequest request) {
        return supplierOnboardingService.suspend(id, request.reason());
    }

    @PostMapping("/{id}/onboarding/blacklist")
    @PreAuthorize(Roles.ADMIN_ONLY)
    BusinessPartyApi.Response blacklist(@PathVariable String id, @RequestBody SupplierOnboardingApi.TransitionRequest request) {
        return supplierOnboardingService.blacklist(id, request.reason());
    }
}
