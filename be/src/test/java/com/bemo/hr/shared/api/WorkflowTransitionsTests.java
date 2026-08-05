package com.bemo.hr.shared.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTransitionsTests {

    private static final Map<String, List<String>> WORKFLOW = Map.of(
            "DRAFT", List.of("CALCULATE"),
            "APPROVED", List.of("REOPEN", "EXPORT"));

    @Test
    void buildsTheSharedResponseWithAllowedActionsForTheCurrentStatus() {
        TransitionResponse response = WorkflowTransitions.response("APPROVED", 7, WORKFLOW);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.version()).isEqualTo(7);
        assertThat(response.allowedActions()).containsExactly("REOPEN", "EXPORT");
    }

    @Test
    void returnsNoActionsForUnknownStatuses() {
        assertThat(WorkflowTransitions.allowedActions("LOCKED", WORKFLOW)).isEmpty();
    }

    @Test
    void returnsNoActionsForNullStatus() {
        assertThat(WorkflowTransitions.allowedActions(null, WORKFLOW)).isEmpty();
    }

    @Test
    void responseDefensivelyCopiesItsActionsList() {
        List<String> actions = new java.util.ArrayList<>(List.of("EXPORT"));
        TransitionResponse response = new TransitionResponse("APPROVED", 1, actions);
        actions.add("REOPEN");

        assertThat(response.allowedActions()).containsExactly("EXPORT");
    }
}
