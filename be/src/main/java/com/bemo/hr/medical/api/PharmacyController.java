package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.PharmacyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic/pharmacy")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping("/items")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'inventory.read')")
    public ResponseEntity<List<PharmacyItemDto>> getAllPharmacyItems() {
        return ResponseEntity.ok(pharmacyService.getAllPharmacyItems());
    }

    @PostMapping("/items")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'inventory.manage')")
    public ResponseEntity<PharmacyItemDto> savePharmacyItem(@Valid @RequestBody SavePharmacyItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacyService.savePharmacyItem(request));
    }

    @GetMapping("/items/{id}/fefo")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'inventory.read')")
    public ResponseEntity<List<BatchFefoSuggestionDto>> getFefoSuggestions(@PathVariable String id) {
        return ResponseEntity.ok(pharmacyService.getFefoSuggestions(id));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage', 'inventory.manage')")
    public ResponseEntity<PharmacyDispenseRecordDto> dispensePrescription(@PathVariable String id,
                                                                          @Valid @RequestBody DispensePrescriptionRequest request,
                                                                          Authentication auth) {
        String currentUserId = auth != null ? auth.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacyService.dispensePrescription(id, request, currentUserId));
    }

    @PostMapping("/dispense/{id}/approve")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage', 'inventory.manage')")
    public ResponseEntity<PharmacyDispenseRecordDto> approveControlledDispense(@PathVariable String id,
                                                                               Authentication auth) {
        String secondSignerId = auth != null ? auth.getName() : "second-signer";
        return ResponseEntity.ok(pharmacyService.approveControlledDispense(id, secondSignerId));
    }

    @GetMapping("/narcotics")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'inventory.read')")
    public ResponseEntity<List<NarcoticsRegisterEntryDto>> getNarcoticsRegister(@RequestParam(required = false) Long from,
                                                                                @RequestParam(required = false) Long to) {
        return ResponseEntity.ok(pharmacyService.getNarcoticsRegister(from, to));
    }
}
