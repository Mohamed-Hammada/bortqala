package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.PatientService;
import com.bemo.hr.medical.nationalid.EgyptianNationalIdParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinic/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PatientResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(patientService.searchPatients(query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(patientService.getPatient(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody RegisterPatientRequest request) {
        return ResponseEntity.ok(patientService.registerPatient(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    @GetMapping("/check-duplicates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicates(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String nationalId
    ) {
        return ResponseEntity.ok(patientService.checkDuplicates(phone, nationalId));
    }

    @GetMapping("/parse-national-id")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalIdParseResponse> parseNationalId(@RequestParam String nationalId) {
        EgyptianNationalIdParser.ParseResult result = EgyptianNationalIdParser.parse(nationalId);
        return ResponseEntity.ok(new NationalIdParseResponse(
                result.valid(),
                result.nationalId(),
                result.birthDate() != null ? result.birthDate().toString() : null,
                result.gender(),
                result.governorateCode(),
                result.governorateName(),
                result.errorMessage()
        ));
    }
}
