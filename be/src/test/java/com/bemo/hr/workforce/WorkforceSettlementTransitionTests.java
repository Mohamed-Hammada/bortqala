package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceSettlementTransitionTests {

    @Mock private WorkforceSettlementPeriodRepository periodRepository;
    @Mock private WorkerSettlementRepository workerSettlementRepository;
    @Mock private ContractorSettlementRepository contractorSettlementRepository;
    @Mock private ContractorSettlementLineRepository contractorSettlementLineRepository;
    @Mock private ContractorSettlementAdjustmentRepository contractorSettlementAdjustmentRepository;
    @Mock private WorkforceSettlementIssueRepository issueRepository;
    @Mock private ManualAttendanceEntryRepository attendanceRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private ContractorRepository contractorRepository;
    @Mock private WorkforceAdvanceRepository advanceRepository;
    @Mock private WorkforceAdvancePolicyRepository advancePolicyRepository;
    @Mock private PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private WorkforceExcelExportService excelExportService;
    @Mock private AuditService auditService;
    @Mock private PlatformTransactionManager platformTransactionManager;

    private WorkforceSettlementService service() {
        return new WorkforceSettlementService(periodRepository, workerSettlementRepository, contractorSettlementRepository,
                contractorSettlementLineRepository, contractorSettlementAdjustmentRepository,
                issueRepository, attendanceRepository, workerRepository, contractorRepository, advanceRepository,
                advancePolicyRepository, partnerLedgerEntryRepository, idempotencyService,
                excelExportService, auditService, platformTransactionManager);
    }

    private static WorkforceSettlementPeriod calculatedPeriod() {
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod("JUL-2", "2026-07-16", "2026-07-31", "HALF_MONTH", "CALCULATED");
        period.markCalculated("admin", emptyInputFingerprint(), 12, new BigDecimal("1000"),
                new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("850"), 2, 0);
        return period;
    }

    private static String emptyInputFingerprint() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new byte[0]));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void reviewReturnsSharedTransitionMetadata() {
        WorkforceSettlementPeriod period = calculatedPeriod();
        when(periodRepository.findById("p1")).thenReturn(Optional.of(period));

        TransitionResponse response = service().reviewPeriod("p1");

        assertThat(response.status()).isEqualTo("REVIEWED");
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.allowedActions()).containsExactly("APPROVE", "RECALCULATE", "EXPORT");
    }

    @Test
    void approveReturnsAllowedLockAndExportActions() {
        WorkforceSettlementPeriod period = calculatedPeriod();
        period.setStatus("REVIEWED");
        when(periodRepository.findById("p1")).thenReturn(Optional.of(period));

        TransitionResponse response = service().approvePeriod("p1");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.allowedActions()).containsExactly("LOCK", "EXPORT");
    }

    @Test
    void lockIsTheTerminalStateWithExportOnly() {
        WorkforceSettlementPeriod period = calculatedPeriod();
        period.setStatus("APPROVED");
        when(periodRepository.findById("p1")).thenReturn(Optional.of(period));

        TransitionResponse response = service().lockPeriod("p1");

        assertThat(response.status()).isEqualTo("LOCKED");
        assertThat(response.allowedActions()).containsExactly("EXPORT");
    }

    @Test
    void lockRejectsAPeriodThatWasNotApproved() {
        WorkforceSettlementPeriod period = calculatedPeriod();
        when(periodRepository.findById("p1")).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service().lockPeriod("p1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("اعتماد");
    }
}
