package com.bemo.license.api;

import com.bemo.license.application.LicenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class LicenseController {
    private final LicenseService licenseService;
    @PostMapping("/api/v1/licenses") @ResponseStatus(HttpStatus.CREATED)
    LicenseApi.CreatedLicense create(@RequestHeader("X-License-Admin-Key") String adminKey,
                                     @Valid @RequestBody LicenseApi.CreateLicenseRequest request){return licenseService.create(adminKey,request);}
    @PostMapping("/public/v1/activations") @ResponseStatus(HttpStatus.CREATED)
    LicenseApi.LicenseCertificate activate(@Valid @RequestBody LicenseApi.ActivateRequest request){return licenseService.activate(request);}
    @PostMapping("/public/v1/activations/validate")
    LicenseApi.LicenseCertificate validate(@Valid @RequestBody LicenseApi.ProofRequest request){return licenseService.validate(request);}
    @PostMapping("/public/v1/activations/deactivate")
    LicenseApi.DeactivationResult deactivate(@Valid @RequestBody LicenseApi.ProofRequest request){return licenseService.deactivate(request);}
}
