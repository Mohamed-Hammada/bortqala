package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollGlPosting;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.infrastructure.PayrollGlPostingRepository;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollGlPostingServiceTests {

    private PayrollGlPostingRepository repository;
    private SubledgerPostingService subledgerPostingService;
    private PayrollGlPostingService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayrollGlPostingRepository.class);
        subledgerPostingService = mock(SubledgerPostingService.class);
        service = new PayrollGlPostingService(repository, subledgerPostingService);
    }

    @Test
    void postsApprovedPayrollRunThroughConfiguredProfile() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PayrollRunHeader run = new PayrollRunHeader("PAY-2026-08", "period-2026-08", java.time.LocalDate.of(2026, 8, 31));
        run.updateTotals(new BigDecimal("120000.00"), new BigDecimal("15000.00"), new BigDecimal("105000.00"));
        run.transitionTo(PayrollRunHeader.Status.REVIEWED);
        run.transitionTo(PayrollRunHeader.Status.APPROVED);
        JournalEntry journalEntry = mock(JournalEntry.class);
        when(journalEntry.getId()).thenReturn("jrnl-900");
        when(subledgerPostingService.postProfileEvent(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyMap(), isNull(), isNull(), isNull(), anyString()))
                .thenReturn(journalEntry);

        PayrollGlPosting posting = service.postApprovedRun(run, "payroll-admin");
        assertThat(posting).isNotNull();
        assertThat(posting.getGrossAmount()).isEqualByComparingTo(new BigDecimal("120000.00"));
        assertThat(posting.getNetAmount()).isEqualByComparingTo(new BigDecimal("105000.00"));
        assertThat(posting.getStatus()).isEqualTo(PayrollGlPosting.Status.POSTED);

        when(repository.findByPayrollPeriodId("period-2026-08")).thenReturn(Optional.of(posting));
        assertThat(service.getGlPosting("period-2026-08")).isNotNull();
    }

    @Test
    void rejectsLegacyClientSuppliedJournalAndAmounts() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.postPayrollToGl(
                "period", "journal", BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("server-managed");
        verifyNoInteractions(subledgerPostingService);
    }
}
