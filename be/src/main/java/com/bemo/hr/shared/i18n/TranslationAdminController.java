package com.bemo.hr.shared.i18n;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/i18n/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TranslationAdminController {
    private final TranslationAdminService service;

    public TranslationAdminController(TranslationAdminService service) {
        this.service = service;
    }

    @GetMapping("/apps")
    List<TranslationAdminService.AppOption> apps() {
        return service.apps();
    }

    @GetMapping("/translations")
    List<TranslationAdminService.TranslationRow> translations(
            @RequestParam String locale,
            @RequestParam(required = false) String appId) {
        return service.list(locale, blankToNull(appId));
    }

    @PutMapping("/translations/{key}")
    TranslationAdminService.TranslationRow save(
            @PathVariable String key,
            @RequestBody TranslationAdminService.TranslationUpdate request,
            Authentication authentication) {
        return service.save(key, new TranslationAdminService.TranslationUpdate(
                request.locale(), blankToNull(request.appId()), request.textValue()), authentication.getName());
    }

    @DeleteMapping("/translations/{key}")
    TranslationAdminService.TranslationRow restoreDefault(
            @PathVariable String key,
            @RequestParam String locale,
            @RequestParam String appId,
            Authentication authentication) {
        return service.restoreDefault(key, locale, blankToNull(appId), authentication.getName());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
