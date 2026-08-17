package com.bemo.hr.product.trial;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/trial")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TrialDemoController {
    private final TrialDemoService service;

    @GetMapping
    TrialDemoApi.StatusResponse status() {
        return service.status();
    }

    @GetMapping("/templates")
    List<TrialDemoApi.TemplateResponse> templates() {
        return service.templates();
    }

    @PostMapping("/start")
    TrialDemoApi.StatusResponse start(@Valid @RequestBody TrialDemoApi.StartRequest request, Authentication auth) {
        return service.start(request, auth.getName());
    }

    @PostMapping("/convert")
    TrialDemoApi.StatusResponse convert(@Valid @RequestBody TrialDemoApi.ConvertRequest request, Authentication auth) {
        return service.convert(request, auth.getName());
    }

    @PostMapping("/reset")
    TrialDemoApi.StatusResponse reset(@Valid @RequestBody TrialDemoApi.ResetRequest request, Authentication auth) {
        return service.reset(request, auth.getName());
    }
}
