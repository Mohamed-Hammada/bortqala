package com.bemo.hr.shared.shortcut.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ScreenShortcutApi {

    private ScreenShortcutApi() {}

    public record DestinationResponse(
        String pageCode,
        String menuId,
        String route,
        String titleKey,
        String module,
        String requiredFeature
    ) {}

    public record ShortcutResponse(
        String id,
        String pageCode,
        String menuId,
        String route,
        String titleKey,
        String secondKeyCode,
        String displayKey,
        boolean enabled,
        boolean defaultShortcut,
        String availabilityStatus,
        String unavailableReasonKey
    ) {}

    public record ProfileResponse(
        String profileMode,
        long version,
        List<ShortcutResponse> shortcuts,
        List<DestinationResponse> availableDestinations,
        Instant updatedAt
    ) {}

    public record ShortcutItemRequest(
        @NotNull
        @Pattern(regexp = "Key[A-Z]|Digit[0-9]")
        String secondKeyCode,

        @NotNull
        @Pattern(regexp = "[A-Z0-9_]{1,80}")
        String pageCode,

        boolean enabled
    ) {}

    public record ReplaceShortcutsRequest(
        @NotNull
        Long expectedVersion,

        @NotNull
        @Size(max = 20)
        List<@Valid ShortcutItemRequest> shortcuts
    ) {}
}
