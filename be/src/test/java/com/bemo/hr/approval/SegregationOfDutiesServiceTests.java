package com.bemo.hr.approval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegregationOfDutiesServiceTests {

    private SegregationOfDutiesService sodService;

    @BeforeEach
    void setUp() {
        sodService = new SegregationOfDutiesService();
    }

    @Test
    void allowsSelfApprovalWhenEnabled() {
        assertThatCode(() -> sodService.validateRequesterNotApprover("john.doe", "john.doe", true))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksSelfApprovalWhenDisabled() {
        assertThatThrownBy(() -> sodService.validateRequesterNotApprover("john.doe", "john.doe", false))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("SELF_APPROVAL_DISABLED");
    }

    @Test
    void allowsDifferentUserApproval() {
        assertThatCode(() -> sodService.validateRequesterNotApprover("john.doe", "jane.smith", false))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksCreatorFromPosting() {
        assertThatThrownBy(() -> sodService.validateCreatorNotPoster("alice", "alice", "POST_JOURNAL"))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("CREATOR_POSTER_CONFLICT");
    }

    @Test
    void blocksPreparerFromDisbursingHighValue() {
        assertThatThrownBy(() -> sodService.validatePreparerNotDisburser("bob", "bob", new BigDecimal("50000"), new BigDecimal("10000")))
                .isInstanceOf(SegregationOfDutiesViolationException.class)
                .hasMessageContaining("HIGH_VALUE_SOD_CONFLICT");
    }
}
