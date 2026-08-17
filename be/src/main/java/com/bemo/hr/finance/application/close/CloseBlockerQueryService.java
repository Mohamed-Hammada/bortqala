package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.ZoneOffset;

@Slf4j
@Service
public class CloseBlockerQueryService {
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JdbcTemplate jdbcTemplate;

    public CloseBlockerQueryService(FiscalPeriodRepository fiscalPeriodRepository, JdbcTemplate jdbcTemplate) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long dated(String periodId, String table, String dateColumn, String unfinishedPredicate) {
        log.debug("dated called with periodId={}, table={}, dateColumn={}", periodId, table, dateColumn);
        FiscalPeriod period = requirePeriod(periodId);
        String sql = "select count(*) from " + table + " where app_id = ? and " + dateColumn
                + " between ? and ? and (" + unfinishedPredicate + ")";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, TenantContext.require(),
                Date.valueOf(period.getStartDate()), Date.valueOf(period.getEndDate()));
        long result = count == null ? 0 : count;
        log.debug("dated result for table={}: {}", table, result);
        return result;
    }

    public long timestamped(String periodId, String table, String timestampColumn, String unfinishedPredicate) {
        log.debug("timestamped called with periodId={}, table={}, timestampColumn={}", periodId, table, timestampColumn);
        FiscalPeriod period = requirePeriod(periodId);
        long start = period.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endExclusive = period.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        String sql = "select count(*) from " + table + " where app_id = ? and " + timestampColumn
                + " >= ? and " + timestampColumn + " < ? and (" + unfinishedPredicate + ")";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, TenantContext.require(), start, endExclusive);
        long result = count == null ? 0 : count;
        log.debug("timestamped result for table={}: {}", table, result);
        return result;
    }

    private FiscalPeriod requirePeriod(String periodId) {
        return fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + periodId));
    }
}
