package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialIssueHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialIssueLine;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialIssueServiceTests {

    private MaterialIssueHeaderRepository issueHeaderRepository;
    private MaterialIssueLineRepository issueLineRepository;
    private MaterialIssueService issueService;

    @BeforeEach
    void setUp() {
        issueHeaderRepository = mock(MaterialIssueHeaderRepository.class);
        issueLineRepository = mock(MaterialIssueLineRepository.class);
        issueService = new MaterialIssueService(issueHeaderRepository, issueLineRepository);
    }

    @Test
    void createsLineAndCancelsMaterialIssueSuccessfully() {
        when(issueHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(issueLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaterialIssueHeader header = issueService.createIssue("MI-001", "po-1", LocalDate.of(2026, 3, 1));
        assertThat(header).isNotNull();
        assertThat(header.getStatus()).isEqualTo(MaterialIssueHeader.Status.ISSUED);

        MaterialIssueLine line = issueService.addIssueLine(header.getId(), "item-1", new BigDecimal("50.00"), "wh-1");
        assertThat(line).isNotNull();

        when(issueHeaderRepository.findById(header.getId())).thenReturn(Optional.of(header));
        issueService.cancelIssue(header.getId());
        assertThat(header.getStatus()).isEqualTo(MaterialIssueHeader.Status.CANCELLED);
    }
}
