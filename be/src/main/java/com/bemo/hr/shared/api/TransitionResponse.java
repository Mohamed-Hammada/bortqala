package com.bemo.hr.shared.api;

import java.util.List;

/**
 * Shared workflow/status-transition metadata returned by status-transition endpoints
 * (guide §5.4). The frontend uses it for UX; the backend rechecks role, tenant,
 * version, dependencies, and state before performing the transition.
 *
 * @param status         current status after the transition
 * @param version        optimistic-lock version (or calculation version) after the transition
 * @param allowedActions actions the current actor may still perform on this entity
 */
public record TransitionResponse(String status, long version, List<String> allowedActions) {
    public TransitionResponse {
        allowedActions = List.copyOf(allowedActions);
    }
}
