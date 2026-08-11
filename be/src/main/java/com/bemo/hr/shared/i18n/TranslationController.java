package com.bemo.hr.shared.i18n;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/i18n")
public class TranslationController {
    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @GetMapping("/{locale}")
    ResponseEntity<TranslationService.TranslationBundle> bundle(@PathVariable String locale) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .varyBy(HttpHeaders.AUTHORIZATION)
                .body(translationService.bundle(locale));
    }
}
