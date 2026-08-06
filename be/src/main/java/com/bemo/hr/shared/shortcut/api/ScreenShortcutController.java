package com.bemo.hr.shared.shortcut.api;

import com.bemo.hr.shared.shortcut.application.UserScreenShortcutService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/preferences/shortcuts")
public class ScreenShortcutController {

    private final UserScreenShortcutService service;

    public ScreenShortcutController(UserScreenShortcutService service) {
        this.service = service;
    }

    @GetMapping
    public ScreenShortcutApi.ProfileResponse get(Authentication authentication) {
        return service.getProfile(authentication.getName());
    }

    @PutMapping
    public ScreenShortcutApi.ProfileResponse replace(
            @Valid @RequestBody ScreenShortcutApi.ReplaceShortcutsRequest request,
            Authentication authentication
    ) {
        return service.replace(authentication.getName(), request);
    }

    @PostMapping("/reset")
    public ScreenShortcutApi.ProfileResponse reset(Authentication authentication) {
        return service.resetToDefaults(authentication.getName());
    }
}
