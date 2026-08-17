package com.bemo.hr.shared.shortcut.application;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultScreenShortcutProvider {

    private static final List<DefaultShortcut> DEFAULTS = List.of(
            new DefaultShortcut("KeyD", "DASHBOARD"),
            new DefaultShortcut("KeyE", "EMPLOYEES"),
            new DefaultShortcut("KeyI", "IMPORTS"),
            new DefaultShortcut("KeyR", "REPORTS"),
            new DefaultShortcut("KeyW", "WORKFORCE_WORKERS"),
            new DefaultShortcut("KeyA", "WORKFORCE_ATTENDANCE"),
            new DefaultShortcut("KeyP", "PROCUREMENT"),
            new DefaultShortcut("KeyJ", "JOURNAL_ENTRIES"),
            new DefaultShortcut("KeyU", "USERS"),
            new DefaultShortcut("KeyS", "SETTINGS")
    );

    public List<DefaultShortcut> defaults() {
        return DEFAULTS;
    }

    public record DefaultShortcut(
            String secondKeyCode,
            String pageCode
    ) {
    }
}
