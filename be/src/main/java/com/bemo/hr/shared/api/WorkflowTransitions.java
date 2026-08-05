package com.bemo.hr.shared.api;

import java.util.List;
import java.util.Map;

/**
 * Generic helper for the shared transition-metadata contract (guide §5.4).
 *
 * <p>A workflow is a state machine described as a {@code Map<currentStatus, allowedActions>}.
 * Services derive the current allowed actions (sometimes dynamically, e.g. depending on
 * staleness or error counts) and wrap the result in a {@link TransitionResponse}. The backend
 * still re-checks role, tenant, version, dependencies, and state inside each transition.
 */
public final class WorkflowTransitions {

    private WorkflowTransitions() {
    }

    public static TransitionResponse response(String status, long version, Map<String, List<String>> workflow) {
        return new TransitionResponse(status, version, allowedActions(status, workflow));
    }

    public static List<String> allowedActions(String status, Map<String, List<String>> workflow) {
        if (status == null) {
            return List.of();
        }
        return workflow.getOrDefault(status, List.of());
    }
}
