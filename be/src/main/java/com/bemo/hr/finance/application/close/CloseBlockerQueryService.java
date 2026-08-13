package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.ZoneOffset;

@Service
public class CloseBlockerQueryService {
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JdbcTemplate jdbcTemplate;

    public CloseBlockerQueryService(FiscalPeriodRepository fiscalPeriodRepository, JdbcTemplate jdbcTemplate) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long dated(String periodId, String table, String dateColumn, String unfinishedPredicate) {
        FiscalPeriod period = requirePeriod(periodId);
        String sql = "select count(*) from " + table + " where app_id = ? and " + dateColumn
                + " between ? and ? and (" + unfinishedPredicate + ")";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, TenantContext.require(),
                Date.valueOf(period.getStartDate()), Date.valueOf(period.getEndDate()));
        return count == null ? 0 : count;
    }

    public long timestamped(String periodId, String table, String timestampColumn, String unfinishedPredicate) {
        FiscalPeriod period = requirePeriod(periodId);
        long start = period.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endExclusive = period.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        String sql = "select count(*) from " + table + " where app_id = ? and " + timestampColumn
                + " >= ? and " + timestampColumn + " < ? and (" + unfinishedPredicate + ")";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, TenantContext.require(), start, endExclusive);
        return count == null ? 0 : count;
    }

    private FiscalPeriod requirePeriod(String periodId) {
        return fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + periodId));
    }
}
