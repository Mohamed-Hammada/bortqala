package com.bemo.hr.attendance.application;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Set;

/**
 * Lightweight signal published after a biometric import has persisted punch evidence.
 * Only the imported time range and affected calendar months are retained so large files
 * do not keep every punch timestamp alive until the surrounding transaction commits.
 * <p>
 * {@code affectedMonths} carries the calendar months that need attendance-report
 * (re-)generation. An empty set signals that no monthly reports are needed.
 */
public record BiometricImportCompletedEvent(
        Instant firstPunch,
        Instant lastPunch,
        String actor,
        Set<YearMonth> affectedMonths) {

    public BiometricImportCompletedEvent {
        actor = actor == null || actor.isBlank() ? "system" : actor;
        affectedMonths = affectedMonths == null ? Set.of() : Set.copyOf(affectedMonths);
    }

    /** Backward-compatible constructor for callers that don't need month tracking. */
    public BiometricImportCompletedEvent(Instant firstPunch, Instant lastPunch, String actor) {
        this(firstPunch, lastPunch, actor, Set.of());
    }
}
