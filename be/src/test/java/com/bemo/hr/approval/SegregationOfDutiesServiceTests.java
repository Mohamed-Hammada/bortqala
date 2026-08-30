package com.bemo.hr.approval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegregationOfDutiesServiceTests {

    private SegregationOfDutiesService service;

    @BeforeEach
    void setUp() {
        service = new SegregationOfDutiesService();
    }

    @Test
    @DisplayName("Requester not approver: passes when different users")
    void requesterNotApprover_differentUsers() {
        assertThatCode(() -> service.validateRequesterNotApprover("userA", "userB", false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Requester not approver: throws when same user and self-approval disabled")
    void requesterNotApprover_sameUser_disabled() {
        assertThatThrownBy(() -> service.validateRequesterNotApprover("userA", "userA", false))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("The creator of the document cannot approve their own request");
    }

    @Test
    @DisplayName("Requester not approver: passes when same user and self-approval enabled")
    void requesterNotApprover_sameUser_enabled() {
        assertThatCode(() -> service.validateRequesterNotApprover("userA", "userA", true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Creator not poster: throws when creator posts own entry")
    void creatorNotPoster_sameUser() {
        assertThatThrownBy(() -> service.validateCreatorNotPoster("userA", "userA", "POST"))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("The user who created the entry cannot execute POST");
    }

    @Test
    @DisplayName("Preparer not disburser: throws when amount exceeds threshold and same user")
    void preparerNotDisburser_exceedsThreshold_sameUser() {
        assertThatThrownBy(() -> service.validatePreparerNotDisburser("userA", "userA",
                new BigDecimal("50000"), new BigDecimal("10000")))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("Transactions over 10000 require a different disburser");
    }

    @Test
    @DisplayName("Claim creator not certifier: throws when same user")
    void claimCreatorNotCertifier_sameUser() {
        assertThatThrownBy(() -> service.validateClaimCreatorNotCertifier("contractor_eng", "contractor_eng", "Owner IPC"))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("The user who prepared the Owner IPC cannot certify or approve it");
    }

    @Test
    @DisplayName("Claim creator not certifier: passes when different users")
    void claimCreatorNotCertifier_differentUsers() {
        assertThatCode(() -> service.validateClaimCreatorNotCertifier("contractor_eng", "consultant_lead", "Owner IPC"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dual authorization: throws when distinct approvers less than 2 for high value")
    void dualAuthorization_failsWhenOnlyOneApprover() {
        assertThatThrownBy(() -> service.validateDualAuthorizationRequired(2, 1,
                new BigDecimal("500000"), new BigDecimal("100000")))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("require at least 2 distinct authorized approvers");
    }

    @Test
    @DisplayName("Dual authorization: passes when 2 distinct approvers sign")
    void dualAuthorization_passesWhenTwoApprovers() {
        assertThatCode(() -> service.validateDualAuthorizationRequired(2, 2,
                new BigDecimal("500000"), new BigDecimal("100000")))
                .doesNotThrowAnyException();
    }
}
