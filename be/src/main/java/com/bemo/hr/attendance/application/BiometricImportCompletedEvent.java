package com.bemo.hr.attendance.application;

import java.time.Instant;

/**
 * Lightweight signal published after a biometric import has persisted punch evidence.
 * Only the imported time range is retained so large files do not keep every punch timestamp
 * alive until the surrounding transaction commits.
 */
public record BiometricImportCompletedEvent(Instant firstPunch, Instant lastPunch, String actor) {
    public BiometricImportCompletedEvent {
        actor = actor == null || actor.isBlank() ? "system" : actor;
    }
}
