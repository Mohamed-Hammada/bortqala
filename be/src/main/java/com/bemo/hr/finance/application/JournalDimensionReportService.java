package com.bemo.hr.finance.application;

import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JournalDimensionReportService {
    private final JdbcTemplate jdbcTemplate;

    public JournalDimensionReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DimensionSummary> summarize(LocalDate from, LocalDate to, String costCenterId, String projectId, String departmentId) {
        log.debug("summarize called with from={}, to={}, costCenterId={}, projectId={}, departmentId={}", from, to, costCenterId, projectId, departmentId);
        StringBuilder sql = new StringBuilder("select d.cost_center_id,d.project_id,d.department_id,sum(l.debit),sum(l.credit) from journal_dimensions d join journal_entry_lines l on l.id=d.journal_entry_line_id join journal_entries j on j.id=l.journal_entry_id where d.app_id=? and j.status='POSTED' and j.entry_date between ? and ?");
        List<Object> args = new ArrayList<>(List.of(TenantContext.require(), from, to));
        if (costCenterId != null) {
            sql.append(" and d.cost_center_id=?");
            args.add(costCenterId);
        }
        if (projectId != null) {
            sql.append(" and d.project_id=?");
            args.add(projectId);
        }
        if (departmentId != null) {
            sql.append(" and d.department_id=?");
            args.add(departmentId);
        }
        sql.append(" group by d.cost_center_id,d.project_id,d.department_id order by d.department_id,d.cost_center_id,d.project_id");
        return jdbcTemplate.query(sql.toString(), (rs, n) -> {
            BigDecimal debit = rs.getBigDecimal(4), credit = rs.getBigDecimal(5);
            return new DimensionSummary(rs.getString(1), rs.getString(2), rs.getString(3), debit, credit, debit.subtract(credit));
        }, args.toArray());
    }

    public record DimensionSummary(String costCenterId, String projectId, String departmentId, BigDecimal debit,
                                   BigDecimal credit, BigDecimal balance) {
    }
}
