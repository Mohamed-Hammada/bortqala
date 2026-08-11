package com.bemo.hr.finance.api;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fiscal-periods")
public class FiscalPeriodController {

    private final FiscalPeriodRepository repository;

    public FiscalPeriodController(FiscalPeriodRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional
    public List<FiscalPeriodApi.FiscalPeriodResponse> listPeriods(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return repository.findByFiscalYearOrderByPeriodNumberAsc(targetYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/generate-year")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public List<FiscalPeriodApi.FiscalPeriodResponse> generateYear(@RequestParam int year) {
        // Auto generate 12 monthly periods for given year if none exist
        var existing = repository.findByFiscalYearOrderByPeriodNumberAsc(year);
        if (!existing.isEmpty()) {
            return existing.stream().map(this::toResponse).toList();
        }

        String[] monthNames = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(year, m, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            FiscalPeriod period = new FiscalPeriod(year, m, "شهر " + monthNames[m - 1] + " (" + m + ")", start, end, FiscalPeriod.Status.OPEN);
            repository.save(period);
        }
        return repository.findByFiscalYearOrderByPeriodNumberAsc(year).stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}/status")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public FiscalPeriodApi.FiscalPeriodResponse updateStatus(@PathVariable String id,
                                                            @Valid @RequestBody FiscalPeriodApi.UpdateStatusPayload payload,
                                                            Authentication authentication) {
        FiscalPeriod period = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("الفترة المالية غير موجودة", "FIN_FISCAL_PERIOD_NOT_FOUND", HttpStatus.CONFLICT));
        if (payload.expectedVersion() != null && payload.expectedVersion() != period.getVersion()) {
            throw new BusinessRuleException("تم تعديل الفترة المالية بواسطة مستخدم آخر.", "RECORD_ALREADY_MODIFIED",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        FiscalPeriod.Status newStatus = FiscalPeriod.Status.valueOf(payload.status().toUpperCase());
        period.updateStatus(newStatus, authentication.getName());
        return toResponse(repository.save(period));
    }

    private FiscalPeriodApi.FiscalPeriodResponse toResponse(FiscalPeriod p) {
        return new FiscalPeriodApi.FiscalPeriodResponse(
                p.getId(),
                p.getFiscalYear(),
                p.getPeriodNumber(),
                p.getPeriodName(),
                p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                p.getEndDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                p.getStatus().name(),
                p.getClosedBy(),
                p.getClosedAt(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
