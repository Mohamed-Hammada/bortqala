package com.bemo.hr.shared.shortcut.api;

import com.bemo.hr.shared.shortcut.application.UserScreenShortcutService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/preferences/shortcuts")
@PreAuthorize("isAuthenticated()")
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
