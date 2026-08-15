package com.bemo.hr.shared.i18n;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    TranslationAdminService.TranslationPage translations(
            @RequestParam String locale,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size) {
        return service.page(locale, blankToNull(appId), search, page, size);
    }

    @PostMapping(value = "/translations/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    TranslationAdminService.TranslationImportResult importTranslations(
            @RequestParam String locale,
            @RequestParam(required = false) String appId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return service.importSpreadsheet(locale, blankToNull(appId), file, authentication.getName());
    }

    @PutMapping("/translations/{key}")
    TranslationAdminService.TranslationRow save(
            @PathVariable String key,
            @RequestBody TranslationAdminService.TranslationUpdate request,
            Authentication authentication) {
        return service.save(key, new TranslationAdminService.TranslationUpdate(
                request.locale(), blankToNull(request.appId()), request.textValue()),
                authentication.getName());
    }

    @DeleteMapping("/translations/{key}")
    TranslationAdminService.TranslationRow restore(
            @PathVariable String key,
            @RequestParam String locale,
            @RequestParam String appId,
            Authentication authentication) {
        return service.restoreDefault(key, locale, appId, authentication.getName());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
