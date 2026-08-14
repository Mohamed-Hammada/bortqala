package com.bemo.hr.payroll;

import com.bemo.hr.PostgresIntegrationTest;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.payroll.domain.PayrollCalculationPolicy;
import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.infrastructure.PayrollCalculationPolicyRepository;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollPaymentConcurrencyTests extends PostgresIntegrationTest {

    private final PayrollService payrollService;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final PayrollRunHeaderRepository payrollRunHeaderRepository;
    private final PayrollInputSnapshotRepository payrollInputSnapshotRepository;
    private final PayrollCalculationPolicyRepository payrollCalculationPolicyRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final TenantApplicationRepository tenantApplicationRepository;

    private String appId;

    @Autowired
    PayrollPaymentConcurrencyTests(PayrollService payrollService,
                                   SalaryPaymentRepository salaryPaymentRepository,
                                   PayrollRunHeaderRepository payrollRunHeaderRepository,
                                   PayrollInputSnapshotRepository payrollInputSnapshotRepository,
                                   PayrollCalculationPolicyRepository payrollCalculationPolicyRepository,
                                   EmployeeRepository employeeRepository,
                                   AttendanceCategoryRepository attendanceCategoryRepository,
                                   AuditLogRepository auditLogRepository,
                                   TenantApplicationRepository tenantApplicationRepository) {
        this.payrollService = payrollService;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.payrollRunHeaderRepository = payrollRunHeaderRepository;
        this.payrollInputSnapshotRepository = payrollInputSnapshotRepository;
        this.payrollCalculationPolicyRepository = payrollCalculationPolicyRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
    }

    @AfterEach
    void cleanup() {
        try {
            if (appId != null) {
                TenantContext.set(appId);
                auditLogRepository.deleteAll();
                salaryPaymentRepository.deleteAll();
                payrollInputSnapshotRepository.deleteAll();
                payrollRunHeaderRepository.deleteAll();
                employeeRepository.deleteAll();
                attendanceCategoryRepository.deleteAll();
                payrollCalculationPolicyRepository.deleteAll();
                tenantApplicationRepository.deleteById(appId);
            }
        } finally {
            appId = null;
            TenantContext.clear();
        }
    }

    @RepeatedTest(10)
    void concurrentPaymentRequestsDoNotDoublePay() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication tenantApplication = tenantApplicationRepository.save(
                new TenantApplication("PAYCON-" + suffix, "Payroll concurrency test"));
        appId = tenantApplication.getId();
        TenantContext.set(appId);

        AttendanceCategory category = attendanceCategoryRepository.save(new AttendanceCategory(
                "PAYCON-" + suffix, "Payroll concurrency", 480, PayCycle.MONTHLY,
                AttendanceMode.MANUAL, false, 111, true));
        Employee employee = employeeRepository.save(new Employee(
                "PAYCON-" + suffix, "Payroll Concurrency Employee", null, category.getId(),
                EmploymentType.FIXED, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 1), null, true));
        payrollCalculationPolicyRepository.save(new PayrollCalculationPolicy(
                "Payroll concurrency policy", LocalDate.of(2026, 1, 1), null,
                new BigDecimal("240.00"), new BigDecimal("1.5000")));

        PayrollRunHeader run = new PayrollRunHeader(
                "PAYCON-" + suffix, "2026-08:FULL_MONTH", LocalDate.of(2026, 8, 31));
        run.updateTotals(new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"));
        run.transitionTo(PayrollRunHeader.Status.REVIEWED);
        run.transitionTo(PayrollRunHeader.Status.APPROVED);
        run.transitionTo(PayrollRunHeader.Status.POSTED);
        run = payrollRunHeaderRepository.save(run);

        PayrollInputSnapshot snapshot = payrollInputSnapshotRepository.save(new PayrollInputSnapshot(
                run.getId(), employee.getId(), "2026-08:FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), new BigDecimal("5000.00"),
                9600, 0, 0, 0, "policy-" + suffix, 0,
                new BigDecimal("240.00"), new BigDecimal("1.5000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000.00"), new BigDecimal("5000.00"), "maker"));

        SalaryPayment payment = new SalaryPayment(
                employee.getId(), null, 2026, 8, "FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000.00"), PaymentStatus.DRAFT, null, null, null, null, "maker");
        payment.transitionTo(PaymentStatus.CALCULATED);
        payment.transitionTo(PaymentStatus.REVIEWED);
        payment.transitionTo(PaymentStatus.APPROVED);
        payment.transitionTo(PaymentStatus.POSTED);
        payment.attachCalculationEvidence(run.getId(), snapshot.getId());
        payment = salaryPaymentRepository.saveAndFlush(payment);

        long expectedVersion = payment.getVersion();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        Thread first = worker(appId, employee.getId(), expectedVersion, "payroll-a", ready, start, succeeded, rejected);
        Thread second = worker(appId, employee.getId(), expectedVersion, "payroll-b", ready, start, succeeded, rejected);
        first.start();
        second.start();
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        first.join(TimeUnit.SECONDS.toMillis(30));
        second.join(TimeUnit.SECONDS.toMillis(30));

        assertThat(first.isAlive()).isFalse();
        assertThat(second.isAlive()).isFalse();
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        SalaryPayment reloaded = salaryPaymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(reloaded.getPaidBy()).isIn("payroll-a", "payroll-b");
        assertThat(auditLogRepository.findByEntityTypeOrderByOccurredAtDesc(
                "SALARY_PAYMENT", org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    private Thread worker(String tenantId, String employeeId, long expectedVersion, String actor,
                          CountDownLatch ready, CountDownLatch start,
                          AtomicInteger succeeded, AtomicInteger rejected) {
        return new Thread(() -> {
            TenantContext.set(tenantId);
            try {
                ready.countDown();
                start.await();
                PayrollApi.PaymentRequest request = new PayrollApi.PaymentRequest(
                        employeeId, 2026, 8, "FULL_MONTH", PaymentMethod.BANK_TRANSFER,
                        "PAYCON", null, null, expectedVersion);
                try {
                    payrollService.recordPayment(request, actor);
                    succeeded.incrementAndGet();
                } catch (BusinessRuleException exception) {
                    rejected.incrementAndGet();
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clear();
            }
        });
    }
}
